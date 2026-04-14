package com.lq.video

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.TextureView
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

    val glThread = HandlerThread("GLThread").also { it.start() }
    val glHandler = Handler(glThread.looper)

    private val encoder = Camera264Encoder()

    @SuppressLint("MissingPermission")
    fun startPreview(
        lifecycleOwner: LifecycleOwner,
        previewView: TextureView,
    ) {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            setUpCamera(previewView, cameraProviderFuture, lifecycleOwner)
        }, mainExecutor)
    }
    private var previewSurface: Surface? = null
    private fun setUpCamera(
        textureView: TextureView,
        cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
        lifecycleOwner: LifecycleOwner
    ) {
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
        val shaderConfig = ShaderConfig()
        val openGlConfig = OpenGlConfig()

        val preview = Preview.Builder()
            .build()

        var surfaceTexture: SurfaceTexture? = null

        encoder.createEncoder(1600, 1200, 2 * 1000 * 1000)
        encoder.startOutputThread(object : Camera264Encoder.AudioBytesMediaCodeCallback{
            override fun onEncodedData(
                data: ByteArray,
                info: MediaCodec.BufferInfo
            ) {
                println("H264 Size:${data.size}")
            }

        })
        preview.setSurfaceProvider  { request ->
            if(previewSurface==null) previewSurface = Surface(surfaceTexture)
            request.provideSurface(previewSurface!!, mainExecutor) { result ->
                previewSurface?.release()
//                        surfaceTexture?.release()
            }
        }
        glHandler.post {
            val eglHelper = EglHelper()
            eglHelper.initEgl()
            eglHelper.initSurfaces(textureView.surfaceTexture!!, encoder.encodeSurface!!)

            val oesTextureId = openGlConfig.createOESTexture()
            surfaceTexture = SurfaceTexture(oesTextureId)
            surfaceTexture.setDefaultBufferSize(1600, 1200)

            eglHelper.makeCurrent2Screen()
            val programId = shaderConfig.initData()
            shaderConfig.initHandler(programId)
            surfaceTexture.setOnFrameAvailableListener {

                surfaceTexture.updateTexImage()

                surfaceTexture.getTransformMatrix(shaderConfig.texMatrix)

                eglHelper.makeCurrent2Screen()
                openGlConfig.drawFrame(programId, shaderConfig, oesTextureId)
                eglHelper.swapBuffers2Screen()

              /*  eglHelper.makeCurrent2Encoder()
                openGlConfig.drawFrame(programId, shaderConfig, oesTextureId)
                eglHelper.eglPresentationTime(surfaceTexture.timestamp)
                eglHelper.swapBuffers2Encoder()*/
            }
        }
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val cameraRecorder = CameraRecorder(encoder)

    /**
     * 开始录制视频
     */
    @SuppressLint("MissingPermission")
    fun startRecording(outputFile: File, onVideoRecordEvent: (VideoRecordEvent) -> Unit) {
//        cameraRecorder.startRecording(outputFile)
    }


    /**
     * 停止录制
     */
    fun stopRecording() {
        recording?.stop()
        recording = null
//        coder.stop()
//        cameraRecorder.stopRecording()
    }
}
