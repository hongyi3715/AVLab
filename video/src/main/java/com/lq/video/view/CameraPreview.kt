package com.lq.video.view

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.lq.video.camera.CameraController

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    controller: CameraController
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = {
            MyTextureView(context).also { view ->
                Log.d("CameraX", "factory 创建 view")
                controller.startPreview(view, lifecycleOwner)
            }
        }
    )

}
