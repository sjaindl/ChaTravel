package com.sjaindl.chatravelserver.fcm

import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.client.model.IndexOptions
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq

class TokenRepository(
    private val database: CoroutineDatabase,
) {

    companion object {
        private const val TOKEN_COLLECTION = "Token"
    }

    suspend fun upsert(userId: Long, token: String) {
        val collection = createOrGetCollection<FcmToken>(collectionName = TOKEN_COLLECTION)

        val token = FcmToken(
            userId = userId,
            token = token,
        )

        collection.insertOne(token)
    }

    suspend fun getTokenForUser(userId: Long): FcmToken? {
        val collection = createOrGetCollection<FcmToken>(collectionName = TOKEN_COLLECTION)
        val token = collection.findOne(FcmToken::userId.eq(userId))

        return token
    }

    private suspend inline fun<reified T: Any> createOrGetCollection(
        collectionName: String,
    ): CoroutineCollection<T> {
        val collection: CoroutineCollection<T>
        if (database.listCollectionNames().contains(collectionName).not()) {
            database.createCollection(collectionName = collectionName, CreateCollectionOptions())
            collection = database.getCollection(collectionName = collectionName)

            val createIndex = when (collectionName) {
                TOKEN_COLLECTION -> """{ "userId" : 1, "token" : 1 }"""
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
