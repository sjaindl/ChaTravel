package com.sjaindl.chatravelserver

import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.client.model.IndexOptions
import org.litote.kmongo.contains
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.`in`

class UserRepository(private val database: CoroutineDatabase) {

    companion object {
        private const val USER_COLLECTION = "User"
    }

    suspend fun userExists(userId: Long): Boolean {
        val collection = createOrGetCollection<User>(USER_COLLECTION)
        val allUsers = collection.find().toList()
        return allUsers.any {
            it.userId == userId
        }
    }

    suspend fun registerUser(
        userId: Long,
        name: String,
        interestValues: List<String>,
    ): User {
        val collection = createOrGetCollection<User>(collectionName = USER_COLLECTION)

        val interests = interestValues.mapNotNull { value ->
            Interest.entries.find {
                it.name == value
            }
        }

        val user = User(userId = userId, name = name, interests = interests)
        collection.insertOne(document = user)
        return user
    }

    suspend fun updateUserInterests(
        userId: Long,
        userName: String,
        interestValues: List<String>,
    ): User {
        val collection = createOrGetCollection<User>(USER_COLLECTION)
        val interests = interestValues.mapNotNull { value ->
            Interest.entries.find {
                it.name == value
            }
        }

        val user = collection.findOne(User::userId.eq(userId))?.copy(interests = interests) ?: registerUser(
            userId = userId,
            name = userName,
            interestValues = interestValues,
        )

        collection.updateOne(User::userId.eq(userId), user)

        return user
    }

    suspend fun getUsersWithSameInterests(interestValue: String): List<User> {
        val collection = createOrGetCollection<User>(USER_COLLECTION)
        val interest = Interest.entries.find {
            it.name == interestValue
        }

        val result = collection.find(User::interests contains interest).toList()
        return result
    }

    suspend fun getUsersWithSameInterests(interests: List<Interest>): List<User> {
        val collection = createOrGetCollection<User>(USER_COLLECTION)

        val result = collection.find(User::interests `in` interests).toList()
        return result
    }

    suspend fun getUserInterests(userId: Long): List<Interest> {
        val user = getUser(userId = userId)

        return user?.interests ?: emptyList()
    }

    suspend fun getUsers(): List<User> {
        val collection = createOrGetCollection<User>(USER_COLLECTION)

        val result = collection.find().toList()
        return result
    }

    suspend fun getUser(userId: Long): User? {
        val collection = createOrGetCollection<User>(USER_COLLECTION)
        val user = collection.findOne(User::userId.eq(userId))

        return user
    }

    private suspend inline fun<reified T: Any> createOrGetCollection(
        collectionName: String,
    ): CoroutineCollection<T> {
        val collection: CoroutineCollection<T>
        if (database.listCollectionNames().contains(collectionName).not()) {
            database.createCollection(collectionName = collectionName, CreateCollectionOptions())
            collection = database.getCollection(collectionName = collectionName)

            val createIndex = when (collectionName) {
                USER_COLLECTION -> """{ "userId" : 1 }"""
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
