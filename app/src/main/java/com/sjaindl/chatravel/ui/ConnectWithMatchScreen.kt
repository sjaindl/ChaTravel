package com.sjaindl.chatravel.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.sjaindl.chatravel.ui.vm.MatchUiState
import com.sjaindl.chatravel.ui.vm.MatchInviteViewModel
import com.sjaindl.chatravel.ui.vm.MatchUser

@Composable
fun ConnectWithMatchScreen(
    matchId: Long,
    onDismiss: () -> Unit
) {
    val viewModel = viewModel {
        MatchInviteViewModel()
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(matchId) {
        viewModel.refresh(matchId)
    }

    ConnectWithMatchScreenContent(
        state = state,
        onDismiss = onDismiss,
        onRefresh = viewModel::refresh,
        onStartChat = {
            viewModel.startChat(
                interest = it,
                onOpened = {
                    onDismiss()
                },
                onError = { /* show snackbar/toast in your host */ }
            )
        }
    )
}

@Composable
fun ConnectWithMatchScreenContent(
    state: MatchUiState,
    onStartChat: (String) -> Unit,
    onDismiss: () -> Unit,
    onRefresh: (Long) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (state) {
            is MatchUiState.Loading -> LoadingState()
            is MatchUiState.Error -> ErrorState(
                message = state.message,
                onRetry = onRefresh,
                onDismiss = onDismiss,
                matchingUserId = state.matchingUserId,
            )
            is MatchUiState.Content -> ReadyState(
                user = state.user,
                isStarting = state.isStarting,
                onStartChat = onStartChat,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun ReadyState(
    user: MatchUser,
    isStarting: Boolean,
    onStartChat: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedInterest by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Avatar(url = user.avatarUrl)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "shares your interests",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            if (user.commonInterests.isNotEmpty()) {
                Text(
                    "Common interests",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                InterestChips(
                    items = user.commonInterests,
                    selectedItem = selectedInterest,
                    onItemClick = { selectedInterest = it }
                )
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(16.dp))

            if (user.interests.isNotEmpty()) {
                Text(
                    "Also interested in",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                InterestChips(items = user.interests - user.commonInterests.toSet())
            }
        }

        Column {
            Button(
                onClick = {
                    selectedInterest?.let {
                        onStartChat(it)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isStarting && selectedInterest != null
            ) {
                if (isStarting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 8.dp)
                    )
                }
                Text(if (isStarting) "Starting…" else "Start Chat")
            }
            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Dismiss") }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    message: String,
    matchingUserId: Long,
    onRetry: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Couldn’t load match", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Row {
            OutlinedButton(onClick = onDismiss) { Text("Close") }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = {
                    onRetry(matchingUserId)
                }
            ) {
                Text("Retry")
            }
        }
    }
}

// ---------- small UI bits ----------

@Composable
private fun Avatar(url: String?, size: Int = 64) {
    val modifier = Modifier
        .size(size.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant)

    if (url.isNullOrBlank()) {
        // fallback vector or placeholder
        Image(
            painter = painterResource(id = android.R.drawable.sym_def_app_icon),
            contentDescription = "avatar",
            modifier = modifier
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = "avatar",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
private fun InterestChips(
    items: List<String>,
    selectedItem: String? = null,
    onItemClick: ((String) -> Unit)? = null,
) {
    if (items.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items.size) { idx ->
            val item = items[idx]
            val isSelected = item == selectedItem
            AssistChip(
                onClick = {
                    if (onItemClick != null) {
                        onItemClick(item)
                    }
                },
                label = { Text(items[idx]) },
                shape = RoundedCornerShape(24.dp),
                colors = if (isSelected && onItemClick != null) {
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        labelColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    AssistChipDefaults.assistChipColors()
                },
            )
        }
    }
}

@Preview
@Composable
fun ConnectWithMatchScreenPreview() {
    ConnectWithMatchScreenContent(
        state = MatchUiState.Content(user = MatchUser(
            1, "name",
            avatarUrl = "",
            interests = emptyList(),
            commonInterests = emptyList(),
        ), isStarting = false),
        onDismiss = { },
        onRefresh = { },
        onStartChat = { }
    )
}
