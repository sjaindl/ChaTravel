package com.sjaindl.chatravel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sjaindl.chatravel.ui.theme.ChaTravelTheme

@Composable
fun LoadingScreen(
    loadingInfo: String? = null,
    paddingValues: PaddingValues = PaddingValues(horizontal = 16.dp),
    modifier: Modifier = Modifier
        .padding(paddingValues = paddingValues)
        .fillMaxSize()
        .wrapContentSize(),
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()

        loadingInfo?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview
@Composable
private fun LoadingScreenPreview() {
    ChaTravelTheme {
        LoadingScreen()
    }
}
