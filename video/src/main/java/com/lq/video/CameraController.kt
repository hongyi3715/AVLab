package com.lq.video

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.Executor

class CameraController(private val context: Context) {

    private var recording: Recording? = null
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)


    @SuppressLint("MissingPermission")
    fun startPreview(
        lifecycleOwner: LifecycleOwner,
        previewView: GLCameraView,
    ) {
        val cameraProviderFuture : ListenableFuture<ProcessCameraProvider>  = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            setUpCamera(cameraProviderFuture,previewView,lifecycleOwner)
        }, mainExecutor)
    }

    private fun setUpCamera(cameraProviderFuture:ListenableFuture<ProcessCameraProvider> ,previewView: GLCameraView,lifecycleOwner: LifecycleOwner){
        val cameraProvider:ProcessCameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder()
            .build().also {
            it.surfaceProvider = previewView.render
        }


        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 开始录制视频
     */
    @SuppressLint("MissingPermission")
    fun startRecording(outputFile: File, onVideoRecordEvent: (VideoRecordEvent) -> Unit) {

    }



    /**
     * 停止录制
     */
    fun stopRecording() {
        recording?.stop()
        recording = null
    }
}
