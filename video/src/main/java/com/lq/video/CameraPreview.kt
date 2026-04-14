package com.lq.video

import android.view.TextureView
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    controller: CameraController
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    // 创建并记住 PreviewView
    val previewView = remember {
        TextureView(context)
    }

    // 当 lifecycleOwner 改变时重新绑定
    LaunchedEffect(lifecycleOwner) {
        controller.startPreview(lifecycleOwner, previewView)
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
