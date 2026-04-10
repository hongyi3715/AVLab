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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraTriangleRender(context: Context) : GLSurfaceView.Renderer, Preview.SurfaceProvider {

    private var oesTextureId: Int = 0

    private var surfaceTexture: SurfaceTexture? = null

    private val mainExecutor = ContextCompat.getMainExecutor(context)

    var surface: Surface? = null
        private set

    private var program: Int = 0

    private var positionHandle: Int = 0

    private var texCoordHandle: Int = 0

    private var textureHandle = 0

    private var texMatrixHandle: Int = 0

    private fun vertexData(): FloatArray {
        return floatArrayOf(
            -1f, -1f,
            1f, -1f,
            -1f,  1f,
            1f,  1f
        )
    }

    private val texData = floatArrayOf(
        0f, 0f,
        1f, 0f,
        0f, 1f,
        1f, 1f
    )

    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(vertexData().size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertexData())
                position(0)
            }

    private val texBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(texData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(texData)
                position(0)
            }

    private val texMatrix = FloatArray(16)

    private val vertexShader = """
    attribute vec4 aPosition;
    attribute vec2 aTexCoord;

    uniform mat4 uTexMatrix;

    varying vec2 vTexCoord;

    void main() {
        gl_Position = aPosition;
        vTexCoord = (uTexMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;
    }
    """.trimIndent()

    private val fragmentShader = """
    #extension GL_OES_EGL_image_external : require

    precision mediump float;

    uniform samplerExternalOES uTexture;
    varying vec2 vTexCoord;

    void main() {
        gl_FragColor = texture2D(uTexture, vTexCoord);
    }
""".trimIndent()

    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int
    ) {
        GLES20.glViewport(0, 0, width, height)
        surfaceTexture?.setDefaultBufferSize(width, height)
        println("Screen size: $width x $height, ratio: ${width.toFloat()/height}")
        // 关键：根据实际 View 宽高重新计算顶点坐标
        updateVertices(width, height)
    }

    override fun onSurfaceCreated(
        gl: GL10?,
        config: EGLConfig?
    ) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        oesTextureId = textures[0]


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

        // 3. 创建 SurfaceTexture
        surfaceTexture = SurfaceTexture(oesTextureId)

        // 4. 创建 Surface，后面给 CameraX
        surface = Surface(surfaceTexture)

        // 5. 编译 shader，创建 OpenGL program
        program = createProgram(vertexShader, fragmentShader)

        // 6. 获取 attribute/uniform location
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
        texMatrixHandle = GLES20.glGetUniformLocation(program, "uTexMatrix")
    }

    override fun onDrawFrame(gl: GL10?) {
        println("onDrawFrame")

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        surfaceTexture?.updateTexImage()
        surfaceTexture?.getTransformMatrix(texMatrix)

        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glUniform1i(textureHandle, 0)
        GLES20.glVertexAttribPointer(
            positionHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            vertexBuffer
        )

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(
            texCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            texBuffer
        )

        GLES20.glUniformMatrix4fv(
            texMatrixHandle,
            1,
            false,
            texMatrix,
            0
        )

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }


    private fun createProgram(
        vertexSource: String,
        fragmentSource: String
    ): Int {
        val vertexShader = loadShader(
            GLES20.GL_VERTEX_SHADER,
            vertexSource
        )

        val fragmentShader = loadShader(
            GLES20.GL_FRAGMENT_SHADER,
            fragmentSource
        )

        val program = GLES20.glCreateProgram()

        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)

        GLES20.glLinkProgram(program)

        return program
    }

    private fun loadShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val result = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, result, 0)

        if (result[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException(error)
        }

        return shader
    }

    override fun onSurfaceRequested(request: SurfaceRequest) {
        val surface = surface
        println("onSurfaceRequested :$surface")

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

    private fun updateVertices(width: Int, height: Int) {
        val screenRatio = width.toFloat() / height.toFloat() // 0.45
        val cameraRatio = 1200f / 1600f                      // 0.75

        var scaleX = 1.0f
        var scaleY = 1.0f

        // 目标：Center Crop (填满屏幕，裁剪掉相机多余的部分)
        if (screenRatio < cameraRatio) {
            // 屏幕太窄了，我们需要放大 X 轴，把相机画面的左右两侧伸出屏幕外
            scaleX = cameraRatio / screenRatio
        } else {
            // 如果屏幕太宽，则放大 Y 轴，把上下裁掉（你的情况不会走这里）
            scaleY = screenRatio / cameraRatio
        }

        // 打印一下缩放值，你会发现 scaleX 应该大于 1.0
         println("scaleX: $scaleX, scaleY: $scaleY")

        val cubeCoords = floatArrayOf(
            -1.0f * scaleX, -1.0f * scaleY,
            1.0f * scaleX, -1.0f * scaleY,
            -1.0f * scaleX,  1.0f * scaleY,
            1.0f * scaleX,  1.0f * scaleY
        )

        vertexBuffer.clear()
        vertexBuffer.put(cubeCoords).position(0)
    }

    private fun release() {

    }
}
