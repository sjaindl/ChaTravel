package com.sjaindl.chatravelserver

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private val conversationSessions = ConcurrentHashMap<Long, MutableSet<DefaultWebSocketServerSession>>()

fun Route.websocketMessageRoutes(
    messagesRepository: MessagesRepository,
) {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    MessageBus.events.onEach { msg ->
        val sessions = conversationSessions[msg.conversationId] ?: return@onEach
        val payload = json.encodeToString(WsNewMessage(msg))
        sessions.forEach { session ->
            runCatching { session.send(Frame.Text(payload)) }
        }
    }.launchIn(CoroutineScope(Dispatchers.Default))

    route("/ws") {
        webSocket("/messages") {
            var conversationId = call.request.queryParameters["conversationId"]?.toLong()

            // If conversationId provided, auto-subscribe
            conversationId?.let {
                addSession(it, this)
            }

            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            when (val evt = json.decodeFromString<WsEvent>(text)) {
                                is WsSubscribe -> {
                                    conversationId?.let { removeSession(it, this) }
                                    conversationId = evt.conversationId
                                    addSession(evt.conversationId, this)
                                    send(json.encodeToString(WsAck(ok = true)))
                                }
                                is WsSendMessage -> {
                                    val ev = json.decodeFromString<WsSendMessage>(text)
                                    if (ev.text.isBlank()) {
                                        send(json.encodeToString(WsAck(false, null, "text must not be blank")))
                                    } else {
                                        val saved = messagesRepository.addMessage(
                                            conversationId = ev.conversationId,
                                            senderId = ev.senderId,
                                            date = Instant.now(),
                                            message = ev.text,
                                        )

                                        send(json.encodeToString(WsAck(true, saved.messageId, null)))
                                    }
                                }
                                else -> {
                                    send(json.encodeToString(WsAck(false, null, "unsupported event: $evt")))
                                }
                            }
                        }
                        is Frame.Ping -> send(Frame.Pong(frame.buffer))
                        else -> {}
                    }
                }
            } catch (throwable: Throwable) {
                conversationId?.let { removeSession(it, this) }
            }
        }
    }
}

private fun addSession(conversationId: Long, session: DefaultWebSocketServerSession) {
    conversationSessions.compute(conversationId) { _, set ->
        (set ?: mutableSetOf()).also { it.add(session) }
    }
}

private fun removeSession(conversationId: Long, session: DefaultWebSocketServerSession) {
    conversationSessions[conversationId]?.let { set ->
        set.remove(session)
        if (set.isEmpty()) conversationSessions.remove(conversationId)
    }
}
