package com.sjaindl.chatravel.data

import kotlinx.serialization.Serializable

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
    val id: Long,
    val interest: String,
    val participantUserIds: List<Long> = emptyList(),
    val lastMessagePreview: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class ConversationsResponse(
    val conversations: List<ConversationDto>
)

@Serializable
data class MessageDto(
    val id: Long,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val createdAt: String, // ISO-8601 instant
)


@Serializable
data class CreateMessageRequest(
    val conversationId: String,
    val senderId: String,
    val text: String,
)


@Serializable
data class MessagesResponse(
    val messages: List<MessageDto>,
    val serverTime: String,
)
