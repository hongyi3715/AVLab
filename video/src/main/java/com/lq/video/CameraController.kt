package com.lq.video

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaCodec
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executor

class CameraController(private val context: Context) {

    private var recording: Recording? = null
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
    val coder = Camera264Encoder()


    @SuppressLint("MissingPermission")
    fun startPreview(
        lifecycleOwner: LifecycleOwner,
        previewView: GLCameraView,
    ) {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            setUpCamera(cameraProviderFuture, previewView, lifecycleOwner)
        }, mainExecutor)
    }

    private fun setUpCamera(
        cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
        previewView: GLCameraView,
        lifecycleOwner: LifecycleOwner
    ) {
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        coder.createEncoder(1600,1200,4000000)
        val recordSurfaceProvider = Preview.SurfaceProvider { request ->
            request.provideSurface(coder.encodeSurface!!,mainExecutor){
                //surface释放
            }

        }
        val preview = Preview.Builder()
            .build().also {

                it.surfaceProvider = recordSurfaceProvider
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

    val cameraRecorder = CameraRecorder(coder)

    /**
     * 开始录制视频
     */
    @SuppressLint("MissingPermission")
    fun startRecording(outputFile: File, onVideoRecordEvent: (VideoRecordEvent) -> Unit) {
        cameraRecorder.startRecording(outputFile)
    }




    /**
     * 停止录制
     */
    fun stopRecording() {
        recording?.stop()
        recording = null
        coder.stop()
        cameraRecorder.stopRecording()
    }
}
