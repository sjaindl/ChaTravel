package com.sjaindl.chatravel.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.sjaindl.chatravel.ui.chat.ChatHomeScreen
import com.sjaindl.chatravel.ui.chat.Conversation
import com.sjaindl.chatravel.ui.chat.detail.ChatDetailScreen
import com.sjaindl.chatravel.ui.chat.sampleConversations
import kotlinx.serialization.Serializable


@Serializable
sealed class NavScreen: NavKey {
    @Serializable
    data object ChatOverview: NavScreen()
    @Serializable
    data class ChatDetail(val conversation: Conversation): NavScreen()
}

@Composable
fun NavContainer() {
    val backStack = rememberNavBackStack<NavScreen>( NavScreen.ChatOverview)

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier
                .padding(innerPadding),
            onBack = { backStack.removeLastOrNull() },
            entryProvider = { key ->
                when (key) {
                    is NavScreen.ChatOverview -> NavEntry(key) {
                        ChatHomeScreen(
                            conversations = sampleConversations(),
                            onConversationClick = {
                                backStack.add(
                                    NavScreen.ChatDetail(it)
                                )
                            },
                        )
                    }

                    is NavScreen.ChatDetail -> NavEntry(key) {
                        key.conversation.lastMessage
                        ChatDetailScreen(
                            title = key.conversation.id,
                            messages = listOf(key.conversation.lastMessage!!),
                            onBack = {
                                backStack.removeLastOrNull()
                            },
                            onSend = {
                                // TODO: pass to VM
                            }
                        )
                    }

                    else -> throw IllegalStateException("Unknown Route")
                }
            }
        )
    }
}
