package com.sjaindl.chatravel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sjaindl.chatravel.data.CreateConversationRequest
import com.sjaindl.chatravel.data.MessagesRepository
import com.sjaindl.chatravel.data.ShortPoller
import com.sjaindl.chatravel.data.UserRepository
import com.sjaindl.chatravel.ui.chat.Conversation
import com.sjaindl.chatravel.ui.chat.Message
import com.sjaindl.chatravel.ui.chat.User
import com.sjaindl.chatravel.ui.profile.Interest
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant

class AppViewModel: ViewModel(), KoinComponent {

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
            poll(userId = currentUserId)
        }.onFailure {
            if (it is CancellationException) throw it
            Napier.e("Could not start conversation", it)
            _contentState.value = ContentState.Error(it)
        }
    }

    fun poll(userId: Long) = viewModelScope.launch {
        val conversations = (contentState.value as? ContentState.Content)?.conversations ?: return@launch

        _contentState.value = ContentState.Loading

        runCatching {
            shortPoller.start(userId = userId)
            shortPoller.messageFlow.collect {
                val mapped = Message(
                    id = it.id,
                    conversationId = it.conversationId,
                    sender = User(id = it.senderId, displayName = "TODO"),
                    text = it.text,
                    sentAt = Instant.now(),
                    isMine = false,
                )

                val index = conversations.indexOfFirst { conversation ->
                    conversation.id == mapped.conversationId
                }

                if (index == -1) return@collect

                val localConversation = conversations[index]
                val updatedConversation = localConversation.copy(messages = localConversation.messages + mapped)

                val updatedConversations = conversations.toMutableList().apply {
                    removeAt(index)
                    add(index, updatedConversation)
                }

                _contentState.value = ContentState.Content(updatedConversations)
            }
        }.onFailure {
            if (it is CancellationException) throw it
            Napier.e("Could not poll messages", it)
            _contentState.value = ContentState.Error(it)
        }
    }
}
