package com.sjaindl.chatravel.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.sjaindl.chatravel.ui.vm.ChatViewModel.ContentState
import com.sjaindl.chatravel.data.UserDto
import com.sjaindl.chatravel.ui.ErrorScreen
import com.sjaindl.chatravel.ui.LoadingScreen
import com.sjaindl.chatravel.ui.profile.Interest
import com.sjaindl.chatravel.ui.theme.ChaTravelTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHomeScreen(
    contentState: ContentState,
    onConversationClick: (Conversation) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Chats",
    loadUsers: suspend (Interest) -> List<UserDto>,
    startConversation: (userId: Long, interest: Interest) -> Unit,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    var showInterestPicker by remember {
        mutableStateOf(false)
    }

    var showUserResults by remember {
        mutableStateOf(false)
    }

    var interestSelection by remember {
        mutableStateOf<Interest?>(null)
    }

    val snackbar = remember {
        SnackbarHostState()
    }

    val scope = rememberCoroutineScope()

    if (showInterestPicker) {
        InterestPickerBottomSheet(
            interests = Interest.entries,
            initiallySelected = interestSelection,
            onDismiss = {
                interestSelection = null
                showInterestPicker = false
            },
            onConfirm = { chosen ->
                interestSelection = chosen
                showInterestPicker = false
                showUserResults = true
            }
        )
    }

    val selection = interestSelection

    if (showUserResults && selection != null) {
        UsersByInterestBottomSheet(
            interest = selection,
            loadUsers = loadUsers,
            onDismiss = {
                showUserResults = false
            },
            onUserSelected = { user ->

                runCatching {
                    startConversation(user.userId, selection)
                }.onSuccess { convo ->
                    showUserResults = false
                    interestSelection = null

                    scope.launch {
                        snackbar.showSnackbar("Conversation started with ${user.name}")
                    }
                }.onFailure { e ->
                    scope.launch {
                        snackbar.showSnackbar("Failed: ${e.message}")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                actions = {
                    if (trailingContent != null) {
                        Row(content = trailingContent)
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbar)
        },
        floatingActionButton = {
            NewConversationButton(
                onClick = {
                    showInterestPicker = true
                }
            )
        }
    ) { padding ->

        when (contentState) {
            is ContentState.Content -> {
                val conversations = contentState.conversations

                if (conversations.isEmpty()) {
                    Box(
                        modifier = modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No conversations yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(conversations, key = { it.id }) { convo ->
                            ConversationRow(
                                conversation = convo,
                                onClick = {
                                    onConversationClick(convo)
                                }
                            )
                        }
                    }
                }
            }

            is ContentState.Error -> {
                ErrorScreen()
            }
            ContentState.Initial, ContentState.Loading -> {
                LoadingScreen()
            }
        }
    }
}

@Composable
fun NewConversationButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "New conversation"
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewHome() {
    ChaTravelTheme {
        ChatHomeScreen(
            contentState = ContentState.Content(sampleConversations()),
            onConversationClick = { },
            title = "Test chat",
            loadUsers = { _ -> emptyList() },
            startConversation = { _, _ -> },
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewHomeNoConversations() {
    ChaTravelTheme {
        ChatHomeScreen(
            contentState = ContentState.Content(emptyList()),
            onConversationClick = { },
            title = "Test chat",
            loadUsers = { _ -> emptyList() },
            startConversation = { _, _ -> },
        )
    }
}
