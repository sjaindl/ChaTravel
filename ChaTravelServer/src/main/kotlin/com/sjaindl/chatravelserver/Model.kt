package com.sjaindl.chatravelserver

import kotlinx.serialization.Serializable

enum class Interest {
    SPORTS,
    TREKKING,
    SIGHTSEEING,
    FOOD,
    CULTURE,
    OFF_THE_BEATEN_TRACK
}

@Serializable
data class User(
    val userId: Long,
    val name: String,
    val interests: List<Interest>,
    val avatarUrl: String = listOf(
        "https://avatar.iran.liara.run/public/99",
        "https://avatar.iran.liara.run/public/job/police/male",
        "https://avatar.iran.liara.run/public/46",
        "https://avatar.iran.liara.run/public/100",
        "https://avatar.iran.liara.run/public/75",
        "https://avatar.iran.liara.run/public/job/firefighters/female",
        "https://avatar.iran.liara.run/public/job/astronomer/female",
    ).random(),
)

@Serializable
data class UsersResponse(
    val users: List<User>,
)

@Serializable
data class CreateOrUpdateUserRequest(
    val userId: Long,
    val name: String,
    val interests: List<String> = emptyList(),
)

@Serializable
data class Conversation(
    val conversationId: Long,
    val firstUserId: Long,
    val secondUserId: Long,
    val interest: Interest,
    val lastMessageAt: String, // ISO-8601
)

@Serializable
data class Message(
    val messageId: Long,
    val conversationId: Long,
    val senderId: Long,
    val text: String,
    val createdAt: String, // ISO-8601
)

@Serializable
data class MessagesResponse(
    val messages: List<Message>,
    val serverTime: String,
)

@Serializable
data class ConversationsResponse(
    val conversations: List<Conversation>,
)

@Serializable
data class CreateConversationRequest(
    val firstUserId: Long,
    val secondUserId: Long,
    val interest: String,
)

@Serializable
data class CreateMessageRequest(
    val conversationId: Long,
    val senderId: Long,
    val text: String
)
