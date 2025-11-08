package com.sjaindl.chatravel.data

import android.content.Context
import com.sjaindl.chatravel.BuildConfig
import com.sjaindl.chatravel.data.polling.LongPoller
import com.sjaindl.chatravel.data.polling.ShortPoller
import com.sjaindl.chatravel.data.prefs.UserSettingsRepository
import com.sjaindl.chatravel.data.room.ChatTravelDatabase
import com.sjaindl.chatravel.data.websocket.WebSocketFetcher
import com.sjaindl.chatravel.ui.chat.Conversation
import com.sjaindl.chatravel.ui.vm.ChatViewModel.ContentState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

class MessageFetcher(
    private val database: ChatTravelDatabase,
    private val shortPoller: ShortPoller,
    private val longPoller: LongPoller,
    private val webSocketFetcher: WebSocketFetcher,
    private val userRepository: UserRepository,
    private val messagesRepository: MessagesRepository,
    private val settingsRepository: UserSettingsRepository,
) {
    private var fetchJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun fetchChats(
        userId: Long,
        lastSync: String,
        context: Context,
        messageNetworkType: String,
        onContentState: (ContentState) -> Unit,
    ) {
        fetchJob?.cancel()

        when (messageNetworkType) {
            "SHORT_POLL" -> shortPoller.stop()
            "LONG_POLL" -> longPoller.stop()
            "WEBSOCKETS" -> webSocketFetcher.stop()
            else -> throw IllegalStateException("Unknown message network type")
        }

        coroutineScope {
            fetchJob = launch {
                onContentState(ContentState.Loading)

                val localConversations = database.conversationDao().allConversations().stateIn(
                    scope = this,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList(),
                )

                val localMessages = database.messageDao().getAllMessages().stateIn(
                    scope = this,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList(),
                )

                runCatching {
                    val networkMessageFlow = when (messageNetworkType) {
                        "SHORT_POLL" -> {
                            shortPoller.start(userId = userId, lastSync = lastSync)
                            shortPoller.messageFlow
                        }

                        "LONG_POLL" -> {
                            longPoller.start(userId = userId, lastSync = lastSync)
                            longPoller.messageFlow
                        }

                        "WEBSOCKETS" -> {
                            webSocketFetcher.start(
                                userId = userId,
                                lastSync = lastSync,
                                onDisconnected = {
                                    Napier.e("Disconnected from websocket", it)

                                    // switch to long polling when web socket connection is too flaky:
                                    fetchChats(
                                        userId = userId,
                                        lastSync = lastSync,
                                        context = context,
                                        messageNetworkType = "LONG_POLL",
                                        onContentState = onContentState,
                                    )
                                }
                            )
                            webSocketFetcher.messageFlow
                        }

                        else -> throw IllegalStateException("Unknown message network type")
                    }.onStart {
                        emit(emptyList())
                    }

                    combine(localConversations, localMessages, networkMessageFlow) { localConversations, localMessages, networkMessages ->
                        val newConversations = messagesRepository.getConversations(
                            userId = userId,
                            sinceIsoInstant = lastSync
                        )

                        val users = userRepository.getUsers().users

                        val me = users.first { user ->
                            user.userId == userId
                        }

                        val mapped = networkMessages.map {
                            val user = users.firstOrNull { user ->
                                user.userId == it.senderId
                            }

                            it.toMessage(
                                userId = userId,
                                userName = user?.name,
                            )
                        }.plus(
                            localMessages.map {
                                val user = users.firstOrNull { user ->
                                    user.userId == it.senderId
                                }

                                it.toMessage(
                                    userId = user?.userId,
                                    userName = user?.name,
                                )
                            }
                        ).groupBy { message ->
                            message.conversationId
                        }

                        val updatedConversations = mapped.map {
                            val interest = newConversations.conversations.find { conversation ->
                                it.key == conversation.conversationId
                            }?.interest ?: localConversations.find { conversation ->
                                it.key == conversation.conversationId
                            }?.interest

                            val secondUser = users.first { user ->
                                user.userId == it.value[0].sender.userId
                            }

                            Conversation(
                                id = it.key,
                                title = "$interest (${secondUser.name})",
                                participants = listOf(me, secondUser),
                                unreadCount = it.value.count { message ->
                                    val ids = context.readMessagesDataStore.data.firstOrNull()?.ids
                                        ?: return@count false
                                    ids.contains(message.id).not()
                                },
                                messages = it.value,
                            )
                        }

                        settingsRepository.setLastSync(Instant.now().toString())
                        onContentState(ContentState.Content(updatedConversations))
                    }.collect()
                }.onFailure {
                    if (it is CancellationException) throw it
                    Napier.e("Could not fetch messages", it)
                    onContentState(ContentState.Error(it))
                    stop()
                }
            }
        }
    }

    fun stop() {
        shortPoller.stop()
        longPoller.stop()
        webSocketFetcher.stop()
    }
}
