package com.sjaindl.chatravel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sjaindl.chatravel.ui.NavContainer
import com.sjaindl.chatravel.ui.theme.ChaTravelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ChaTravelTheme {

                var matchId by remember {
                    mutableStateOf<Long?>(null)
                }

                if (intent != null && intent.action == "OPEN_MATCH") {
                    matchId = intent.getStringExtra("matchId")?.toLong()
                }

                NavContainer(
                    matchId = matchId,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
