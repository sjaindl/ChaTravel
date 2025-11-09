package com.sjaindl.chatravel.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.sjaindl.chatravel.R
import com.sjaindl.chatravel.ui.chat.ChatHomeScreen
import com.sjaindl.chatravel.ui.chat.Conversation
import com.sjaindl.chatravel.ui.chat.detail.ChatDetailScreen
import com.sjaindl.chatravel.ui.profile.ProfileEditor
import com.sjaindl.chatravel.ui.vm.ChatSyncViewModel
import com.sjaindl.chatravel.ui.vm.ChatViewModel
import com.sjaindl.chatravel.ui.vm.FcmViewModel
import com.sjaindl.chatravel.ui.vm.InterestMatchViewModel
import com.sjaindl.chatravel.ui.vm.ProfileViewModel
import com.sjaindl.chatravel.ui.vm.ProfileViewModel.UserState
import com.sjaindl.chatravel.ui.vm.TopMatchesViewModel
import io.github.aakira.napier.Napier
import kotlinx.serialization.Serializable

@Serializable
sealed class NavScreen: NavKey {

    @Serializable
    data object Profile: NavScreen()

    @Serializable
    data object ChatOverview: NavScreen()

    @Serializable
    data class ChatDetail(val conversation: Conversation): NavScreen()

    @Serializable
    data class ConnectWithMatch(val matchId: Long): NavScreen()
}

@Composable
fun NavContainer(matchId: Long?) {
    val context = LocalContext.current

    val chatViewModel = viewModel {
        ChatViewModel()
    }

    val profileViewModel = viewModel {
        ProfileViewModel()
    }

    val interestMatchViewModel = viewModel {
        InterestMatchViewModel()
    }

    val chatSyncViewModel = viewModel {
        ChatSyncViewModel()
    }

    val topMatchesViewModel = viewModel {
        TopMatchesViewModel()
    }

    val fcmViewModel = viewModel {
        FcmViewModel()
    }

    val backStack = rememberNavBackStack(
        NavScreen.Profile
    )

    val snackbar = remember {
        SnackbarHostState()
    }

    val contentState by chatViewModel.contentState.collectAsStateWithLifecycle()
    val userState by profileViewModel.userState.collectAsStateWithLifecycle()
    val sseState by interestMatchViewModel.events.collectAsStateWithLifecycle()
    val syncedMessages by chatSyncViewModel.messages.collectAsStateWithLifecycle()
    val topMatches by topMatchesViewModel.contentState.collectAsStateWithLifecycle()
    val notifyUser by profileViewModel.notifyUser.collectAsStateWithLifecycle()
    val lastSync by chatViewModel.lastSync.collectAsStateWithLifecycle()

    LaunchedEffect(sseState) {
        Napier.d("Interest match state: $sseState")
        sseState.lastOrNull()?.let {
            snackbar.showSnackbar("New user with same interests: ${it.name}")

            // refetching top matches
            (userState as? UserState.Content)?.user?.let { user ->
                topMatchesViewModel.start(userId = user.userId)
            }
        }
    }

    LaunchedEffect(matchId) {
        if (matchId != null) {
            backStack.add(NavScreen.ConnectWithMatch(matchId))
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        snackbarHost = {
            SnackbarHost(snackbar)
        },
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

                        ProfileEditor(
                            userState = userState,
                            notify = notifyUser,
                            onNotifyChecked = profileViewModel::setNotify,
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
                        LaunchedEffect(userState) {
                            (userState as? UserState.Content)?.user?.let { user ->
                                chatViewModel.fetchChats(userId = user.userId, lastSync = lastSync, context = context)
                                interestMatchViewModel.start(userId = user.userId)
                                topMatchesViewModel.start(userId = user.userId)
                                fcmViewModel.registerToken(userId = user.userId)
                            }
                        }

                        when (userState) {
                            UserState.Initial, is UserState.Loading -> {
                                LoadingScreen()
                            }

                            is UserState.Content -> {
                                ChatHomeScreen(
                                    contentState = contentState,
                                    topMatches = topMatches,
                                    onConversationClick = {
                                        backStack.add(
                                            NavScreen.ChatDetail(it)
                                        )
                                    },
                                    loadUsers = profileViewModel::loadUsers,
                                    startConversation = { otherUserId, interest ->
                                        chatViewModel.startConversation(
                                            userId = otherUserId,
                                            lastSync = lastSync,
                                            interest = interest,
                                            context = context,
                                        )
                                    },
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

                            is UserState.Error -> {
                                ErrorScreen(text = (userState as UserState.Error).throwable.message ?: stringResource(R.string.errorDescription),)
                            }
                        }
                    }

                    is NavScreen.ChatDetail -> NavEntry(key) {
                        LaunchedEffect(key) {
                            chatViewModel.markAsRead(
                                messageIds = key.conversation.messages.map { it.id },
                                context = context
                            )
                        }

                        val conversation =
                            (contentState as? ChatViewModel.ContentState.Content)?.conversations?.find { it.id == key.conversation.id }
                        ChatDetailScreen(
                            title = key.conversation.title,
                            messages =  (syncedMessages + (conversation?.messages ?: key.conversation.messages)).sortedByDescending {
                                it.sentAt
                            }.distinctBy {
                                it.id
                            },
                            onBack = {
                                backStack.removeLastOrNull()
                            },
                            onSend = {
                                chatViewModel.sendMessage(
                                    conversationId = key.conversation.id,
                                    text = it,
                                )
                            },
                            onSync = {
                                chatSyncViewModel.start(
                                    conversationId = key.conversation.id,
                                    lastSeen = key.conversation.messages.filter {
                                        it.id != 0L
                                    }.minOfOrNull {
                                        it.sentAt
                                    },
                                )
                            }
                        )
                    }

                    is NavScreen.ConnectWithMatch -> NavEntry(key) {
                        ConnectWithMatchScreen(
                            matchId = key.matchId,
                            onDismiss = {
                                backStack.removeLastOrNull()
                                backStack.add(NavScreen.ChatOverview)
                            }
                        )
                    }

                    else -> throw IllegalStateException("Unknown Route")
                }
            }
        )
    }
}
