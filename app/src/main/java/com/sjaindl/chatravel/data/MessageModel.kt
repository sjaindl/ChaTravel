package com.sjaindl.chatravel.data

import com.sjaindl.chatravel.ui.chat.Message
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class CreateUserRequest(
    val userId: Long,
    val name: String,
    val interests: List<String> = emptyList(),
)

@Serializable
data class UserDto(
    val userId: Long,
    val name: String,
    val avatarUrl: String? = null,
    val interests: List<String> = emptyList(),
) {
    companion object {
        val Empty = UserDto(userId = 0, name = "")
    }
}

@Serializable
data class UsersResponse(
    val users: List<UserDto>
)

@Serializable
data class CreateConversationRequest(
    val firstUserId: Long,
    val secondUserId: Long,
    val interest: String,
)

@Serializable
data class ConversationDto(
    val conversationId: Long,
    val firstUserId: Long,
    val secondUserId: Long,
    val interest: String,
)

@Serializable
data class ConversationsResponse(
    val conversations: List<ConversationDto>
)

@Serializable
data class MessageDto(
    val messageId: Long,
    val conversationId: Long,
    val senderId: Long,
    val text: String,
    val createdAt: String, // ISO-8601 instant
) {
    companion object {
        operator fun plus(messages: List<MessageDto>): List<MessageDto> {
            return messages.sortedByDescending {
                it.createdAt
            }
        }

        val Initial = MessageDto(messageId = 0, conversationId = 0, senderId = 0, text = "Welcome to your new conversation!", createdAt = Instant.ofEpochMilli(0).toString())
    }

    fun toMessage(userId: Long?, userName: String?) = Message(
        id = messageId,
        conversationId = conversationId,
        sender = UserDto(
            userId = senderId,
            name = userName ?: "Anonymous"
        ),
        text = text,
        sentAt = Instant.parse(createdAt),
        isMine = senderId == userId,
    )
}

@Serializable
data class CreateMessageRequest(
    val conversationId: Long,
    val senderId: Long,
    val text: String,
)

@Serializable
data class MessagesResponse(
    val messages: List<MessageDto>,
    val serverTime: String,
)
