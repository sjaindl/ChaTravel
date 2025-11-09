package com.sjaindl.chatravel.data.websocket

import com.sjaindl.chatravel.data.MessageDto
import com.sjaindl.chatravel.data.MessagesRepository
import com.sjaindl.chatravel.data.room.ChatTravelDatabase
import com.sjaindl.chatravel.data.room.MessageEntity
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.collections.plus

class WebSocketFetcher(
    private val messagesRepository: MessagesRepository,
    private val webSocketsMessagesApi: WebSocketsMessagesApi,
    private val database: ChatTravelDatabase,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    private val _messageFlow = MutableStateFlow<List<MessageDto>>(emptyList())
    val messageFlow: StateFlow<List<MessageDto>> = _messageFlow

    fun start(
        userId: Long,
        lastSync: String, // ISO-8601 instant
        onDisconnected: suspend (Throwable?) -> Unit,
    ) {
        job?.cancel()

        job = scope.launch(Dispatchers.IO) {
            webSocketsMessagesApi.connect(
                userId = userId,
                lastSync = lastSync,
                onConnected = {
                    Napier.d("Connected to websocket")
                    val conversations = messagesRepository.getConversations(userId = userId, sinceIsoInstant = lastSync).conversations

                    val messages = buildList {
                        addAll(
                            conversations.flatMap { conversation ->
                                buildList {
                                    add(
                                        MessageDto.Initial.copy(
                                            conversationId = conversation.conversationId,
                                            senderId = conversation.secondUserId,
                                        )
                                    )
                                }.sortedBy {
                                    it.createdAt
                                }
                            }
                        )
                    }

                    _messageFlow.emit(messages)
                },
                onMessage = { message ->
                    _messageFlow.update { it + message }.also {
                        database.messageDao().upsert(
                            MessageEntity(
                                messageId = message.messageId,
                                conversationId = message.conversationId,
                                senderId = message.senderId,
                                text = message.text,
                                createdAt = Instant.now().toString(),
                            )
                        )
                    }
                },
                onDisconnected = onDisconnected,
            )
        }
    }

    fun sendMessage(
        conversationId: Long,
        senderId: Long,
        text: String
    ) {
        webSocketsMessagesApi.sendMessage(
            conversationId = conversationId,
            senderId = senderId,
            text = text,
        )
    }

    fun stop() {
        webSocketsMessagesApi.disconnect()
        job?.cancel()
    }
}
