package com.sjaindl.chatravelserver.fcm

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.pushRoutes(
    tokenRepository: TokenRepository,
) {
    post("/push/register") {
        val body = call.receive<RegisterFcmRequest>()
        tokenRepository.upsert(userId = body.userId.toLong(), token = body.token)
        call.respond(mapOf("ok" to true))
    }
}
