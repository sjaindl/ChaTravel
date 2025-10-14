package com.sjaindl.chatravel.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sjaindl.chatravel.data.UserDto
import com.sjaindl.chatravel.ui.profile.Interest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersByInterestBottomSheet(
    interest: Interest,
    loadUsers: suspend (Interest) -> List<UserDto>,
    onDismiss: () -> Unit,
    onUserSelected: (UserDto) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var users by remember { mutableStateOf<List<UserDto>>(emptyList()) }


    LaunchedEffect(interest) {
        isLoading = true
        error = null

        runCatching { loadUsers(interest) }
            .onSuccess { users = it }
            .onFailure { e -> error = e.message ?: "Failed to load" }
        isLoading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Travellers interested in", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))

            Text(interest.displayName)

            Spacer(Modifier.height(12.dp))

            when {
                isLoading -> {
                    Row(Modifier.fillMaxWidth().height(200.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Text("Error: $error", color = MaterialTheme.colorScheme.error)
                }
                users.isEmpty() -> {
                    Text("No travellers found for \"${interest.displayName}\" yet.")
                }
                else -> {
                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        items(users, key = { it.userId }) { user ->
                            ListItem(
                                headlineContent = { Text(user.name) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onUserSelected(user)
                                    }
                            )

                            HorizontalDivider()
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
