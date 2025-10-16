package com.sjaindl.chatravel.ui.chat

import com.sjaindl.chatravel.data.UserDto
import java.time.Instant

private val Me = UserDto(userId = 0, name = "You")
private val Luke = UserDto(userId = 1, name = "Luke Skywalker")
private val Han = UserDto(userId = 2, name = "Han Solo")

fun sampleMessages(): List<Message> = listOf(
    Message(
        id = 5,
        conversationId = 1,
        sender = Luke,
        text = "See you at 6?",
        sentAt = Instant.now().minusSeconds(120),
        isMine = false,
    ),
    Message(
        id = 6,
        conversationId = 2,
        sender = Me,
        text = "Perfect. I'll bring snacks.",
        sentAt = Instant.now().minusSeconds(60),
        isMine = true,
    ),
)

fun sampleConversations(): List<Conversation> {
    return listOf(
        Conversation(
            id = 1,
            title = "Project Alpha Centauri",
            participants = listOf(Me, Luke, Han),
            unreadCount = 2,
            messages = emptyList()
        ),
        Conversation(
            id = 2,
            title = "Sports talk",
            participants = listOf(Luke),
            unreadCount = 0,
            messages = emptyList(),
        )
    )
}
