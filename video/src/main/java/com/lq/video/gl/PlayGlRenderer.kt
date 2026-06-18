package com.lq.video.gl

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.util.Size
import android.view.Surface
import com.lq.common.MediaClock
import com.lq.video.pipeline.RenderDecision
import com.lq.video.pipeline.VideoSyncController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class PlayGlRenderer {

    private val eglSurfaceManager = EGLSurfaceManager()
    private val openGlConfig = OpenGlConfig()

    private var oesTextureId = 0
    private var surfaceTexture: SurfaceTexture? = null

    private var outputSurface: Surface? = null
    private var inputSurface: Surface? = null

    private var playWidth = 0
    private var playHeight = 0

    private val glDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + glDispatcher)

    private val shaderConfig = PlayShaderConfig()
    private var oesProgram = 0

    private var syncController: VideoSyncController? = null

    private var frameChannel: Channel<Unit>? = null
    private var renderJob: Job? = null
    private val pendingFrameCount = AtomicInteger(0)

    fun initSyncClock(clock: MediaClock) {
        if (syncController != null) return
        syncController = VideoSyncController(clock)
    }

    suspend fun start(
        outputSurface: Surface,
        playSize: Size,
    ): Surface {
        this.outputSurface = outputSurface
        this.playWidth = playSize.width
        this.playHeight = playSize.height

        return withContext(glDispatcher) {
            initOpenGl()
            startRenderLoop()
            requireNotNull(inputSurface)
        }
    }

    private fun initOpenGl() {
        eglSurfaceManager.initPreview(requireNotNull(outputSurface))
        eglSurfaceManager.makeCurrentPreview()

        oesTextureId = openGlConfig.createOESTexture()
        oesProgram = shaderConfig.createProgram()

        frameChannel = Channel(
            capacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        surfaceTexture = SurfaceTexture(oesTextureId).apply {
            setDefaultBufferSize(playWidth, playHeight)

            inputSurface = Surface(this)

            setOnFrameAvailableListener {
                pendingFrameCount.incrementAndGet()
                frameChannel?.trySend(Unit)
            }
        }
    }

    private fun startRenderLoop() {
        if (renderJob != null) return

        renderJob = scope.launch {
            val channel = frameChannel ?: return@launch

            for (signal in channel) {
                drainFrames()
            }
        }
    }

    /**
     * 连续消费已经积压的 decoder 输出。
     *
     * Drop 时继续 latch 下一帧追赶音频；Render/Wait 处理完当前帧后返回，
     * 如果等待期间又来了新帧，最后会重新唤醒 render loop。
     */
    private suspend fun drainFrames() {
        eglSurfaceManager.makeCurrentPreview()

        val st = surfaceTexture ?: return

        while (consumeFrameSignal()) {
            val done = drawOneFrameWhenReady(st)
            if (done) break
        }

        if (pendingFrameCount.get() > 0) {
            frameChannel?.trySend(Unit)
        }
    }

    /**
     * 消费一帧 decoder 输出，然后根据音频时钟决定等待、丢弃或渲染。
     *
     * 返回 true 表示本轮已经渲染/等待过一帧，应把控制权交回事件循环；
     * 返回 false 表示当前帧被丢弃，可以继续追下一帧。
     */
    private suspend fun drawOneFrameWhenReady(st: SurfaceTexture): Boolean {
        st.updateTexImage()
        st.getTransformMatrix(shaderConfig.texMatrix)

        val videoPtsUs = st.timestamp / 1000

        while (true) {
            when (val decision = syncController?.decide(videoPtsUs) ?: RenderDecision.Render) {
                is RenderDecision.Wait -> {
                    delay(decision.delayMs.coerceIn(MIN_WAIT_MS, MAX_WAIT_STEP_MS))
                }

                is RenderDecision.Drop -> {
                    return false
                }

                is RenderDecision.Render -> {
                    drawCurrentTexture()
                    return true
                }
            }
        }
    }

    private fun consumeFrameSignal(): Boolean {
        while (true) {
            val count = pendingFrameCount.get()
            if (count <= 0) return false
            if (pendingFrameCount.compareAndSet(count, count - 1)) return true
        }
    }

    private fun drawCurrentTexture() {
        GLES20.glViewport(0, 0, playWidth, playHeight)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        shaderConfig.drawOes(
            oesProgram,
            oesTextureId,
            shaderConfig.texMatrix
        )

        eglSurfaceManager.swapPreview()
    }

    fun stop() {
        scope.launch {
            surfaceTexture?.setOnFrameAvailableListener(null)
            surfaceTexture?.release()
            surfaceTexture = null

            inputSurface?.release()
            inputSurface = null

            renderJob?.cancel()
            renderJob = null

            frameChannel?.close()
            frameChannel = null
        }
    }

    companion object {
        private const val MIN_WAIT_MS = 1L
        private const val MAX_WAIT_STEP_MS = 10L
    }
}
