package com.sjaindl.chatravelserver

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant

fun Route.generalRoutes() {
    get("/health") {
        call.respond(mapOf("status" to "ok", "time" to Instant.now().toString()))
    }
}

fun Route.userRoutes(userRepository: UserRepository) {

    route("/user") {
        // Create user
        post {
            val body = call.receive<CreateOrUpdateUserRequest>()
            if (body.name.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "user name must not be blank"))
                return@post
            }

            val user = userRepository.registerUser(userId = body.userId, name = body.name, interestValues = body.interests)

            call.respond(status = HttpStatusCode.Created, message = user)
        }

        // Update user interests
        put {
            val body = call.receive<CreateOrUpdateUserRequest>()

            val success = userRepository.updateUserInterests(userId = body.userId, interestValues = body.interests)

            call.respond(status = HttpStatusCode.Created, message = success)
        }

        get {
            val interest = call.request.queryParameters["interest"]
            if (interest == null) {
                // Get all users
                val users = userRepository.getUsers()
                call.respond(UsersResponse(users = users))
                return@get
            }

            // Get users by interest
            val users = userRepository.getUsersByInterest(interestValue = interest)
            call.respond(UsersResponse(users = users))
        }
    }
}

fun Route.conversationRoutes(
    messagesRepository: MessagesRepository,
    userRepository: UserRepository,
) {
    route("/conversation") {

        // Get conversations of user
        get {
            val userId = call.request.queryParameters["userId"]?.toLong()
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "User ID missing or not a number")
                return@get
            }

            val conversations = messagesRepository.getConversations(userId = userId)
            call.respond(ConversationsResponse(conversations = conversations))
        }

        // Start conversation
        post {
            val body = call.receive<CreateConversationRequest>()
            if (body.interest.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Interest must not be blank"))
                return@post
            }

            if (!userRepository.userExists(body.firstUserId)) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User ${body.firstUserId} not existing"))
                return@post
            }

            if (!userRepository.userExists(body.secondUserId)) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User ${body.secondUserId} not existing"))
                return@post
            }

            val conversation = with(body) {
                messagesRepository.startConversation(
                    firstUserId = firstUserId,
                    secondUserId = secondUserId,
                    interestValue = interest,
                )
            }
            call.respond(HttpStatusCode.Created, conversation)
        }
    }
}

fun Route.messagesRoutes(
    messagesRepository: MessagesRepository,
) {
    route("/message") {

        get {
            val since = call.request.queryParameters["since"]
            val conversationId = call.request.queryParameters["conversationId"]?.toLong()
            if (conversationId == null) {
                call.respond(HttpStatusCode.BadRequest, "Conversation ID missing or not a number")
                return@get
            }

            val sinceInstant = try {
                since?.let { Instant.parse(it) }
            } catch (e: Exception) {
                null
            }

            // Suggest a polling interval via headers (client may respect this)
            call.response.headers.append("X-Poll-Interval-Seconds", "5")

            // Basic cache hints to avoid intermediaries caching dynamic content
            call.response.headers.append(HttpHeaders.CacheControl, "no-store")

            val messages = messagesRepository.getMessages(conversationId = conversationId, since = sinceInstant)
            call.respond(MessagesResponse(messages = messages, serverTime = Instant.now().toString()))
        }

        get("/long") {
            val since = call.request.queryParameters["since"]
            val conversationId = call.request.queryParameters["conversationId"]?.toLong()
            if (conversationId == null) {
                call.respond(HttpStatusCode.BadRequest, "Conversation ID missing or not a number")
                return@get
            }

            val timeoutSeconds = call.request.queryParameters["timeoutSec"]?.toLongOrNull()?.coerceIn(5, 60) ?: 10L

            val sinceInstant = try {
                since?.let { Instant.parse(it) }
            } catch (e: Exception) {
                null
            }

            // hint to clients about server-held timeout
            call.response.headers.append("X-Long-Poll-Timeout-Seconds", timeoutSeconds.toString())
            call.response.headers.append(HttpHeaders.CacheControl, "no-store")

            // Fast path: if we already have newer messages, return immediately
            val initial = messagesRepository.getMessages(conversationId = conversationId, since = sinceInstant)
            if (initial.isNotEmpty()) {
                call.respond(MessagesResponse(messages = initial, serverTime = Instant.now().toString()))
                return@get
            }

            // Otherwise wait until a new matching message arrives or timeout
            val result = withTimeoutOrNull(timeoutSeconds * 1000) {
                MessageBus.events.first { message ->
                    message.conversationId == conversationId &&
                            (sinceInstant == null || Instant.parse(message.createdAt).isAfter(sinceInstant))
                }
            }

            if (result != null) {
                val newMessages = messagesRepository.getMessages(conversationId = conversationId, since = sinceInstant)
                call.respond(MessagesResponse(messages = newMessages, serverTime = Instant.now().toString()))
            } else {
                // Timeout: nothing new - return 200 with empty list (alternative: 204 No Content)
                call.respond(MessagesResponse(messages = emptyList(), serverTime = Instant.now().toString()))
            }
        }

        // Create message
        post {
            val body = call.receive<CreateMessageRequest>()
            if (body.text.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Text must not be blank"))
                return@post
            }

            messagesRepository.addMessage(
                conversationId = body.conversationId,
                senderId = body.senderId,
                date = Instant.now(),
                message = body.text,
            )

            call.respond(HttpStatusCode.Created)
        }
    }
}
