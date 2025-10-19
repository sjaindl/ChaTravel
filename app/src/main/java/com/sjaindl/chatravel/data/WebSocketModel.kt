package com.sjaindl.chatravel.data

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

@Serializable @SerialName("unsubscribe")
data class WsUnsubscribe(val conversationId: Long): WsEvent()

@Serializable
@SerialName("sendMessage")
data class WsSendMessage(
    val conversationId: Long,
    val senderId: Long,
    val text: String,
    val type: String = "sendMessage"
) : WsEvent()

@Serializable
@SerialName("newMessage")
data class WsNewMessage(val message: MessageDto) : WsEvent()


@Serializable
@SerialName("ack")
data class WsAck(val ok: Boolean, val messageId: Long? = null, val error: String? = null) : WsEvent()
