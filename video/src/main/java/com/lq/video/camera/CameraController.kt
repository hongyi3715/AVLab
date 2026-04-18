package com.lq.video.camera

import android.content.Context
import android.view.Surface
import android.view.TextureView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.lq.video.CameraFlowState
import com.lq.video.view.MyTextureView
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.Executor

class CameraController(private val context: Context) {

    private val state = MutableStateFlow<CameraFlowState>(CameraFlowState.Idle)

    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    private val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> by lazy {
        ProcessCameraProvider.getInstance(context)
    }

    private var cameraProvider: ProcessCameraProvider? = null


    init {
        initializedCamera()
    }

    private fun initializedCamera() {
        cameraProviderFuture.addListener({
            withException {
                cameraProvider = cameraProviderFuture.get()
                transitionTo(CameraFlowState.Ready)
            }
        }, mainExecutor)
    }



    fun startPreview(textureView: MyTextureView, lifecycleOwner: LifecycleOwner) {
        val currentState = state
        withException {
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(mainExecutor,textureView)

            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview
            )
            transitionTo(CameraFlowState.Preview(textureView,lifecycleOwner))
        }
    }


    fun stopPreview(){

    }


    fun encode() {

    }

    fun release(){

    }

    private inline fun withException(block: () -> Unit) {
        try {
            block.invoke()
        } catch (e: Exception) {
            transitionTo(CameraFlowState.Error(e))
        }
    }

    private fun transitionTo(newState: CameraFlowState) {
        val oldState = state.value
        state.value = newState
        println("Camera State Changed oldState:${oldState} newState:${newState}")
    }

    private fun onStateChange(oldState: CameraFlowState, newState: CameraFlowState) {
        when (newState) {
            is CameraFlowState.Error -> {
                println("OnStateChanged Error:${newState.exception}")
            }

            else -> {

            }
        }
    }

}
