package com.sjaindl.chatravel.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.sjaindl.chatravel.ui.vm.ProfileViewModel.UserState
import com.sjaindl.chatravel.data.UserDto
import com.sjaindl.chatravel.ui.ErrorScreen
import com.sjaindl.chatravel.ui.LoadingScreen

enum class Interest(val displayName: String) {
    SPORTS("Sports"),
    TREKKING("Trekking"),
    SIGHTSEEING("Sightseeing"),
    FOOD("Food"),
    CULTURE("Culture"),
    OFF_THE_BEATEN_TRACK("Off the Beaten Track")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditor(
    userState: UserState,
    onContinue: (String, List<Interest>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var username by remember {
        mutableStateOf((userState as? UserState.Content)?.user?.name ?: "")
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    var selectedInterests by remember {
        mutableStateOf<List<Interest>>(emptyList())
    }

    val userNameLocked by remember(userState) {
        derivedStateOf {
            val name = (userState as? UserState.Content)?.user?.name ?: ""
            name.isNotBlank()
        }
    }

    LaunchedEffect(userState) {
        (userState as? UserState.Content)?.let { content ->
            username = content.user?.name ?: ""
            selectedInterests = content.user?.interests?.map {
                Interest.valueOf(it)
            } ?: emptyList()
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {

        when (userState) {
            UserState.Initial, UserState.Loading -> {
                LoadingScreen()
            }

            is UserState.Error -> {
                ErrorScreen(
                    modifier = modifier,
                    text = userState.throwable.message ?: "Error updating profile",
                    onButtonClick = {
                        onContinue(username, selectedInterests)
                    },
                )
            }
            is UserState.Content -> {
                TextField(
                    value = username,
                    onValueChange = {
                        username = it
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    enabled = !userNameLocked,
                    label = {
                        Text("Username")
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = selectedInterests.joinToString(", ") {
                            it.displayName
                        },
                        onValueChange = { },
                        readOnly = true,
                        label = {
                            Text("Select Interests")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        Interest.entries.forEach { interest ->
                            val isSelected = selectedInterests.contains(interest)
                            DropdownMenuItem(
                                text = {
                                    Text(interest.displayName)
                                },
                                onClick = {
                                    selectedInterests = if (isSelected) {
                                        selectedInterests - interest
                                    } else {
                                        selectedInterests + interest
                                    }
                                },
                                trailingIcon = {
                                    Checkbox(checked = isSelected, onCheckedChange = null)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onContinue(username, selectedInterests) },
                    enabled = username.isNotBlank() && selectedInterests.isNotEmpty(),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun ProfileEditorPreview() {
    ProfileEditor(
        userState = UserState.Content(UserDto(1, "Luke Skywalker", listOf(Interest.OFF_THE_BEATEN_TRACK.name))),
        onContinue = { _, _ -> }
    )
}

@PreviewLightDark
@Composable
fun ProfileEditorErrorPreview() {
    ProfileEditor(
        userState = UserState.Error(IllegalStateException("Test error")),
        onContinue = { _, _ -> }
    )
}


@PreviewLightDark
@Composable
fun ProfileEditorLoadingPreview() {
    ProfileEditor(
        userState = UserState.Loading,
        onContinue = { _, _ -> }
    )
}
