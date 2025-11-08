package com.sjaindl.chatravel.data

import com.sjaindl.chatravel.data.room.ChatTravelDatabase
import com.sjaindl.chatravel.data.room.ConversationEntity
import com.sjaindl.chatravel.data.room.MessageEntity
import java.time.Instant

class MessagesRepositoryImpl(
    private val api: MessagesApi,
    private val database: ChatTravelDatabase,
): MessagesRepository {
    override suspend fun getConversations(userId: Long, sinceIsoInstant: String): ConversationsResponse {
        return api.getConversations(userId = userId, sinceIsoInstant = sinceIsoInstant)
    }

    override suspend fun startConversation(request: CreateConversationRequest): Long {
        return api.startConversation(request = request).also { conversationId ->
            database.conversationDao().upsert(
                conversation = ConversationEntity(
                    conversationId = conversationId,
                    firstUserId = request.firstUserId,
                    secondUserId = request.secondUserId,
                    interest = request.interest,
                )
            )
        }
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
        api.createMessage(body).also { messageId ->
            database.messageDao().upsert(
                MessageEntity(
                    messageId = messageId,
                    conversationId = body.conversationId,
                    senderId = body.senderId,
                    text = body.text,
                    createdAt = Instant.now().toString(),
                )
            )
        }
    }
}
