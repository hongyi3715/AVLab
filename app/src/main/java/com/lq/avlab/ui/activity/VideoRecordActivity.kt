package com.lq.avlab.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.lq.annotation.Route
import com.lq.avlab.ui.theme.AVLabTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@Route("/main/video_record")
class VideoRecordActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AVLabTheme {
                Scaffold(modifier = Modifier.Companion.fillMaxSize()) { innerPadding ->
                    VideoRecordNavigation()
                }
            }
        }
    }
}

@Composable
fun VideoRecordNavigation() {
    var currentScreen by remember { mutableStateOf("select") }
    var selectedHost by remember { mutableStateOf("") }

    when (currentScreen) {
        "select" -> {
            SelectIpScreen(
                onIpSelected = { host ->
                    selectedHost = host
                    currentScreen = "play"
                }
            )
        }
        "play" -> {
            VideoScreen(
                host = selectedHost,
                onBack = {
                    currentScreen = "select"
                }
            )
        }
    }
}
