package com.sjaindl.chatravel.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class UserApi(
    private val client: HttpClient,
) {

    suspend fun addUser(userId: Long, name: String, interests: List<String>): UserDto {
        val request = CreateUserRequest(
            userId = userId,
            name = name,
            interests = interests
        )

        return client.post("/user") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateUser(userId: Long, name: String, interests: List<String>): Boolean {
        val request = CreateUserRequest(
            userId = userId,
            name = name,
            interests = interests
        )

        return client.put("/user") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getUsersByInterest(interest: String): UsersResponse {
        return client.get("/user") {
            url {
                parameters.append("interest", interest)
            }
        }.body()
    }

    suspend fun getUsers(): UsersResponse {
        return client.get("/user").body()
    }
}
