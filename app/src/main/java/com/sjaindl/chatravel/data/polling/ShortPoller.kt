package com.sjaindl.chatravel.data.polling

import com.sjaindl.chatravel.data.MessageDto
import com.sjaindl.chatravel.data.MessagesRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class ShortPoller(
    private val messagesRepository: MessagesRepository,
    private val scope: CoroutineScope,
    private val pollInterval: Duration = 5.seconds,
) {
    private var lastSeen: Instant? = null
    private var job: Job? = null

    private val _messageFlow = MutableSharedFlow<List<MessageDto>>()
    val messageFlow = _messageFlow.asSharedFlow()

    fun start(userId: Long, lastSync: String) {
        if (job?.isActive == true) return

        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                runCatching {
                    val conversations = messagesRepository.getConversations(
                        userId = userId,
                        sinceIsoInstant = lastSync,
                    ).conversations

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
                                        messagesRepository.getMessages(
                                            conversationId = conversation.conversationId,
                                            sinceIsoInstant = null, // use iso instant to get latest messages
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
                        Napier.e("Error during short polling", throwable)
                        null
                    }

                    messages
                }.onSuccess {
                    _messageFlow.emit(it)
                }.onFailure {
                    if (it is CancellationException) {
                        throw it
                    } else {
                        Napier.e("Error during short polling", it)
                        throw it
                    }
                }

                delay(pollInterval)
            }
        }
    }

    fun stop() {
        job?.cancel()
    }
}
