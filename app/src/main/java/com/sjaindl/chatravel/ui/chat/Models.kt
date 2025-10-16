package com.sjaindl.chatravel.ui.chat

import com.sjaindl.chatravel.data.UserDto
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

@Serializable
data class Message(
    val id: Long,
    val conversationId: Long,
    val sender: UserDto,
    val text: String?,
    @Serializable(with = InstantSerializer::class)
    val sentAt: Instant,
    val isMine: Boolean,
)

@Serializable
data class Conversation(
    val id: Long,
    val title: String,
    val participants: List<UserDto>,
    val unreadCount: Int,
    val messages: List<Message>,
) {
    val lastMessage: Message? = messages.lastOrNull()
}

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString()) // Serialize as ISO-8601 string
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString()) // Deserialize from ISO-8601 string
    }
}
