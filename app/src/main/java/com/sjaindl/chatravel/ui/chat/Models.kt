package com.sjaindl.chatravel.ui.chat

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

@Serializable
data class User(
    val id: String,
    val displayName: String,
)

@Serializable
data class Message(
    val id: Long,
    val conversationId: String,
    val sender: User,
    val text: String?,
    @Serializable(with = InstantSerializer::class)
    val sentAt: Instant,
    val isMine: Boolean,
)

@Serializable
data class Conversation(
    val id: String,
    val title: String?,
    val participants: List<User>,
    val lastMessage: Message?,
    val unreadCount: Int,
    val messages: List<Message>,
)

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString()) // Serialize as ISO-8601 string
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString()) // Deserialize from ISO-8601 string
    }
}
