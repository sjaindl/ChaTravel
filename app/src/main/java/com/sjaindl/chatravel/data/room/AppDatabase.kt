package com.sjaindl.chatravel.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(StringListConverter::class)
abstract class ChatTravelDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

    companion object {
        const val DATABASE_NAME = "chatravel.db"
    }
}
