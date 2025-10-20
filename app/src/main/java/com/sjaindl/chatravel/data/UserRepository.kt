package com.sjaindl.chatravel.data

import com.sjaindl.chatravel.ui.profile.Interest

interface UserRepository {
    suspend fun getCurrentUser(): UserDto?
    suspend fun addUser(userId: Long, name: String, interests: List<Interest>): UserDto
    suspend fun updateUser(userId: Long, name: String, interests: List<Interest>): UserDto
    suspend fun getUsers(interest: Interest): UsersResponse
    suspend fun getUsers(): UsersResponse
}
