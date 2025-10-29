package com.sjaindl.chatravel.data.fcm

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sjaindl.chatravel.MainActivity
import com.sjaindl.chatravel.data.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ChaTravelMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val userRepository: UserRepository by inject()
    private val tokenRepository: TokenRepository by inject()

    override fun onNewToken(token: String) {
        scope.launch {
            val currentUser = userRepository.getCurrentUser() ?: return@launch
            tokenRepository.registerToken(userId = currentUser.userId, token = token)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        val type = remoteMessage.data["type"]
        if (type != "interest_match") return

        val matchingUserId = remoteMessage.data["matchingUserId"] ?: return
        val name = remoteMessage.data["matchingUserName"] ?: "Unknown"

        // PendingIntent to open ConnectWithMatchScreen
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "OPEN_MATCH"
            putExtra("matchId", matchingUserId)
        }

        val pending = PendingIntent.getActivity(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, "matches")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$name shares your interests")
            .setContentText("Tap to connect")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(matchingUserId.hashCode(), notification)
    }
}
