package com.sjaindl.chatravelserver.graphql

import com.sjaindl.chatravelserver.MessagesRepository
import com.sjaindl.chatravelserver.UserRepository
import graphql.GraphQL
import graphql.schema.DataFetcher
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.temporal.ChronoUnit

data class GTopMatch(
    val user: GUser,
    val score: Int,
    val commonInterestsCount: Int,
    val commonInterests: List<String>,
    val activeConversations: Int,
)

data class GUser(
    val userId: String,
    val name: String,
    val interests: List<String>,
    val avatarUrl: String?,
)

class TopMatchesResolver(
    private val userRepository: UserRepository,
    private val messages: MessagesRepository,
) {
    suspend fun topMatches(userId: Long, limit: Int): List<GTopMatch> {
        val user = userRepository.getUser(userId) ?: return emptyList()
        val userInterests = user.interests

        // Search for candidates that share at least one interest
        val candidates = userRepository.getUsersWithSameInterests(interests = userInterests)
            .filter { it.userId != userId }

        val since = Instant.now().minus(30, ChronoUnit.DAYS)

        val scored = candidates.map { user ->
            val commonInterests = user.interests.filter {
                it in userInterests
            }.map {
                it.name
            }

            // Score results by number of common interests and the amount of active conversations
            val activeConversations = messages.countActiveConversationsForUserSince(userId = user.userId, since = since)
            val score = commonInterests.size * 10 + activeConversations

            GTopMatch(
                user = GUser(
                    userId = user.userId.toString(),
                    name = user.name,
                    interests = user.interests.map { it.name },
                    avatarUrl = user.avatarUrl,
                ),
                score = score,
                commonInterestsCount = commonInterests.size,
                commonInterests = commonInterests,
                activeConversations = activeConversations
            )
        }.sortedWith(
            compareByDescending<GTopMatch> { it.score }
                .thenByDescending { it.commonInterestsCount }
                .thenByDescending { it.activeConversations }
                .thenBy { it.user.name.lowercase() }
        )

        return scored.take(limit)
    }

    suspend fun user(id: Long): GUser? {
        val user = userRepository.getUser(userId = id) ?: return null

        return GUser(
            userId = user.userId.toString(),
            name = user.name,
            interests = user.interests.map { it.name },
            avatarUrl = user.avatarUrl,
        )
    }
}

fun buildGraphQL(
    userRepository: UserRepository,
    messagesRepository: MessagesRepository,
    sdl: String,
): GraphQL {
    val typeRegistry: TypeDefinitionRegistry = SchemaParser().parse(sdl)
    val resolver = TopMatchesResolver(userRepository = userRepository, messages = messagesRepository)

    val wiring = RuntimeWiring.newRuntimeWiring()
        .type("Query") { typeWiring ->
            typeWiring
                .dataFetcher("topMatches", DataFetcher { env ->
                    val userId = env.getArgument<String>("userId")!!.toLong()
                    val limit = env.getArgument<Int?>("limit") ?: 20

                    // graphql-java fetchers are synchronous
                    runBlocking {
                        resolver.topMatches(userId = userId, limit = limit)
                    }
                })
                .dataFetcher("user", DataFetcher { env ->
                    val userId = env.getArgument<String>("userId")!!.toLong()
                    runBlocking {
                        resolver.user(userId)
                    }
                })
        }
        .build()

    val schema = SchemaGenerator().makeExecutableSchema(typeRegistry, wiring)
    return GraphQL.newGraphQL(schema).build()
}
