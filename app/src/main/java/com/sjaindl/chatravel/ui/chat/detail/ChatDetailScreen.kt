package com.sjaindl.chatravel.ui.chat.detail

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.sjaindl.chatravel.ui.chat.Message
import com.sjaindl.chatravel.ui.chat.sampleMessages
import com.sjaindl.chatravel.ui.chat.timeFormatter
import com.sjaindl.chatravel.ui.theme.ChaTravelTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    title: String,
    messages: List<Message>,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember {
        mutableStateOf("")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            MessageInputBar(
                value = draft,
                onValueChange = { draft = it },
                onSend = {
                    val trimmed = draft.trim()
                    if (trimmed.isNotEmpty()) {
                        onSend(trimmed)
                        draft = ""
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            reverseLayout = true // Newest at bottom, start from end
        ) {
            items(messages) { msg ->
                MessageBubble(
                    text = msg.text.orEmpty(),
                    time = timeFormatter.format(msg.sentAt),
                    isMine = msg.isMine,
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ChatDetailPreview() {
    ChaTravelTheme {
        ChatDetailScreen(
            title = "Project Alpha Centauri",
            messages = sampleMessages().asReversed(),
            onBack = {},
            onSend = {}
        )
    }
}
