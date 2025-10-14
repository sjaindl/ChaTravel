package com.sjaindl.chatravel.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
object UserSerializer : Serializer<UserDto> {
    override val defaultValue: UserDto = UserDto(
        userId = 0L,
        name = "",
        interests = emptyList()
    )

    override suspend fun readFrom(input: InputStream): UserDto {
        return try {
            ProtoBuf.decodeFromByteArray(
                UserDto.serializer(),
                input.readBytes()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: UserDto, output: OutputStream) {
        output.write(
            ProtoBuf.encodeToByteArray(UserDto.serializer(), t)
        )
    }
}

val Context.userDataStore: DataStore<UserDto> by dataStore(
    fileName = "user_prefs.pb",
    serializer = UserSerializer,
)
