package com.sjaindl.chatravel.data.prefs

import android.content.Context
import com.sjaindl.chatravel.prefs.v1.UserSettings
import kotlinx.coroutines.flow.Flow

class UserSettingsRepository(
    private val context: Context,
) {
    val prefs: Flow<UserSettings> = context.userSettingsDataStore.data

    suspend fun setNotify(notify: Boolean) = context.userSettingsDataStore.updateData {
        it.toBuilder().clearUserInterestNotify().setUserInterestNotify(notify).build()
    }

    suspend fun setLastSync(time: String) = context.userSettingsDataStore.updateData {
        it.toBuilder().clearUserInterestNotify().setLastSync(time).build()
    }
}
