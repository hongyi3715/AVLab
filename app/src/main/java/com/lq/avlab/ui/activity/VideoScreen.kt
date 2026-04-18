package com.lq.avlab.ui.activity

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lq.video.CameraPreview
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import com.lq.video.camera.CameraController

@Preview
@Composable
fun VideoScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val controller = remember { CameraController(context) }
    var isRecording by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            controller = controller,
            modifier = Modifier.fillMaxWidth().fillMaxHeight()
        )

        Button(
            onClick = {

            },
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter).padding(bottom = 32.dp)
        ) {
            Text(if (isRecording) "停止录制" else "开始录制")
        }
    }
}
