package com.sjaindl.chatravel.data

import android.content.Context
import com.sjaindl.chatravel.ui.profile.Interest
import kotlinx.coroutines.flow.firstOrNull

class UserRepositoryImpl(
    private val context: Context,
    private val userApi: UserApi,
) : UserRepository {

    override suspend fun getCurrentUser(): UserDto? {
        return context.userDataStore.data.firstOrNull()
    }

    override suspend fun addUser(userId: Long, name: String, interests: List<Interest>): UserDto {
        context.userDataStore.updateData {
            it.copy(
                userId = userId,
                name = name,
                interests = interests.map { it.name }
            )
        }

        return userApi.addUser(userId = userId, name = name, interests = interests.map { it.name })
    }

    override suspend fun updateUser(
        userId: Long,
        name: String,
        interests: List<Interest>
    ): UserDto {
        context.userDataStore.updateData {
            it.copy(
                userId = userId,
                name = name,
                interests = interests.map { it.name }
            )
        }
        return userApi.updateUser(userId = userId, name = name, interests = interests.map { it.name })
    }

    override suspend fun getUsers(interest: Interest): UsersResponse {
        return userApi.getUsersByInterest(interest.name)
    }

    override suspend fun getUsers(): UsersResponse {
        return userApi.getUsers()
    }
}

