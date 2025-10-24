package com.sjaindl.chatravel.data.websocket

import androidx.core.net.toUri
import com.sjaindl.chatravel.data.MessageDto
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class WebSocketsMessagesApi(
    private val client: HttpClient,
    private val json: Json,
    private val baseUrl: String = "ws://10.0.2.2:8080",
) {

    private var sessionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val sendMessageQueue = MutableSharedFlow<WsSendMessage>(
        replay = 0,
        extraBufferCapacity = 64
    )

    fun connect(
        userId: Long,
        onMessage: suspend (MessageDto) -> Unit,
        onConnected: suspend () -> Unit = {},
        onDisconnected: suspend (Throwable?) -> Unit = {},
    ) {
        if (sessionJob?.isActive == true) return

        sessionJob = scope.launch(Dispatchers.IO) {
            var attempt = 0
            while (isActive) {
                try {
                    client.webSocket(
                        method = HttpMethod.Get,
                        host = baseUrl.toUri().host,
                        port = baseUrl.toUri().port.takeIf { it != -1 } ?: 80,
                        path = "/ws/messages",
                        request = {
                            url.protocol = if (baseUrl.startsWith("wss")) URLProtocol.WSS else URLProtocol.WS
                            url.parameters.append("userId", userId.toString())
                           // url.parameters.append("conversationId", conversationId.toString()) // auto-subscribe
                        }
                    ) {
                        onConnected()
                        attempt = 0

                        coroutineScope {
                            val writer = launch {
                                // Collect queued sends and write them to the active session
                                sendMessageQueue.collect { msg ->
                                    // (Optional) subscribe once per conversation instead of every send:
                                    // sendSerialized(WsSubscribe(conversationId = msg.conversationId))
                                    sendSerialized(WsSubscribe(conversationId = msg.conversationId))
                                    sendSerialized(msg)
                                }
                            }

                            val reader = launch {
                                for (frame in incoming) {
                                    when (frame) {
                                        is Frame.Text -> {
                                            when (val evt = json.decodeFromString<WsEvent>(frame.readText())) {
                                                is WsNewMessage -> onMessage(evt.message)
                                                is WsAck -> handleAck(evt)
                                                else -> Unit
                                            }
                                        }
                                        is Frame.Ping -> send(Frame.Pong(frame.buffer))
                                        else -> Unit
                                    }
                                }
                            }

                            // If either fails/finishes, cancel the other
                            try {
                                joinAll(writer, reader)
                            } finally {
                                writer.cancel()
                                reader.cancel()
                            }
                        }
                    }
                } catch (t: Throwable) {
                    onDisconnected(t)
                    attempt++
                    val backoff = (1000L * attempt).coerceAtMost(10_000L)
                    delay(backoff)
                    continue
                }

                // normal close, reconnect after short delay
                onDisconnected(null)
                delay(1500)
            }
        }
    }

    fun sendMessage(conversationId: Long, senderId: Long, text: String) {
        if (!sendMessageQueue.tryEmit(WsSendMessage(conversationId, senderId, text))) {
            scope.launch { sendMessageQueue.emit(WsSendMessage(conversationId, senderId, text)) }
        }
    }

    fun disconnect() {
        sessionJob?.cancel()
        sessionJob = null
    }

    private fun handleAck(evt: WsAck) {
        Napier.d("Web socket connection ACK: $evt")
    }

    // Send serialized JSON over WS
    private suspend inline fun <reified T> DefaultClientWebSocketSession.sendSerialized(payload: T) {
        val txt = json.encodeToString(payload)
        send(Frame.Text(txt))
    }
}
