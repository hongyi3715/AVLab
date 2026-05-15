package com.lq.video.gl

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.util.Size
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class DefaultGlRenderer : GLRenderer {
    private val glDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + glDispatcher)

    private val eglSurfaceManager = EGLSurfaceManager()
    private val shaderConfig = ShaderConfig()
    private val openGlConfig = OpenGlConfig()

    private var previewSurface: Surface? = null
    private var encoderSurface: Surface? = null
    private var cameraSurface: Surface? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var fboManager: FboManager? = null

    private var oesProgram = 0
    private var textureProgram = 0
    private var oesTextureId = 0
    private var oesHandler: ProgramHandles? = null
    private var textureHandler: ProgramHandles? = null

    private var previewWidth = 0
    private var previewHeight = 0
    private var frameWidth = 0
    private var frameHeight = 0

    private var frameChannel: Channel<Unit>? = null
    private var renderJob: Job? = null

    override val cameraInputSurface: Surface
        get() = requireNotNull(cameraSurface)


    @Volatile
    private var encoderEnabled: Boolean = false

    override suspend fun start(
        previewSurface: Surface,
        encoderSurface: Surface,
        previewSize: Size,
        frameSize: Size
    ) {
        this.previewSurface = previewSurface
        this.encoderSurface = encoderSurface
        this.previewWidth = previewSize.width
        this.previewHeight = previewSize.height
        this.frameWidth = frameSize.width
        this.frameHeight = frameSize.height

        withContext(glDispatcher) {
            initGl()
            startRenderLoop()
        }
    }

    override fun onPreviewSizeChanged(width: Int, height: Int) {
        previewWidth = width
        previewHeight = height
    }

    override fun attachEncoderSurface(surface: Surface) {
        encoderSurface = surface
        eglSurfaceManager.initEncoder(surface)
        encoderEnabled = true
    }

    override fun detachEncoderSurface() {
        encoderEnabled = false
        eglSurfaceManager.releaseEncoder()
        encoderSurface = null
    }

    override fun stop() {
        scope.launch {
            releaseGl()
        }
    }

    private fun initGl() {
        eglSurfaceManager.initPreview(requireNotNull(previewSurface))
        eglSurfaceManager.initEncoder(requireNotNull(encoderSurface))

        eglSurfaceManager.makeCurrentPreview()

        oesTextureId = openGlConfig.createOESTexture()
        surfaceTexture = SurfaceTexture(oesTextureId).apply {
            setDefaultBufferSize(frameWidth, frameHeight)
            fboManager = FboManager(frameWidth, frameHeight)
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
    }

    private fun startRenderLoop() {
        if (renderJob != null) return
        if (frameChannel == null) return
        renderJob = scope.launch {
            try {
                for (signal in frameChannel) {
                    drawFrame()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun drawFrame() {
        if (oesHandler == null) return
        if (textureHandler == null) return
        val st = surfaceTexture ?: return

        shaderConfig.updatePreviewVertexBuffer(
            srcWidth = frameHeight,
            srcHeight = frameWidth,
            dstWidth = previewWidth,
            dstHeight = previewHeight
        )

        st.updateTexImage()
        st.getTransformMatrix(shaderConfig.texMatrix)
        val timestamp = st.timestamp

        eglSurfaceManager.makeCurrentPreview()
        fboManager?.bind()
        GLES20.glViewport(0, 0, frameWidth, frameHeight)
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

        if (!encoderEnabled || encoderSurface == null) return //部分条件下停止渲染到编码器

        eglSurfaceManager.makeCurrentEncoder()
        GLES20.glViewport(0, 0, frameWidth, frameHeight)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        fboTexId?.let {
            openGlConfig.draw2DTexture(textureProgram, it)
            shaderConfig.drawShader(textureHandler!!, shaderConfig.identityMatrix)
        }

        eglSurfaceManager.presentationTime(timestamp)
        eglSurfaceManager.swapEncoder()
    }

    private fun releaseGl() {
        surfaceTexture?.setOnFrameAvailableListener(null)
        surfaceTexture?.release()
        renderJob?.cancel()
        renderJob = null
        fboManager?.release()
    }
}
