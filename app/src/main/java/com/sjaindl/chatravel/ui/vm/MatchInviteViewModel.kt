package com.sjaindl.chatravel.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sjaindl.chatravel.data.CreateConversationRequest
import com.sjaindl.chatravel.data.MessagesRepository
import com.sjaindl.chatravel.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class MatchUser(
    val id: Long,
    val name: String,
    val avatarUrl: String?,
    val interests: List<String>,
    val commonInterests: List<String>,
)

sealed interface MatchUiState {
    object Loading : MatchUiState
    data class Error(val message: String, val matchingUserId: Long) : MatchUiState
    data class Content(val user: MatchUser, val isStarting: Boolean = false) : MatchUiState
}

class MatchInviteViewModel() : ViewModel(), KoinComponent {

    private val userRepository: UserRepository by inject<UserRepository>()
    private val messagesRepository: MessagesRepository by inject<MessagesRepository>()

    private val _uiState = MutableStateFlow<MatchUiState>(MatchUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun refresh(matchingUserId: Long) {
        viewModelScope.launch {
            _uiState.value = MatchUiState.Loading

            val currentUser = userRepository.getCurrentUser()
            val otherUser = userRepository.getUsers().users.firstOrNull {
                it.userId == matchingUserId
            }

            if (currentUser != null && otherUser != null) {
                val commonInterests = currentUser.interests.filter {
                    otherUser.interests.contains(it)
                }

                val matchUser = MatchUser(
                    id = otherUser.userId,
                    name = otherUser.name,
                    avatarUrl = otherUser.avatarUrl,
                    interests = otherUser.interests,
                    commonInterests = commonInterests,
                )

                _uiState.value = MatchUiState.Content(matchUser)
            } else {
                _uiState.value = MatchUiState.Error("Could not load user", matchingUserId)
            }
        }
    }

    fun startChat(
        interest: String,
        onOpened: (conversationId: Long) -> Unit,
        onError: (String) -> Unit,
    ) {
        val current = (_uiState.value as? MatchUiState.Content) ?: return
        _uiState.update {
            current.copy(isStarting = true)
        }

        viewModelScope.launch {
            runCatching {
                val currentUserId = userRepository.getCurrentUser()?.userId ?: throw IllegalStateException("No user logged in")

                messagesRepository.startConversation(
                    CreateConversationRequest(
                        firstUserId = currentUserId,
                        secondUserId = current.user.id,
                        interest = interest,
                    ),
                )
            }.onSuccess {
                onOpened(it)
            }.onFailure {
                _uiState.update {
                    current.copy(isStarting = false)
                }

                onError(it.message ?: "Could not start conversation")
            }
        }
    }
}
