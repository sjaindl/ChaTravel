package com.sjaindl.chatravel.ui.chat

import java.time.Instant

private val Me = User("me", "You")
private val Luke = User("1", "Luke Skywalker")
private val Han = User("2", "Han Solo")

fun sampleMessages(): List<Message> = listOf(
    Message(
        id = 5,
        conversationId = "c1",
        sender = Luke,
        text = "See you at 6?",
        sentAt = Instant.now().minusSeconds(120),
        isMine = false,
    ),
    Message(
        id = 6,
        conversationId = "c1",
        sender = Me,
        text = "Perfect. I'll bring snacks.",
        sentAt = Instant.now().minusSeconds(60),
        isMine = true,
    ),
)

fun sampleConversations(): List<Conversation> {
    val last = sampleMessages().last()
    return listOf(
        Conversation(
            id = "c1",
            title = "Project Alpha Centauri",
            participants = listOf(Me, Luke, Han),
            lastMessage = last,
            unreadCount = 2,
            messages = emptyList()
        ),
        Conversation(
            id = "c2",
            title = null, // fallback to participants
            participants = listOf(Luke),
            lastMessage = last.copy(text = "Let's ship it!"),
            unreadCount = 0,
            messages = emptyList(),
        )
    )
}
