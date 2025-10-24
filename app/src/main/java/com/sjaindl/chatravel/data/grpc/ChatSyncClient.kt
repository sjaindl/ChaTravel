package com.sjaindl.chatravel.data.grpc

import com.chatravel.grpc.v1.ChatClientEvent
import com.chatravel.grpc.v1.ChatServerEvent
import com.chatravel.grpc.v1.ChatServiceGrpcKt
import com.chatravel.grpc.v1.ClientHello
import com.chatravel.grpc.v1.ClientSendMessage
import com.chatravel.grpc.v1.Message
import io.github.aakira.napier.Napier
import io.grpc.HttpConnectProxiedSocketAddress
import io.grpc.ManagedChannel
import io.grpc.ProxyDetector
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.net.SocketAddress
import java.time.Instant
import java.util.concurrent.TimeUnit

private object NoProxyDetector : ProxyDetector {
    override fun proxyFor(targetServerAddress: SocketAddress?): HttpConnectProxiedSocketAddress? = null
}

class ChatSyncClient(
    host: String = "10.0.2.2",
    port: Int = 9090,
    private val scope: CoroutineScope
) {
    private val channel: ManagedChannel = OkHttpChannelBuilder
        .forAddress(host, port)
        .proxyDetector(NoProxyDetector)
        .usePlaintext() // just for local dev (prod should use TLS)
        .build()

    private val stub = ChatServiceGrpcKt.ChatServiceCoroutineStub(channel)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private var streamJob: Job? = null
    private val outgoing = MutableSharedFlow<ChatClientEvent>(extraBufferCapacity = 64)

    fun start(conversationId: Long, lastSeenIso: Instant?) {
        streamJob?.cancel()

        streamJob = scope.launch(Dispatchers.IO) {
            val requests = flow {
                emit(ChatClientEvent.newBuilder().setHello(
                    ClientHello.newBuilder()
                        .setConversationId(conversationId)
                        .apply { if (lastSeenIso != null) setLastSeenMessageIso(lastSeenIso.toString()) }
                ).build())
            }.onCompletion {
            /* keep stream alive after hello */
            }.mergeWith(outgoing)

            val responses: Flow<ChatServerEvent> = stub.chatStream(requests)

            responses.collect { ev ->
                when (ev.kindCase) {
                    ChatServerEvent.KindCase.BACKFILL -> {
                        val messages = ev.backfill.messagesList
                        if (messages.isNotEmpty()) {
                            _messages.update {
                                (it + messages).distinctBy { message ->
                                    message.messageId
                                }.sortedBy { message ->
                                    message.createdAtIso
                                }
                            }
                        }
                    }
                    ChatServerEvent.KindCase.ACK -> {
                        Napier.d("ACK: ${ev.ack.messageId}")
                    }
                    ChatServerEvent.KindCase.HEARTBEAT -> {
                        Napier.d("HEARTBEAT: ${ev.heartbeat.serverTimeIso}")
                    }
                    ChatServerEvent.KindCase.KIND_NOT_SET -> {
                        Napier.e("Unknown event: $ev")
                    }
                }
            }
        }
    }

    fun send(conversationId: Long, userId: Long, text: String) {
        val evt = ChatClientEvent.newBuilder().setSend(
            ClientSendMessage.newBuilder()
                .setConversationId(conversationId)
                .setUserId(userId)
                .setText(text)
        ).build()
        outgoing.tryEmit(evt)
    }

    suspend fun stop() {
        streamJob?.cancelAndJoin()
        channel.shutdown().awaitTermination(3, TimeUnit.SECONDS)
    }
}

private fun <T> Flow<T>.mergeWith(other: Flow<T>): Flow<T> = channelFlow {
    val s1 = launch { collect { send(it) } }
    val s2 = launch { other.collect { send(it) } }
    awaitClose { s1.cancel(); s2.cancel() }
}
