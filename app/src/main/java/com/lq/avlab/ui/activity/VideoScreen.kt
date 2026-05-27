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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lq.video.camera.CameraController
import java.io.File

@Composable
fun VideoScreen(host: String,onBack:()->Unit, viewModel: RecordViewModel = hiltViewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val controller = remember { CameraController(context) }
    var isRecording by remember { mutableStateOf(false) }

    LaunchedEffect(host) {
        viewModel.setHost(host)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            controller = controller,
            recordPipeline = viewModel.encodePipeline,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        )
        Button(
            onClick = {
                if (isRecording) {
                    controller.stopRecord()
                    viewModel.stopRecord()
                } else {
                    controller.startRecord(viewModel.encodePipeline)
                    viewModel.startRecord()
                }
                isRecording = !isRecording
            },
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text(if (isRecording) "停止录制" else "开始录制")
        }
    }
}
