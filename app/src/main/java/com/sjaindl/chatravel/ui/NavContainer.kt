package com.sjaindl.chatravel.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.sjaindl.chatravel.AppViewModel
import com.sjaindl.chatravel.ProfileViewModel
import com.sjaindl.chatravel.ui.chat.ChatHomeScreen
import com.sjaindl.chatravel.ui.chat.Conversation
import com.sjaindl.chatravel.ui.chat.detail.ChatDetailScreen
import com.sjaindl.chatravel.ui.chat.sampleConversations
import com.sjaindl.chatravel.ui.profile.ProfileEditor
import kotlinx.serialization.Serializable

@Serializable
sealed class NavScreen: NavKey {

    @Serializable
    data object Profile: NavScreen()

    @Serializable
    data object ChatOverview: NavScreen()

    @Serializable
    data class ChatDetail(val conversation: Conversation): NavScreen()
}

@Composable
fun NavContainer() {
    val appViewModel = viewModel {
        AppViewModel()
    }

    val profileViewModel = viewModel {
        ProfileViewModel()
    }


    val backStack = rememberNavBackStack<NavScreen>(
        NavScreen.Profile
    )

    val contentState = appViewModel.contentState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier
                .padding(innerPadding),
            onBack = {
                backStack.removeLastOrNull()
            },
            entryProvider = { key ->
                when (key) {
                    is NavScreen.Profile -> NavEntry(key) {

                        val userState by profileViewModel.userState.collectAsStateWithLifecycle()

                        ProfileEditor(
                            userState = userState,
                            onContinue = { username, interests ->
                                profileViewModel.onUserSelected(
                                    userName = username,
                                    interests = interests,
                                )
                                backStack.add(NavScreen.ChatOverview)
                            }
                        )
                    }

                    is NavScreen.ChatOverview -> NavEntry(key) {
                        ChatHomeScreen(
                            conversations = sampleConversations(),
                            onConversationClick = {
                                backStack.add(
                                    NavScreen.ChatDetail(it)
                                )
                            },
                            loadUsers = profileViewModel::loadUsers,
                            startConversation = appViewModel::startConversation,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        backStack.add(
                                            NavScreen.Profile
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SupervisedUserCircle,
                                        contentDescription = "Profile",
                                    )
                                }
                            },
                        )
                    }

                    is NavScreen.ChatDetail -> NavEntry(key) {
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
