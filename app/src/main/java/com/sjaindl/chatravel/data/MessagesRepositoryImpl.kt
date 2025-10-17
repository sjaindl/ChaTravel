package com.sjaindl.chatravel.data

class MessagesRepositoryImpl(
    private val api: MessagesApi,
): MessagesRepository {
    override suspend fun getConversations(userId: Long): ConversationsResponse {
        return api.getConversations(userId = userId)
    }

    override suspend fun startConversation(request: CreateConversationRequest): Long {
        return api.startConversation(request = request)
    }

    override suspend fun getMessages(conversationId: Long, sinceIsoInstant: String?): MessagesResponse {
        return api.getMessages(conversationId, sinceIsoInstant)
    }

    override suspend fun getMessagesLongPolling(
        conversationId: Long,
        sinceIsoInstant: String?
    ): MessagesResponse {
        return api.getMessagesLongPolling(conversationId, sinceIsoInstant)
    }

    override suspend fun createMessage(body: CreateMessageRequest) {
        api.createMessage(body)
    }
}
