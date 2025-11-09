package com.sjaindl.chatravel.data.polling

import com.sjaindl.chatravel.data.MessageDto
import com.sjaindl.chatravel.data.MessagesRepository
import io.github.aakira.napier.Napier
import io.ktor.network.sockets.SocketTimeoutException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.ConnectException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class LongPoller(
    private val messagesRepository: MessagesRepository,
    private val scope: CoroutineScope,
) {
    private var lastSeen: Instant? = null
    private var job: Job? = null

    private val _messageFlow = MutableStateFlow<List<MessageDto>>(emptyList())
    val messageFlow = _messageFlow.asStateFlow()

    fun start(userId: Long, lastSync: String) {
        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                runCatching {
                    val conversations = messagesRepository.getConversations(userId = userId, sinceIsoInstant = lastSync).conversations

                    val iso = lastSeen?.toString()

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

                                    addAll(
                                        messagesRepository.getMessagesLongPolling(
                                            conversationId = conversation.conversationId,
                                            sinceIsoInstant = iso,
                                        ).messages
                                    )
                                }.sortedBy {
                                    it.createdAt
                                }
                            }
                        )
                    }

                    lastSeen = try {
                        Instant.parse(
                            messages.maxOf {
                                it.createdAt
                            }
                        )
                    } catch (throwable: Throwable) {
                        Napier.e("Error during long polling", throwable)
                        null
                    }

                    messages
                }.onSuccess {
                    val list = messageFlow.value.toMutableList().apply {
                        addAll(it)
                    }
                    _messageFlow.emit(list)
                }.onFailure {
                    when (it) {
                        is CancellationException -> {
                            throw it
                        }

                        is SocketTimeoutException -> {
                            // restart long polling after timeout
                            start(userId = userId, lastSync = lastSync)
                        }

                        is ConnectException -> {
                            _messageFlow.emit(emptyList())
                        }

                        else -> {
                            Napier.e("Error during long polling", it)
                            throw it
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
    }
}
