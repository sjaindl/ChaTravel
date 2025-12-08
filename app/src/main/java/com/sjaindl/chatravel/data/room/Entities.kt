package com.sjaindl.chatravel.data.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sjaindl.chatravel.data.UserDto
import com.sjaindl.chatravel.ui.chat.Message
import java.time.Instant

@Entity(
    tableName = "conversations",
    indices = [Index("interest")]
)
data class ConversationEntity(
    @PrimaryKey val conversationId: Long,
    val firstUserId: Long,
    val secondUserId: Long,
    val interest: String,
)

@Entity(
    tableName = "messages",
    indices = [Index("conversationId"), Index("senderId"), Index("createdAt"), Index(value=["conversationId","createdAt"])]
)
data class MessageEntity(
    @PrimaryKey val messageId: Long,
    val conversationId: Long,
    val senderId: Long,
    val text: String,
    val createdAt: String, // ISO-8601
) {
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

@Entity(
tableName = "user",
indices = [Index("userId")]
)
data class UserEntity(
    @PrimaryKey
    val userId: Long,
    val name: String,
    val avatarUrl: String? = null,
    val interests: List<String> = emptyList(),
)

@Entity(
    tableName = "outbox_messages",
    indices = [Index("id")]
)
data class OutboxMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val senderId: Long,
    val text: String,
    val createdAtIso: String,
    val attemptCount: Int = 0,
)
