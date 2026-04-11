package com.lq.video

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.Surface
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.core.content.ContextCompat
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraTriangleRender(context: Context) : GLSurfaceView.Renderer, Preview.SurfaceProvider {

    private var oesTextureId: Int = 0

    private var surfaceTexture: SurfaceTexture? = null

    private val mainExecutor = ContextCompat.getMainExecutor(context)

    var surface: Surface? = null
        private set

    private var program: Int = 0

    private val shaderConfig = ShaderConfig()


    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int
    ) {
        GLES20.glViewport(0, 0, width, height)
        updateVertices(width, height)
    }

    override fun onSurfaceCreated(
        gl: GL10?,
        config: EGLConfig?
    ) {
        gleConfig()
    }

    override fun onDrawFrame(gl: GL10?) {
        draw()
    }


    override fun onSurfaceRequested(request: SurfaceRequest) {
        println("onSurfaceRequested, surface is null: ${surface == null}")
        val cameraWidth = request.resolution.width   // 1600
        val cameraHeight = request.resolution.height // 1200
        surfaceTexture?.setDefaultBufferSize(cameraWidth, cameraHeight)

        val surface = surface
        if (surface == null) {
            request.willNotProvideSurface()
            return
        }

        request.provideSurface(
            surface,
            mainExecutor
        ) {

        }
    }

    private fun draw(){
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        surfaceTexture?.updateTexImage()
        surfaceTexture?.getTransformMatrix(shaderConfig.texMatrix)

        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)

        GLES20.glEnableVertexAttribArray(shaderConfig.positionHandle)
        GLES20.glUniform1i(shaderConfig.textureHandle, 0)
        GLES20.glVertexAttribPointer(
            shaderConfig.positionHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            shaderConfig.verTexBuffer
        )

        GLES20.glEnableVertexAttribArray(shaderConfig.texCoordHandle)
        GLES20.glVertexAttribPointer(
            shaderConfig.texCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            shaderConfig.texBuffer
        )

        GLES20.glUniformMatrix4fv(
            shaderConfig.texMatrixHandle,
            1,
            false,
            shaderConfig.texMatrix,
            0
        )

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun gleConfig(){
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)

        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        oesTextureId = textures[0]

        surfaceTexture = SurfaceTexture(oesTextureId)

        surface = Surface(surfaceTexture)
        program = shaderConfig.createProgram()
        shaderConfig.initHandler(program)
    }

    private fun updateVertices(width: Int, height: Int) {
        val screenRatio = width.toFloat() / height  // 1080/2376 = 0.45
        val cameraRatio = 1200f / 1600f             // 改成这个，= 1.33

        var scaleX = 1.0f
        var scaleY = 1.0f

        if (screenRatio < cameraRatio) {
            scaleX = cameraRatio / screenRatio  // 1.33/0.45 = 2.96
        } else {
            scaleY = screenRatio / cameraRatio
        }

        val cubeCoords = floatArrayOf(
            -scaleX, -scaleY,
            scaleX, -scaleY,
            -scaleX,  scaleY,
            scaleX,  scaleY
        )
        shaderConfig.verTexBuffer.apply {
            clear()
            put(cubeCoords).position(0)
        }
    }

}
