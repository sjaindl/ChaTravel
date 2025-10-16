package com.sjaindl.chatravel.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class ReadMessages(
    val ids: List<Long>,
)

@OptIn(ExperimentalSerializationApi::class)
object ReadMessagesSerializer : Serializer<ReadMessages> {
    override val defaultValue: ReadMessages = ReadMessages(ids = emptyList())

    override suspend fun readFrom(input: InputStream): ReadMessages {
        return try {
            ProtoBuf.decodeFromByteArray(
                ReadMessages.serializer(),
                input.readBytes()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: ReadMessages, output: OutputStream) {
        output.write(
            ProtoBuf.encodeToByteArray(ReadMessages.serializer(), t)
        )
    }
}

val Context.readMessagesDataStore: DataStore<ReadMessages> by dataStore(
    fileName = "read_messages.pb",
    serializer = ReadMessagesSerializer,
)



