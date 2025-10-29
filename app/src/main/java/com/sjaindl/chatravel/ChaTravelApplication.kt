package com.sjaindl.chatravel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.sjaindl.chatravel.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ChaTravelApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@ChaTravelApplication)
            modules(appModule)
        }

        ensureChannels()
    }

    private fun ensureChannels() {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "matches",
            "Interest matches",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        channel.description = "Notifies when other users shares your interests"
        mgr.createNotificationChannel(channel)
    }
}
