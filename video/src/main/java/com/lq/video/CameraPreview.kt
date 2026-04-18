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
import com.lq.video.camera.CameraController
import com.lq.video.view.MyTextureView

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    controller: CameraController
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val previewView = remember {
        MyTextureView(context)
    }

    LaunchedEffect(lifecycleOwner) {
        controller.startPreview(previewView,lifecycleOwner)
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
