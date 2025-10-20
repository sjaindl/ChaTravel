package com.sjaindl.chatravel.data.websocket

import com.sjaindl.chatravel.data.MessageDto
import com.sjaindl.chatravel.data.MessagesRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.plus
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class WebSocketFetcher(
    private val messagesRepository: MessagesRepository,
    private val webSocketsMessagesApi: WebSocketsMessagesApi,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null

    private val _messageFlow = MutableStateFlow<List<MessageDto>>(emptyList())
    val messageFlow: StateFlow<List<MessageDto>> = _messageFlow

    fun start(userId: Long) {
        job?.cancel()

        job = scope.launch(Dispatchers.IO) {
            webSocketsMessagesApi.connect(
                userId = userId,
                onConnected = {
                    Napier.d("Connected to websocket")
                    val conversations = messagesRepository.getConversations(userId = userId).conversations

                    val messages = buildList {
                        addAll(
                            conversations.flatMap { conversation ->
                                buildList {
                                    add(
                                        MessageDto.Companion.Initial.copy(
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
                onMessage = { msg ->
                    _messageFlow.update { it + msg }
                }
            )
        }
    }

    fun sendMessage(
        conversationId: Long,
        senderId: Long,
        text: String
    ) {
        webSocketsMessagesApi.sendMessage(conversationId, senderId, text)
    }

    fun stop() {
        webSocketsMessagesApi.disconnect()
        job?.cancel()
    }
}
