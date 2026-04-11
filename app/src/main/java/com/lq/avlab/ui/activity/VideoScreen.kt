package com.lq.avlab.ui.activity

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lq.video.CameraController
import com.lq.video.CameraPreview
import java.io.File

@Preview
@androidx.compose.runtime.Composable
fun VideoScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    // 1. 初始化控制器
    val controller = androidx.compose.runtime.remember { CameraController(context) }
    var isRecording by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
        // 2. 显示预览
        CameraPreview(
            controller = controller,
            modifier = Modifier.fillMaxWidth().height(600.dp)
        )

        // 3. 录制按钮
        androidx.compose.material3.Button(
            onClick = {
                if (isRecording) {
                    controller.stopRecording()
                    isRecording = false
                } else {
                    val file = File(context.externalCacheDir, "${System.currentTimeMillis()}.mp4")
                    controller.startRecording(file) { event ->
                        // 处理录制事件（如结束、错误等）
                    }
                    isRecording = true
                }
            },
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter).padding(bottom = 32.dp)
        ) {
            androidx.compose.material3.Text(if (isRecording) "停止录制" else "开始录制")
        }
    }
}
