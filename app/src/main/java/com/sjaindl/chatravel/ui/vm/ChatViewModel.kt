package com.sjaindl.chatravel.ui.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sjaindl.chatravel.BuildConfig
import com.sjaindl.chatravel.data.CreateConversationRequest
import com.sjaindl.chatravel.data.CreateMessageRequest
import com.sjaindl.chatravel.data.MessageFetcher
import com.sjaindl.chatravel.data.MessagesRepository
import com.sjaindl.chatravel.data.UserRepository
import com.sjaindl.chatravel.data.prefs.UserSettingsRepository
import com.sjaindl.chatravel.data.readMessagesDataStore
import com.sjaindl.chatravel.data.websocket.WebSocketFetcher
import com.sjaindl.chatravel.ui.chat.Conversation
import com.sjaindl.chatravel.ui.profile.Interest
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant

class ChatViewModel: ViewModel(), KoinComponent {

    sealed class ContentState {
        data object Initial: ContentState()

        data object Loading: ContentState()

        data class Content(val conversations: List<Conversation>): ContentState()

        data class Error(val throwable: Throwable): ContentState()
    }

    private val messageFetcher: MessageFetcher by inject<MessageFetcher>()

    private val userRepository: UserRepository by inject<UserRepository>()
    private val messagesRepository: MessagesRepository by inject<MessagesRepository>()
    private val settingsRepository: UserSettingsRepository by inject<UserSettingsRepository>()
    private val webSocketFetcher: WebSocketFetcher by inject<WebSocketFetcher>()

    private var _contentState: MutableStateFlow<ContentState> = MutableStateFlow(ContentState.Initial)
    val contentState: StateFlow<ContentState> = _contentState.asStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ContentState.Initial,
        )

    val lastSync = settingsRepository.prefs.map {
        Instant.ofEpochMilli(0).toString()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
        initialValue = Instant.ofEpochMilli(0).toString(),
    )

    override fun onCleared() {
        super.onCleared()

        messageFetcher.stop()
    }

    fun markAsRead(messageIds: List<Long>, context: Context) = viewModelScope.launch {
        context.readMessagesDataStore.updateData {
            it.copy(ids = it.ids + messageIds)
        }
    }

    fun startConversation(userId: Long, interest: Interest, lastSync: String, context: Context) = viewModelScope.launch {
        _contentState.value = ContentState.Loading

        val currentUserId = userRepository.getCurrentUser()?.userId

        if (currentUserId == null) {
            _contentState.value = ContentState.Error(IllegalStateException("No current user"))
            return@launch
        }

        runCatching {
            val request = CreateConversationRequest(
                firstUserId = currentUserId,
                secondUserId = userId,
                interest = interest.name,
            )

            messagesRepository.startConversation(request = request)
        }.onSuccess {
            Napier.d("Started conversation with $userId")
            fetchChats(userId = currentUserId, lastSync = lastSync, context = context)
        }.onFailure {
            if (it is CancellationException) throw it
            Napier.e("Could not start conversation", it)
            _contentState.value = ContentState.Error(it)
        }
    }

    fun sendMessage(conversationId: Long, text: String) = viewModelScope.launch {
        val currentUserId = userRepository.getCurrentUser()?.userId ?: return@launch

        runCatching {
            when (BuildConfig.MESSAGE_NETWORK_TYPE) {
                "SHORT_POLL", "LONG_POLL" -> messagesRepository.createMessage(
                    body = CreateMessageRequest(
                        conversationId = conversationId,
                        senderId = currentUserId,
                        text = text
                    )
                )
                "WEBSOCKETS" -> webSocketFetcher.sendMessage(
                    conversationId = conversationId,
                    senderId = currentUserId,
                    text = text,
                )
                else -> throw IllegalStateException("Unknown message network type")
            }
        }.onSuccess {
            Napier.d("Send message successfully")
        }.onFailure {
            if (it is CancellationException) throw it
            Napier.e("Could not send message", it)
            _contentState.value = ContentState.Error(it)
        }
    }

    fun fetchChats(userId: Long, lastSync: String, context: Context) {
        viewModelScope.launch {
            messageFetcher.fetchChats(
                userId = userId,
                lastSync = lastSync,
                context = context,
                messageNetworkType = BuildConfig.MESSAGE_NETWORK_TYPE,
            ) {
                _contentState.value = it
            }
        }
    }
}
