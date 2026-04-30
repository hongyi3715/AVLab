package com.lq.video.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.lq.video.record.CameraRecorder
import com.lq.video.encode.Camera264Encoder
import com.lq.video.view.MyTextureView
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.Executor
import com.lq.video.camera.CameraFlowState.*
import com.lq.video.gl.EGLSurfaceManager
import com.lq.video.gl.OpenGlConfig
import com.lq.video.gl.ShaderConfig
import com.lq.video.camera.CameraEvent.*
import com.lq.video.gl.FboManager
import com.lq.video.gl.ProgramHandles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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

    private val coroutineScope = CoroutineScope(SupervisorJob() + glDispatcher)

    private val audioEncoderDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "AudioEncoderThread").apply {
            priority = Thread.NORM_PRIORITY
        }
    }.asCoroutineDispatcher()

    private val encodeScope = CoroutineScope(audioEncoderDispatcher + SupervisorJob())

    private var frameChannel: Channel<Unit>? = null

    private var renderJob: Job? = null


    private fun startRenderLoop() {
        if (renderJob != null) return
        if (frameChannel == null) return
        renderJob = coroutineScope.launch {
            try {
                for (signal in frameChannel) {
                    drawFrame()
                }
            } catch (e: Exception) {
                dispatch(OnError(e))
            }
        }
    }

    private fun stopRenderLoop() {
        renderJob?.cancel()
        renderJob = null
    }

    fun startPreview(textureView: MyTextureView, lifecycleOwner: LifecycleOwner) = withException {
        this.textureView = textureView
        this.lifecycleOwner = lifecycleOwner
        dispatch(InitializeCamera)
    }

    fun stopPreview() {
        if (state.value is PreviewRunning) {
            dispatch(StopPreview)
        }

        stopRenderLoop()
        release()
        dispatch(StopPreview)
    }

    fun startRecord() {
        val file = File(context.externalCacheDir, "${System.currentTimeMillis()}.h264")
        cameraRecoder.startRecording(file)
    }

    fun stopRecord() {
        cameraRecoder.stopRecording()
    }


    private fun release() {
        surfaceTexture?.setOnFrameAvailableListener(null)
        surfaceTexture?.release()
        cameraSurface?.release()
        previewSurface?.release()
        cameraProvider?.unbindAll()
        textureView = null
        lifecycleOwner = null
        fboManager?.release()
        eglSurfaceManager.release()
        frameChannel?.close()
        frameChannel = null
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
        cameraSurface?.let { surface ->
            preview.setSurfaceProvider { request ->

                request.provideSurface(surface, mainExecutor) { result ->
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

    private val cameraRecoder = CameraRecorder(encoder)
    private fun initEncoder() = encodeScope.launch {
        cameraRecoder.coder.createEncoder(1600, 1200, 15_000_000)

        dispatch(InitializeOpenGL)
    }

    private val shaderConfig: ShaderConfig by lazy { ShaderConfig() }
    private val eglSurfaceManager: EGLSurfaceManager by lazy { EGLSurfaceManager() }
    private val openGlConfig: OpenGlConfig by lazy { OpenGlConfig() }

    private var previewSurface: Surface? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var fboManager: FboManager? = null

    private var oesProgram: Int = 0
    private var textureProgram: Int = 0
    private var oesTextureId: Int = 0
    private var oesHandler: ProgramHandles? = null
    private var textureHandler: ProgramHandles? = null

    private fun initOpenGL() = coroutineScope.launch {

        textureView?.surfaceTexture?.let {
            previewSurface = Surface(it)
            eglSurfaceManager.initPreview(previewSurface!!)
        }
        encoder.encodeSurface?.let {
            eglSurfaceManager.initEncoder(it)
        }

        eglSurfaceManager.makeCurrentPreview()

        oesTextureId = openGlConfig.createOESTexture()
        surfaceTexture = SurfaceTexture(oesTextureId).apply {
            setDefaultBufferSize(1600, 1200)
            fboManager = FboManager(1600, 1200)
            cameraSurface = Surface(this)
        }

        oesProgram = shaderConfig.createOesProgram()
        textureProgram = shaderConfig.createTexture2DProgram()
        oesHandler = shaderConfig.initHandler(oesProgram)
        textureHandler = shaderConfig.initHandler(textureProgram)
        surfaceTexture?.setOnFrameAvailableListener {
            frameChannel?.trySend(Unit)
        }
        frameChannel = Channel(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        startRenderLoop()
        withContext(Dispatchers.Main) {
            dispatch(BindCamera)
        }
    }


    val fboWidth = 1600
    val fboHeight = 1200
    private fun drawFrame() {
        if (state.value !is PreviewRunning) return
        if (oesHandler == null) return
        if (textureHandler == null) return
        val st = surfaceTexture ?: return

        val previewWidth = textureView?.width ?: fboWidth
        val previewHeight = textureView?.height ?: fboHeight

        shaderConfig.updatePreviewVertexBuffer(
            srcWidth = fboHeight,
            srcHeight = fboWidth,
            dstWidth = previewWidth,
            dstHeight = previewHeight
        )

        st.updateTexImage()
        st.getTransformMatrix(shaderConfig.texMatrix)
        val timestamp = st.timestamp

        eglSurfaceManager.makeCurrentPreview()
        fboManager?.bind()
        GLES20.glViewport(0, 0, fboWidth, fboHeight)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        openGlConfig.drawFrame(oesProgram, oesTextureId)
        shaderConfig.drawShader(oesHandler!!, shaderConfig.texMatrix)
        val fboTexId = fboManager?.getTextureId()
        fboManager?.unbind()

        GLES20.glViewport(0, 0, previewWidth, previewHeight)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        fboTexId?.let {
            openGlConfig.draw2DTexture(textureProgram, it)
            shaderConfig.drawShader(
                textureHandler!!,
                shaderConfig.identityMatrix,
                shaderConfig.previewVertexBuffer
            )
        }
        eglSurfaceManager.swapPreview()

        eglSurfaceManager.makeCurrentEncoder()
        GLES20.glViewport(0, 0, fboWidth, fboHeight)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        fboTexId?.let {
            openGlConfig.draw2DTexture(textureProgram, it)
            shaderConfig.drawShader(textureHandler!!, shaderConfig.identityMatrix)
        }

        eglSurfaceManager.presentationTime(timestamp)
        eglSurfaceManager.swapEncoder()
    }


    private inline fun withException(block: () -> Unit) {
        try {
            block.invoke()
        } catch (e: Exception) {
            dispatch(OnError(e))
            throw e
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

            is EncoderInitializing -> when (event) {
                InitializeOpenGL -> RenderInitializing
                else -> state
            }

            is RenderInitializing -> when (event) {
                BindCamera -> Binding
                else -> state
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

            new is EncoderInitializing && event is InitializeEncoder -> {
                initEncoder()
            }

            new is RenderInitializing && event is InitializeOpenGL -> {
                initOpenGL()
            }

            new is Binding && event is BindCamera -> {
                bindCamera()
            }

            new is Error -> { //出错直接暂停退出
                stopPreview()
            }
        }
    }


}
