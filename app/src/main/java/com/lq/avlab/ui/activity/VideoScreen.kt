package com.lq.avlab.ui.activity

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lq.video.view.CameraPreview
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import com.lq.video.camera.CameraController
import java.io.File

@Composable
fun VideoScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val controller = remember { CameraController(context) }
    var isRecording by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            controller = controller,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        )
        Button(
            onClick = {
                if (isRecording) {
                    controller.stopRecord()
                } else {
                    controller.startRecord()
                }
                isRecording = !isRecording
            },
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text(if (isRecording) "开始录制" else "停止录制")
        }
    }
}
