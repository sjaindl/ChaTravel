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
import com.sjaindl.chatravel.data.MessageFetcher
import com.sjaindl.chatravel.data.sse.InterestMatchSseClient
import com.sjaindl.chatravel.ui.NavContainer
import com.sjaindl.chatravel.ui.theme.ChaTravelTheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {

    private val messageFetcher: MessageFetcher by inject()
    private val interestMatchSseClient: InterestMatchSseClient by inject()

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

    /**
     * Release memory when the UI becomes hidden or when system resources become low.
     * @param level the memory-related event that is raised.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            // Release memory related to UI elements, such as bitmap caches.
        }

        if (level >= TRIM_MEMORY_BACKGROUND) {
            // Release memory related to background processing
            messageFetcher.stop()
            interestMatchSseClient.stop()
        }
    }
}
