package com.sjaindl.chatravel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sjaindl.chatravel.data.CreateConversationRequest
import com.sjaindl.chatravel.data.CreateMessageRequest
import com.sjaindl.chatravel.data.MessagesRepository
import com.sjaindl.chatravel.data.ShortPoller
import com.sjaindl.chatravel.data.UserDto
import com.sjaindl.chatravel.data.UserRepository
import com.sjaindl.chatravel.data.readMessagesDataStore
import com.sjaindl.chatravel.ui.chat.Conversation
import com.sjaindl.chatravel.ui.chat.Message
import com.sjaindl.chatravel.ui.profile.Interest
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
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

    private val shortPoller: ShortPoller by inject<ShortPoller>()

    private val userRepository: UserRepository by inject<UserRepository>()
    private val messagesRepository: MessagesRepository by inject<MessagesRepository>()

    private var _contentState: MutableStateFlow<ContentState> = MutableStateFlow(ContentState.Initial)
    val contentState: StateFlow<ContentState> = _contentState.asStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ContentState.Initial,
        )

    override fun onCleared() {
        super.onCleared()
        shortPoller.stop()
    }

    fun markAsRead(messageIds: List<Long>, context: Context) = viewModelScope.launch {
        context.readMessagesDataStore.updateData {
            it.copy(ids = it.ids + messageIds)
        }
    }

    fun startConversation(userId: Long, interest: Interest) = viewModelScope.launch {
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
        }.onFailure {
            if (it is CancellationException) throw it
            Napier.e("Could not start conversation", it)
            _contentState.value = ContentState.Error(it)
        }
    }

    fun sendMessage(conversationId: Long, text: String) = viewModelScope.launch {
        val currentUserId = userRepository.getCurrentUser()?.userId ?: return@launch

        runCatching {
            messagesRepository.createMessage(
                body = CreateMessageRequest(
                    conversationId = conversationId,
                    senderId = currentUserId,
                    text = text
                )
            )
        }.onSuccess {
            Napier.d("Send message successfully")
        }.onFailure {
            if (it is CancellationException) throw it
            Napier.e("Could not start conversation", it)
            _contentState.value = ContentState.Error(it)
        }
    }

    fun poll(userId: Long, context: Context) = viewModelScope.launch {
        _contentState.value = ContentState.Loading

        runCatching {
            shortPoller.start(userId = userId)

            shortPoller.messageFlow.collectLatest { messageList ->
                val currentUserId = userRepository.getCurrentUser()?.userId ?: return@collectLatest

                val conversations = messagesRepository.getConversations(currentUserId)
                val users = userRepository.getUsers().users

                val mapped = messageList.map {
                    Message(
                        id = it.messageId,
                        conversationId = it.conversationId,
                        sender = UserDto(
                            userId = it.senderId,
                            name = users.first {
                                    user -> user.userId == it.senderId
                            }.name
                        ),
                        text = it.text,
                        sentAt = Instant.parse(it.createdAt),
                        isMine = it.senderId == currentUserId,
                    )
                }.groupBy { message ->
                    message.conversationId
                }

                val updatedConversations = mapped.map {
                    val interest = conversations.conversations.find { conversation ->
                        it.key == conversation.conversationId
                    }?.interest

                    val me = users.first { user ->
                        user.userId == currentUserId
                    }

                    val secondUser = users.first { user ->
                        user.userId == it.value[0].sender.userId
                    }

                    Conversation(
                        id = it.key,
                        title = "$interest (${secondUser.name})",
                        participants = listOf(me, secondUser),
                        unreadCount = it.value.count { message ->
                            val ids = context.readMessagesDataStore.data.firstOrNull()?.ids ?: return@count false
                            ids.contains(message.id).not()
                        },
                        messages = it.value,
                    )
                }

                _contentState.value = ContentState.Content(updatedConversations)
            }
        }.onFailure {
            if (it is CancellationException) throw it
            Napier.e("Could not poll messages", it)
            _contentState.value = ContentState.Error(it)
            shortPoller.stop()
        }
    }
}
