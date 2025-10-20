package com.sjaindl.chatravelserver.sse

import com.sjaindl.chatravelserver.Interest
import com.sjaindl.chatravelserver.UserRepository
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.Instant

fun Route.SSERoutes(userRepository: UserRepository) {
    route("/sse") {
        // GET /sse/interest-matches?userId=123
        get("/interest-matches") {
            val userId = call.request.queryParameters["userId"]?.toLongOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "userId missing or not a number")
                return@get
            }

            val myInterests: Set<Interest> = runCatching {
                userRepository.getUserInterests(userId).toSet()
            }.getOrDefault(emptySet())

            call.response.cacheControl(CacheControl.NoStore(null))

            call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                val writer = this

                fun writeEvent(event: String? = null, id: String? = null, data: String) {
                    if (event != null) writer.write("event: $event\n")
                    if (id != null) writer.write("id: $id\n")
                    writer.write("data: $data\n\n")
                    writer.flush()
                }

                fun heartbeat() {
                    // SSE comment line; most proxies accept this as keepalive
                    writer.write(": keep-alive ${Instant.now()}\n\n")
                    writer.flush()
                }

                val scope = CoroutineScope(currentCoroutineContext())

                val heartbeatJob = scope.launch {
                    while (isActive) {
                        delay(15_000)
                        heartbeat()
                    }
                }

                // Event collector with filtering by interest overlap
                val collectorJob = InterestMatchBus.events
                    .onEach { ev ->
                        // require at least one overlapping interest
                        if (ev.userId != userId && ev.interests.any { it in myInterests }) {
                            val json = """{"userId":${ev.userId},"name":"${ev.name}","interests":[${ev.interests.joinToString(","){ "\"$it\"" }}]}"""
                            writeEvent(event = "match", id = ev.userId.toString(), data = json)
                        }
                    }
                    .launchIn(scope)

                // Optionally send a “connected” event once
                writeEvent(event = "connected", data = """{"time":"${Instant.now()}","interests":[${myInterests.joinToString(","){ "\"$it\"" }}]}""")

                // Keep the handler alive until client disconnects
                try {
                    awaitCancellation()
                } finally {
                    heartbeatJob.cancel()
                    collectorJob.cancel()
                }
            }
        }
    }
}
