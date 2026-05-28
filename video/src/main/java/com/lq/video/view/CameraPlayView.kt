package com.lq.video.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lq.video.pipeline.VideoPlayPipeline
import com.lq.video.play.PlayController


@Composable
fun CameraPlayView(
    modifier: Modifier,
    controller: PlayController,
    pipeline: VideoPlayPipeline
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = {
            PlayerTextureView(context).also {
                println("播放端 factory 创建 view")
                controller.play(it, lifecycleOwner,pipeline)
            }
        }
    )

}
