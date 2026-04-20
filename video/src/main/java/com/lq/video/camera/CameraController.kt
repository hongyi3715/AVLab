package com.lq.video.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.opengl.GLES20
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.lq.video.encode.Camera264Encoder
import com.lq.video.view.MyTextureView
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.Executor
import com.lq.video.camera.CameraFlowState.*
import com.lq.video.gl.EGLCore
import com.lq.video.gl.EGLSurfaceManager
import com.lq.video.gl.OpenGlConfig
import com.lq.video.gl.ShaderConfig
import com.lq.video.camera.CameraEvent.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class CameraController(private val context: Context) {

    private val state = MutableStateFlow<CameraFlowState>(Idle)

    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    private val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> by lazy {
        ProcessCameraProvider.getInstance(context)
    }

    private var cameraProvider: ProcessCameraProvider? = null

    private var textureView: MyTextureView? = null
    private var lifecycleOwner: LifecycleOwner? = null

    private var cameraSurface: Surface? = null

    private val glDispatcher = Executors
        .newSingleThreadExecutor { r ->
            Thread(r, "OpenGLThread").apply {
                priority = Thread.MAX_PRIORITY
            }
        }
        .asCoroutineDispatcher()

    private val audioEncoderDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "AudioEncoderThread").apply {
            priority = Thread.NORM_PRIORITY
        }
    }.asCoroutineDispatcher()

    private val encodeScope = CoroutineScope(audioEncoderDispatcher+SupervisorJob())

    private val coroutineScope = CoroutineScope(SupervisorJob() + glDispatcher)

    fun startPreview(textureView: MyTextureView, lifecycleOwner: LifecycleOwner) = withException {
        this.textureView = textureView
        this.lifecycleOwner = lifecycleOwner
        dispatch(InitializeCamera)
    }

    fun stopPreview() = withException {
        release()
        dispatch(StopPreview)
    }


    private fun release() {
        surfaceTexture?.release()
        cameraSurface?.release()
        previewSurface?.release()
        cameraProvider?.unbindAll()
        textureView = null
        lifecycleOwner = null
    }

    private fun initializedCamera() {
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            dispatch(InitializeEncoder)
        }, mainExecutor)
    }

    private fun bindCamera() = withException {
        println("bindCamera thread = ${Thread.currentThread().name}")
        val preview = Preview.Builder().build()
        cameraSurface?.let { surface->
            preview.setSurfaceProvider { request ->

                request.provideSurface(surface, mainExecutor) { result->
                    println("SurfaceResult: $result")
                    surface.release()
                }
            }
        }
        cameraProvider?.unbindAll()
        lifecycleOwner?.let { //bind成功，不代表出帧,这里是否发送event待考虑
            cameraProvider?.bindToLifecycle(it, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            dispatch(BindSuccess)
        }
    }

    private val encoder = Camera264Encoder()

    private fun initEncoder() = encodeScope.launch {
        encoder.createEncoder(1600, 1200, 2 * 1000 * 1000)
        encoder.startOutputThread(object : Camera264Encoder.AudioBytesMediaCodeCallback {
            override fun onEncodedData(
                data: ByteArray,
                info: MediaCodec.BufferInfo
            ) {
                println("H264 Size:${data.size}")
            }

        })
        dispatch(InitializeOpenGL)
    }

    private val shaderConfig : ShaderConfig by lazy { ShaderConfig() }
    private val eglSurfaceManager : EGLSurfaceManager by lazy { EGLSurfaceManager() }
    private val openGlConfig : OpenGlConfig by lazy { OpenGlConfig() }

    private var previewSurface: Surface? = null
    private var surfaceTexture: SurfaceTexture? = null
    private fun initOpenGL() = coroutineScope.launch {

        textureView?.surfaceTexture?.let {
            previewSurface = Surface(it)
            eglSurfaceManager.initPreview(previewSurface!!)
        }
        encoder.encodeSurface?.let {
            eglSurfaceManager.initEncoder(it)
        }

        eglSurfaceManager.makeCurrentPreview()

        val oesTextureId = openGlConfig.createOESTexture()
        surfaceTexture = SurfaceTexture(oesTextureId).apply {
            setDefaultBufferSize(1600,1200)
            cameraSurface = Surface(this)
        }

        val programId = shaderConfig.initData()
        shaderConfig.initHandler(programId)

        surfaceTexture?.setOnFrameAvailableListener {
            drawFrame(programId,oesTextureId)
        }
        withContext(Dispatchers.Main){
            dispatch(BindCamera)
        }
    }

    private fun drawFrame(programId:Int,oesTextureId:Int) = coroutineScope.launch {
        surfaceTexture?.updateTexImage()
        surfaceTexture?.getTransformMatrix(shaderConfig.texMatrix)

        eglSurfaceManager.makeCurrentPreview()
        openGlConfig.drawFrame(programId,  oesTextureId)
        shaderConfig.drawShader()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        eglSurfaceManager.swapPreview()

        val timestamp = surfaceTexture?.timestamp
        println("timestamp=${surfaceTexture?.timestamp}")
        eglSurfaceManager.makeCurrentEncoder()
        shaderConfig.drawShader()
        openGlConfig.drawFrame(programId,  oesTextureId)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        eglSurfaceManager.presentationTime(timestamp)
        eglSurfaceManager.swapEncoder()
        println("ENCODER swap encoder called")
    }


    private inline fun withException(block: () -> Unit) {
        try {
            block.invoke()
        } catch (e: Exception) {
            dispatch(OnError(e))
        }
    }

    private fun dispatch(event: CameraEvent) {
        val currentState = state.value
        val nextState = reduce(currentState, event)
        if (currentState != nextState) {
            state.value = nextState
            handleEffect(nextState, event)
            println("State Transition :$currentState $nextState $event ")
        }
    }

    /**
     * 需要state 和 event共同确定下一个state
     * */
    fun reduce(state: CameraFlowState, event: CameraEvent): CameraFlowState {
        return when (state) {
            is Idle -> when (event) {
                is InitializeCamera -> CameraInitializing
                else -> state
            }

            is CameraInitializing -> when (event) {
                InitializeEncoder -> EncoderInitializing
                else -> state
            }

            is EncoderInitializing -> when(event){
                InitializeOpenGL -> RenderInitializing
                else -> state
            }

            is RenderInitializing -> when(event){
                BindCamera -> Binding
                else->state
            }

            is Binding -> when (event) {
                is BindSuccess -> PreviewRunning
                else -> state
            }

            is PreviewRunning -> when (event) {
                is StopPreview -> Idle
                else -> state
            }

            else -> state
        }
    }

    fun handleEffect(new: CameraFlowState, event: CameraEvent) {
        when {
            new is CameraInitializing && event is InitializeCamera -> {
                initializedCamera()
            }

            new is EncoderInitializing && event is InitializeEncoder->{
                initEncoder()
            }

            new is RenderInitializing && event is InitializeOpenGL -> {
                initOpenGL()
            }

            new is Binding && event is BindCamera ->{
                bindCamera()
            }
        }
    }


}
