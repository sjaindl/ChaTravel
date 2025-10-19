package com.sjaindl.chatravelserver

import com.mongodb.MongoClientSettings
import de.flapdoodle.embed.mongo.distribution.Version
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import io.ktor.server.routing.*
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import org.slf4j.event.Level
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun main() {
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

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(CallLogging) {
            level = Level.INFO
        }

        install(ContentNegotiation) {
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
        }
    }.start(wait = true)
}
