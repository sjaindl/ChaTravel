package com.sjaindl.chatravelserver.grpc

import com.chatravel.grpc.v1.ChatClientEvent
import com.chatravel.grpc.v1.ChatServerEvent
import com.chatravel.grpc.v1.ChatServiceGrpcKt
import com.chatravel.grpc.v1.Message
import com.chatravel.grpc.v1.ServerAck
import com.chatravel.grpc.v1.ServerBackfill
import com.chatravel.grpc.v1.ServerHeartbeat
import com.sjaindl.chatravelserver.MessagesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.time.Instant
import kotlin.collections.filter

class ChatService(
    private val messagesRepository: MessagesRepository,
) : ChatServiceGrpcKt.ChatServiceCoroutineImplBase() {

    override fun chatStream(requests: Flow<ChatClientEvent>): Flow<ChatServerEvent> = channelFlow {
        var conversationId: Long? = null
        var lastSeenMessageIso: String? = null

        val messageSyncJob = launch {
            requests.collect { event ->
                when (event.kindCase) {
                    ChatClientEvent.KindCase.HELLO -> {
                        val helloEvent = event.hello
                        conversationId = helloEvent.conversationId
                        lastSeenMessageIso = helloEvent.lastSeenMessageIso

                        val backlog = messagesRepository.getMessages(conversationId = conversationId, since = null)

                        val missingMessages = backlog.filter {
                            lastSeenMessageIso == null || it.createdAt < lastSeenMessageIso
                        }

                        val backfill = ServerBackfill.newBuilder().apply {
                            missingMessages.forEach { m ->
                                addMessages(m.toProto())
                            }
                        }.build()

                        send(ChatServerEvent.newBuilder().setBackfill(backfill).build())
                    }

                    ChatClientEvent.KindCase.SEND -> {
                        val sendEvent = event.send

                        if (sendEvent.text.isBlank()) {
                            send(ChatServerEvent.newBuilder().setAck(
                                ServerAck.newBuilder().setOk(false).setError("text must not be blank")
                            ).build())
                        } else {
                            val saved = messagesRepository.addMessage(
                                conversationId = sendEvent.conversationId,
                                senderId = sendEvent.userId,
                                date = Instant.now(),
                                message = sendEvent.text,
                            )
                            
                            send(ChatServerEvent.newBuilder().setAck(
                                ServerAck.newBuilder().setOk(true).setMessageId(saved.messageId)
                            ).build())
                        }
                    }

                    ChatClientEvent.KindCase.ACK -> {
                        send(ChatServerEvent.newBuilder().setAck(
                            ServerAck.newBuilder().setOk(true)
                        ).build())
                    }

                    ChatClientEvent.KindCase.KIND_NOT_SET -> {
                        send(ChatServerEvent.newBuilder().setAck(
                            ServerAck.newBuilder().setOk(false).setError("Unknown event")
                        ).build())
                    }
                }
            }
        }

        val heartBeatJob = launch {
            while (isActive) {
                delay(25_000)
                send(ChatServerEvent.newBuilder().setHeartbeat(
                    ServerHeartbeat.newBuilder().setServerTimeIso(Instant.now().toString())
                ).build())
            }
        }

        awaitClose {
            messageSyncJob.cancel()
            heartBeatJob.cancel()
        }
    }
}

private fun com.sjaindl.chatravelserver.Message.toProto(): Message =
    Message.newBuilder()
        .setMessageId(messageId)
        .setConversationId(conversationId)
        .setSenderId(senderId)
        .setText(text)
        .setCreatedAtIso(createdAt)
        .build()
