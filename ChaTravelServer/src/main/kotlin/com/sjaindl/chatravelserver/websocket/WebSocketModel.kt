package com.sjaindl.chatravelserver.websocket

import com.sjaindl.chatravelserver.Message
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class WsEvent

@Serializable
@SerialName("subscribe")
data class WsSubscribe(val conversationId: Long, val type: String = "subscribe") : WsEvent()

@Serializable
@SerialName("sendMessage")
data class WsSendMessage(
    val conversationId: Long,
    val senderId: Long,
    val text: String,
    val type: String = "sendMessage",
) : WsEvent()

@Serializable
@SerialName("newMessage")
data class WsNewMessage(val message: Message, val type: String = "newMessage") : WsEvent()


@Serializable
@SerialName("ack")
data class WsAck(val ok: Boolean, val messageId: Long? = null, val error: String? = null, val type: String = "ack") : WsEvent()
