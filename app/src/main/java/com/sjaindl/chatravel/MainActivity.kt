package com.sjaindl.chatravel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sjaindl.chatravel.ui.NavContainer
import com.sjaindl.chatravel.ui.theme.ChaTravelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ChaTravelTheme {
                NavContainer()
            }
        }
    }
}
