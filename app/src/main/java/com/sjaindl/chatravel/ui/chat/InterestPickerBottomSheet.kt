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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.sjaindl.chatravel.ui.profile.Interest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestPickerBottomSheet(
    interests: List<Interest>,
    initiallySelected: Interest? = null,
    onDismiss: () -> Unit,
    onConfirm: (Interest?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var query by remember {
        mutableStateOf(TextFieldValue(""))
    }

    var selected by remember {
        mutableStateOf(initiallySelected)
    }

    val filtered = remember(interests, query) {
        val q = query.text.trim().lowercase()
        if (q.isEmpty()) interests.toList() else interests.filter { it.name.lowercase().contains(q) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(text = "Choose interests", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth(),
                placeholder = { Text("Search interests…") }
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered, key = { it }) { interest ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selected = interest }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = interest == selected,
                            onClick = { selected = interest }
                        )
                        Text(interest.displayName, Modifier.padding(start = 8.dp))
                    }
                    HorizontalDivider()
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onDismiss) { Text("Cancel") }
                Button(modifier = Modifier.weight(1f), onClick = { onConfirm(selected) }) {
                    Text("Confirm")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
