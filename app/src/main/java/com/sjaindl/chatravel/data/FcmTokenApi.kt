package com.sjaindl.chatravel.data

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class FcmTokenApi(private val client: HttpClient) {

    @Serializable
    data class RegisterTokenRequest(
        val userId: Long,
        val token: String,
    )

    suspend fun registerToken(body: RegisterTokenRequest): String {
        val resp: HttpResponse = client.post("/push/register") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        return resp.bodyAsText()
    }
}
