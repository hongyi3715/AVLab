package com.lq.video.gl

import android.util.Size
import android.view.Surface

interface GLRenderer {


    val cameraInputSurface: Surface

    suspend fun start(
        previewSurface: Surface,
        encoderSurface: Surface,
        previewSize: Size,
        frameSize: Size
    )

    fun attachEncoderSurface(surface: Surface)

    fun detachEncoderSurface()

    fun onPreviewSizeChanged(width:Int,height:Int)

    fun stop()
}
