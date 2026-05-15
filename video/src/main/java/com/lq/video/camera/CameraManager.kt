package com.lq.video.camera

import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

class CameraManager(private val context: Context) {
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    private val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> by lazy {
        ProcessCameraProvider.getInstance(context)
    }
}
