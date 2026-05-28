package com.lq.video.camera

import android.content.Context
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import com.lq.video.gl.DefaultGlRenderer
import com.lq.video.gl.GLRenderer
import com.lq.video.pipeline.VideoEncoderPipeline
import com.lq.video.view.PreviewTextureView
import java.util.concurrent.Executor
import kotlinx.coroutines.launch

class CameraController(private val context: Context) {

    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    private val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> by lazy {
        ProcessCameraProvider.getInstance(context)
    }

    private var cameraProvider: ProcessCameraProvider? = null

    private var textureView: PreviewTextureView? = null


    val glRenderer: GLRenderer = DefaultGlRenderer()

    fun startPreview(textureView: PreviewTextureView, lifecycleOwner: LifecycleOwner, encodePipeline: VideoEncoderPipeline) = withException {
        this.textureView = textureView

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val previewSurface = Surface(requireNotNull(textureView.surfaceTexture))
            println("startPreview surfaceTexture=${textureView.surfaceTexture.hashCode()}")
            encodePipeline.prepare(1600, 1200, 15_000_000)
            val encoderSurface = requireNotNull(encodePipeline.inputSurface)

            lifecycleOwner.lifecycleScope.launch {
                glRenderer.start(
                    previewSurface = previewSurface,
                    encoderSurface = encoderSurface,
                    previewSize = Size(textureView.width, textureView.height),
                    frameSize = Size(1600, 1200)
                )

                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider { request ->
                        request.provideSurface(glRenderer.cameraInputSurface, mainExecutor) {
                            glRenderer.cameraInputSurface.release()
                        }
                    }
                }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview
                )
            }

        }, mainExecutor)
    }

    fun startRecord(encodePipeline: VideoEncoderPipeline) {
        encodePipeline.prepare(1600, 1200, 15_000_000)
        glRenderer.attachEncoderSurface(requireNotNull(encodePipeline.inputSurface))
    }

    fun stopRecord() {
        glRenderer.detachEncoderSurface()
    }


    private fun release() {
        glRenderer.stop()
        cameraProvider?.unbindAll()
        textureView = null
    }


    private inline fun withException(block: () -> Unit) {
        try {
            block.invoke()
        } catch (e: Exception) {
            throw e
        }
    }



}
