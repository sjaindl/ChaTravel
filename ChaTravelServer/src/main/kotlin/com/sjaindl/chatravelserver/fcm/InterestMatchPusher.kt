package com.sjaindl.chatravelserver.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.sjaindl.chatravelserver.UserRepository
import com.sjaindl.chatravelserver.sse.InterestMatchBus
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class InterestMatchPusher(
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun start() {
        InterestMatchBus.events.onEach { event ->
            // Find other users who share at least 1 interest:
            val matchingUsers = userRepository.getUsersWithSameInterests(event.interests).filter {
                it.userId != event.userId
            }

            if (matchingUsers.isEmpty()) return@onEach

            val collapseKey = "matching_user_${event.userId}"

            matchingUsers.forEach { match ->
                val token = tokenRepository.getTokenForUser(userId = match.userId) ?: return@forEach

                val message = Message.builder()
                    .setToken(token.token)
                    .putData("type", "interest_match")
                    .putData("matchingUserId", event.userId.toString())
                    .putData("matchingUserName", event.name)
                    .putData("interests", event.interests.joinToString(","))
                    .setAndroidConfig(AndroidConfig.builder()
                        .setNotification(AndroidNotification.builder()
                            .setChannelId("matches")
                            .setTitle("${event.name} shares your interests")
                            .setBody("Tap to connect • ${event.interests.joinToString(" • ")}")
                            .build())
                        .setCollapseKey(collapseKey) // deduping
                        .build())
                    .build()

                FirebaseMessaging.getInstance().sendAsync(message)
            }
        }.launchIn(scope)
    }
}
