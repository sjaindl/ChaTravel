package com.sjaindl.chatravel.data

import android.content.Context
import com.sjaindl.chatravel.data.room.ChatTravelDatabase
import com.sjaindl.chatravel.data.room.UserEntity
import com.sjaindl.chatravel.ui.profile.Interest
import kotlinx.coroutines.flow.firstOrNull
import java.net.ConnectException

class UserRepositoryImpl(
    private val context: Context,
    private val userApi: UserApi,
    private val database: ChatTravelDatabase,
) : UserRepository {

    override suspend fun getCurrentUser(): UserDto? {
        return context.userDataStore.data.firstOrNull()
    }

    override suspend fun addUser(userId: Long, name: String, interests: List<Interest>): UserDto {
        context.userDataStore.updateData {
            UserDto(
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

    override suspend fun getUsers(interest: Interest): List<UserDto> {
        return userApi.getUsersByInterest(interest.name).users
    }

    override suspend fun getUsers(): List<UserDto> {
        return try {
            usersFromNetwork().users
        } catch (exception: ConnectException) {
            database.userDao().allUsers().map {
                UserDto(
                    userId = it.userId,
                    name = it.name,
                    avatarUrl = it.avatarUrl,
                    interests = it.interests,
                )
            }
        }
    }

    private suspend fun usersFromNetwork(): UsersResponse {
        return userApi.getUsers().also { usersResponse ->
            val allUsers = database.userDao().allUsers()
            val newUsers = usersResponse.users.filterNot { userDto ->
                allUsers.any {
                    it.userId == userDto.userId
                }
            }

            database.userDao().insert(
                newUsers.map {
                    UserEntity(
                        userId = it.userId,
                        name = it.name,
                        avatarUrl = it.avatarUrl,
                        interests = it.interests,
                    )
                }
            )
        }
    }
}
