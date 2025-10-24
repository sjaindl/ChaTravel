package com.sjaindl.chatravel.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sjaindl.chatravel.data.UserRepository
import com.sjaindl.chatravel.data.grpc.ChatSyncClient
import com.sjaindl.chatravel.ui.chat.Message
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant

class ChatSyncViewModel : ViewModel(), KoinComponent {
    private val client = ChatSyncClient(scope = viewModelScope)

    private val userRepository: UserRepository by inject<UserRepository>()

    val messages = client.messages.map {  messages ->
        messages.mapNotNull {
            val user = userRepository.getUsers().users.firstOrNull { user ->
                user.userId == it.senderId
            } ?: return@mapNotNull null

            Message(
                id = it.messageId,
                conversationId = it.conversationId,
                sender = user,
                text = it.text,
                sentAt = Instant.parse(it.createdAtIso),
                isMine = it.senderId == userRepository.getCurrentUser()?.userId,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    fun start(conversationId: Long, lastSeen: Instant?) {
        client.start(conversationId = conversationId, lastSeenIso = lastSeen)
    }

    fun send(conversationId: Long, me: Long, text: String) {
        client.send(conversationId = conversationId, userId = me, text = text)
    }

    override fun onCleared() {
        viewModelScope.launch {
            client.stop()
        }

        super.onCleared()
    }
}
