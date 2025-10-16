package com.sjaindl.chatravel.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

class MessagesApi(private val client: HttpClient) {

    suspend fun getConversations(userId: Long): ConversationsResponse {
        val resp: HttpResponse = client.get("/conversation") {
            url {
                parameters.append("userId", userId.toString())
            }
        }
        return resp.body()
    }


    suspend fun startConversation(request: CreateConversationRequest): Long {
        val resp: HttpResponse = client.post("/conversation") {
            setBody(request)
            contentType(ContentType.Application.Json)
        }
        return resp.body()
    }

    suspend fun getMessages(conversationId: Long, sinceIsoInstant: String? = null): MessagesResponse {
        val resp: HttpResponse = client.get("/message") {
            url {
                parameters.append("conversationId", conversationId.toString())
                sinceIsoInstant?.let {
                    parameters.append("since", it)
                }
            }
        }
        return resp.body()
    }

    suspend fun createMessage(body: CreateMessageRequest): String {
        val resp: HttpResponse = client.post("/message") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        return resp.bodyAsText()
    }
}
