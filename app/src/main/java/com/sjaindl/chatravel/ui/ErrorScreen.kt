package com.sjaindl.chatravel.ui

import android.content.ClipData
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sjaindl.chatravel.R
import com.sjaindl.chatravel.ui.theme.ChaTravelTheme
import kotlinx.coroutines.launch

@Composable
fun ErrorScreen(
    modifier: Modifier = Modifier,
    onButtonClick: (() -> Unit)? = null,
    title: String = stringResource(R.string.errorTitle),
    text: String = stringResource(R.string.errorDescription),
    buttonTitle: String = stringResource(R.string.retry),
) {
    val scope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxSize(),
    ) {
        Image(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            colorFilter = ColorFilter
                .tint(color = colorScheme.error),
        )

        Text(
            text = title,
            style = typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        val clipboard = LocalClipboard.current

        Text(
            text = text,
            style = typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp).clickable {
                scope.launch {
                    val clipData = ClipData.newPlainText(text, text)
                    clipboard.setClipEntry(clipData.toClipEntry())
                }
            }
        )
        onButtonClick?.let {
            Button(onClick = onButtonClick) {
                Text(
                    text = buttonTitle,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ErrorScreenPreview() {
    ChaTravelTheme {
        ErrorScreen(
            onButtonClick = { },
            modifier = Modifier
                .fillMaxSize(),
        )
    }
}
