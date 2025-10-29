package com.sjaindl.chatravel.data.fcm

import com.sjaindl.chatravel.data.FcmTokenApi

interface TokenRepository {
    suspend fun registerToken(userId: Long, token: String)
}

class TokenRepositoryImpl(
    private val fcmTokenApi: FcmTokenApi,
): TokenRepository {

    override suspend fun registerToken(userId: Long, token: String) {
        runCatching {
            fcmTokenApi.registerToken(
                body = FcmTokenApi.RegisterTokenRequest(
                    userId = userId,
                    token = token,
                )
            )

        }.onSuccess {
            println("Token registered")
        }.onFailure {
            it.printStackTrace()
        }
    }
}
