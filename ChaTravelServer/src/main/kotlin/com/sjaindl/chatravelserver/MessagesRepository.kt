package com.sjaindl.chatravelserver

import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.client.model.IndexOptions
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.or
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class MessagesRepository(
    private val database: CoroutineDatabase,
) {

    companion object {
        private const val CONVERSATION_COLLECTION = "Conversation"
        private const val MESSAGES_COLLECTION = "Messages"
    }

    suspend fun startConversation(
        firstUserId: Long,
        secondUserId: Long,
        interestValue: String,
    ): Long {

        val collection = createOrGetCollection<Conversation>(collectionName = CONVERSATION_COLLECTION)

        val id = Random.nextLong(from = 0, until = Long.MAX_VALUE)
        val interest = Interest.valueOf(interestValue)
        val conversation = Conversation(
            conversationId = id,
            firstUserId = firstUserId,
            secondUserId = secondUserId,
            interest = interest
        )
        collection.insertOne(document = conversation)

        return id
    }

    suspend fun addMessage(
        conversationId: Long,
        senderId: Long,
        date: Instant,
        message: String,
    ): Message {
        val collection = createOrGetCollection<Message>(collectionName = MESSAGES_COLLECTION)
        val dateFormatted = DateTimeFormatter.ISO_INSTANT.format(date)
        val id = Random.nextLong(from = 0, until = Long.MAX_VALUE)
        val message = Message(
            messageId = id,
            conversationId = conversationId,
            senderId = senderId,
            text = message,
            createdAt = dateFormatted,
        )

        collection.insertOne(message)

        MessageBus.emitNewMessage(message)

        return message
    }

    suspend fun getConversations(userId: Long): List<Conversation> {
        val collection = createOrGetCollection<Conversation>(CONVERSATION_COLLECTION)
        val result = collection.find(or(
            Conversation::firstUserId eq userId,
            Conversation::secondUserId eq userId)
        ).toList()

        return result
    }

    suspend fun getMessages(conversationId: Long, since: Instant?): List<Message> {
        val collection = createOrGetCollection<Message>(MESSAGES_COLLECTION)
        val result = collection.find(Message::conversationId eq conversationId).toList()

        return since(messages = result, instant = since)
    }

    private fun since(messages: List<Message>, instant: Instant?): List<Message> {
        return messages
            .asSequence()
            .filter { instant == null || Instant.parse(it.createdAt).isAfter(instant) }
            .sortedBy { it.conversationId }
            .toList()
    }

    private suspend inline fun<reified T: Any> createOrGetCollection(
        collectionName: String,
    ): CoroutineCollection<T> {
        val collection: CoroutineCollection<T>
        if (database.listCollectionNames().contains(collectionName).not()) {
            database.createCollection(collectionName = collectionName, CreateCollectionOptions())
            collection = database.getCollection(collectionName = collectionName)

            val createIndex = when (collectionName) {
                CONVERSATION_COLLECTION -> """{ "conversationId" : 1 }"""
                MESSAGES_COLLECTION -> """{ "messageId" : 1 }"""
                else -> null
            }

            createIndex?.let {
                collection.createIndex(key = it, IndexOptions().apply {
                    unique(true)
                })
            }
        } else {
            collection = database.getCollection(collectionName = collectionName)
        }
        return collection
    }
}
