package com.sjaindl.chatravel.data.websocket

import androidx.core.net.toUri
import com.sjaindl.chatravel.data.MessageDto
import com.sjaindl.chatravel.data.room.ChatTravelDatabase
import com.sjaindl.chatravel.data.room.OutboxMessageEntity
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
import java.time.Instant

class WebSocketsMessagesApi(
    private val client: HttpClient,
    private val json: Json,
    private val database: ChatTravelDatabase,
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
        lastSync: String, // ISO-8601 instant
        onConnected: suspend () -> Unit,
        onMessage: suspend (MessageDto) -> Unit,
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
                            url.parameters.append("lastSync", lastSync)
                            // url.parameters.append("conversationId", conversationId.toString()) // auto-subscribe
                        }
                    ) {
                        attempt = 0
                        Napier.d("WebSocket connected")
                        onConnected()
                        drainOutbox(this)

                        coroutineScope {
                            val writer = launch {
                                sendMessageQueue.collect { msg ->
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

                            try {
                                joinAll(writer, reader)
                            } finally {
                                writer.cancel()
                                reader.cancel()
                            }
                        }
                    }
                    // normal close, reconnect after short delay
                    Napier.d("WebSocket normally closed, reconnecting...")
                    onDisconnected(null)
                    delay(1500)
                } catch (t: Exception) {
                    attempt++
                    Napier.e("WebSocket connect failed, attempt: $attempt", t)

                    if (attempt >= 5) {
                        Napier.e("WebSocket connect failed after 5 attempts, giving up.")
                        onDisconnected(t)
                        break // give up
                    }

                    val backoff = (1000L * attempt).coerceAtMost(10_000L)
                    delay(backoff)
                }
            }
        }
    }

    fun sendMessage(conversationId: Long, senderId: Long, text: String) {
        scope.launch {
            val localId = database.outboxDao().insert(
                OutboxMessageEntity(
                    conversationId = conversationId,
                    senderId = senderId,
                    text = text,
                    createdAtIso = Instant.now().toString()
                )
            )

            sendMessageQueue.emit(WsSendMessage(conversationId, senderId, text, localId))
        }
    }

    fun disconnect() {
        sessionJob?.cancel()
        sessionJob = null
    }

    private suspend fun drainOutbox(webSocketSession: DefaultClientWebSocketSession) {
        val pending = database.outboxDao().allMessagesWithOldestFirst()
        for (message in pending) {
            try {
                webSocketSession.sendSerialized(WsSubscribe(conversationId = message.conversationId))

                webSocketSession.sendSerialized(WsSendMessage(
                    conversationId = message.conversationId,
                    senderId = message.senderId,
                    text = message.text,
                    localId = message.id
                ))

                database.outboxDao().incAttempt(message.id)
            } catch (_: Throwable) {
                break
            }
        }
    }

    private suspend fun handleAck(evt: WsAck) {
        Napier.d("Web socket connection ACK: $evt")

        val localId = evt.localId ?: return
        database.outboxDao().delete(id = localId)
    }

    private suspend inline fun <reified T> DefaultClientWebSocketSession.sendSerialized(payload: T) {
        val txt = json.encodeToString(payload)
        send(Frame.Text(txt))
    }
}
