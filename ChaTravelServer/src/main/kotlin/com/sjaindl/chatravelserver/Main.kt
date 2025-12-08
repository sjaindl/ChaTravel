package com.sjaindl.chatravelserver

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.mongodb.MongoClientSettings
import com.sjaindl.chatravelserver.fcm.InterestMatchPusher
import com.sjaindl.chatravelserver.fcm.TokenRepository
import com.sjaindl.chatravelserver.fcm.pushRoutes
import com.sjaindl.chatravelserver.graphql.buildGraphQL
import com.sjaindl.chatravelserver.graphql.graphqlRoutes
import com.sjaindl.chatravelserver.grpc.ChatService
import com.sjaindl.chatravelserver.sse.SSERoutes
import com.sjaindl.chatravelserver.websocket.websocketMessageRoutes
import de.flapdoodle.embed.mongo.distribution.Version
import graphql.GraphQL
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import io.ktor.serialization.jackson.jackson
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import io.ktor.server.routing.*
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.slf4j.event.Level
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import java.util.concurrent.TimeUnit
import java.io.FileInputStream
import kotlin.jvm.java
import kotlin.time.Duration.Companion.seconds

fun main() {
    runBlocking {
        initFirebaseAdmin(serviceAccountPath = "src/main/resources/chatravel-firebase-adminsdk.json")

        val embeddedMongoDB = EmbeddedMongoDB(
            ip = "localhost",
            port = 27017,
            version = Version.Main.V8_0,
        )

        val mongoClient = KMongo.createClient(
            MongoClientSettings.builder()
                .applyConnectionString(embeddedMongoDB.connectionString)
                .build()
        ).coroutine

        val mongoDbName = "chatravel_db"
        val database = mongoClient.getDatabase(name = mongoDbName)

        val userRepository = UserRepository(database = database)
        val messagesRepository = MessagesRepository(database = database)
        val tokenRepository = TokenRepository(database = database)

        val ktorEngine = startServer(messagesRepository = messagesRepository, userRepository = userRepository, tokenRepository = tokenRepository)
        val grpcServer = startGrpc(messagesRepository = messagesRepository)
        InterestMatchPusher(userRepository = userRepository, tokenRepository = tokenRepository).start()

        println("HTTP on 8080 and gRPC on 9090 are up.")

        Runtime.getRuntime().addShutdownHook(Thread {
            println("Shutting down…")
            try {
                grpcServer.shutdown().awaitTermination(5, TimeUnit.SECONDS)
            } catch (throwable: Throwable) {
                println("error shutting down grpc server: ${throwable.message}")
            }
            try {
                ktorEngine.stop(gracePeriodMillis = 2_000, timeoutMillis = 5_000)
            } catch (throwable: Throwable) {
                println("error shutting down netty server: ${throwable.message}")
            }

            try {
                mongoClient.client.close()
            } catch (throwable: Throwable) {
                println("error shutting down mongo client: ${throwable.message}")
            }

            try {
                embeddedMongoDB.close()
            } catch (throwable: Throwable) {
                println("error closing mongo db: ${throwable.message}")
            }

            println("Shutdown complete")
        })

        awaitCancellation()
    }
}

fun startServer(
    messagesRepository: MessagesRepository,
    userRepository: UserRepository,
    tokenRepository: TokenRepository,
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {

    val graphQL = GraphQLLoader(messagesRepository = messagesRepository, userRepository = userRepository).load()

    return embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(CallLogging) {
            level = Level.DEBUG
        }

        install(ContentNegotiation) {
            jackson {
                SerializationFeature.INDENT_OUTPUT
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            }

            json(
                Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
            )
        }

        install(WebSockets) {
            pingPeriod = 30.seconds
            timeout = 15.seconds
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        routing {
            generalRoutes()
            userRoutes(userRepository = userRepository)
            conversationRoutes(messagesRepository = messagesRepository, userRepository = userRepository)
            messagesRoutes(messagesRepository = messagesRepository)
            websocketMessageRoutes(messagesRepository = messagesRepository)
            SSERoutes(userRepository = userRepository)
            graphqlRoutes(graphQL = graphQL)
            pushRoutes(tokenRepository = tokenRepository)
        }
    }.start(wait = false)
}

fun startGrpc(
    messagesRepository: MessagesRepository,
): Server {
    val port = 9090

    val server = ServerBuilder
        .forPort(port)
        .addService(ChatService(messagesRepository = messagesRepository))
        .addService(ProtoReflectionService.newInstance())
        .build()
        .start()

    println("gRPC server listening on $port")
    return server
}

fun initFirebaseAdmin(serviceAccountPath: String) {
    if (FirebaseApp.getApps().isNotEmpty()) return
    val options = FirebaseOptions.builder()
        .setCredentials(ServiceAccountCredentials.fromStream(FileInputStream(serviceAccountPath)))
        .build()
    FirebaseApp.initializeApp(options)
}

class GraphQLLoader(
    private val messagesRepository: MessagesRepository,
    private val userRepository: UserRepository,
) {

    fun load(): GraphQL {
        val sdl = this::class.java.classLoader
            .getResource("graphql/schema.graphqls")!!.readText()

        return buildGraphQL(userRepository = userRepository, messagesRepository = messagesRepository, sdl = sdl)
    }
}
