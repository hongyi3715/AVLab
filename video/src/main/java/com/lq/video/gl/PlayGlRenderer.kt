package com.lq.video.gl

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.util.Size
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

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

    private var fboManager: FboManager? = null

    private val shaderConfig = PlayShaderConfig()

    private var oesProgram = 0

    suspend fun start(
        outputSurface: Surface,
        playSize: Size,
    ) : Surface{
        this.outputSurface = outputSurface
        this.playWidth = playSize.width
        this.playHeight = playSize.height

        return withContext(glDispatcher) {
            initOpenGl()
            requireNotNull(inputSurface)
        }
    }

    private fun initOpenGl() {
        eglSurfaceManager.initPreview(requireNotNull(outputSurface))

        eglSurfaceManager.makeCurrentPreview()

        oesTextureId = openGlConfig.createOESTexture()
        oesProgram = shaderConfig.createProgram()
        surfaceTexture = SurfaceTexture(oesTextureId).apply {
            setDefaultBufferSize(playWidth, playHeight)
            inputSurface  = Surface(this)

            setOnFrameAvailableListener {
                scope.launch {
                    drawFrame()
                }
            }
        }
    }

    private fun drawFrame(){
        eglSurfaceManager.makeCurrentPreview()

        val st = surfaceTexture ?: return
        st.updateTexImage()
        st.getTransformMatrix(shaderConfig.texMatrix)

        GLES20.glViewport(0, 0, playWidth, playHeight)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        shaderConfig.drawOes(oesProgram, oesTextureId, shaderConfig.texMatrix)

        eglSurfaceManager.swapPreview()
    }

}
