package com.sjaindl.chatravel.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversations: List<ConversationEntity>)

    @Query("SELECT * FROM conversations")
    fun allConversations(): Flow<List<ConversationEntity>>
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun messagesForConversation(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages")
    fun getAllMessages(): Flow<List<MessageEntity>>
}


@Dao
interface UserDao {
    @Insert
    suspend fun insert(item: List<UserEntity>)

    @Insert
    suspend fun insert(item: UserEntity): Long

    @Query("DELETE FROM user WHERE userId = :userId")
    suspend fun delete(userId: Long)

    @Query("SELECT * FROM user")
    suspend fun allUsers(): List<UserEntity>
}


@Dao
interface OutboxDao {
    @Insert
    suspend fun insert(item: OutboxMessageEntity): Long

    @Query("DELETE FROM outbox_messages WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE outbox_messages SET attemptCount = attemptCount + 1 WHERE id = :id")
    suspend fun incAttempt(id: Long)

    @Query("SELECT * FROM outbox_messages ORDER BY createdAtIso ASC")
    fun allMessages(): Flow<List<OutboxMessageEntity>>

    @Query("SELECT * FROM outbox_messages ORDER BY createdAtIso ASC")
    suspend fun allMessagesWithOldestFirst(): List<OutboxMessageEntity>
}
