package com.sjaindl.chatravel.data.prefs

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.sjaindl.chatravel.prefs.v1.UserSettings
import java.io.InputStream
import java.io.OutputStream

val Context.userSettingsDataStore by dataStore(
    fileName = "user_settings.pb",
    serializer = UserSettingsSerializer
)

object UserSettingsSerializer : Serializer<UserSettings> {
    override val defaultValue: UserSettings = UserSettings.getDefaultInstance()
    override suspend fun readFrom(input: InputStream): UserSettings {
        try {
            return UserSettings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Could not read proto file", exception)
        }
    }

    override suspend fun writeTo(t: UserSettings, output: OutputStream) = t.writeTo(output)
}
