package com.sjaindl.chatravelserver.fcm

import kotlinx.serialization.Serializable

@Serializable
data class FcmToken(
    val userId: Long,
    val token: String,
    val platform: String = "android",
    val createdAt: String = java.time.Instant.now().toString()
)

@Serializable
data class RegisterFcmRequest(
    val userId: String,
    val token: String,
)
