package com.sjaindl.chatravel.data

interface MessagesRepository {
    suspend fun getConversations(userId: Long): ConversationsResponse
    suspend fun startConversation(request: CreateConversationRequest): Long

    suspend fun getMessages(conversationId: Long, sinceIsoInstant: String? = null): MessagesResponse
    suspend fun getMessagesLongPolling(conversationId: Long, sinceIsoInstant: String? = null): MessagesResponse
    suspend fun createMessage(body: CreateMessageRequest)
}
