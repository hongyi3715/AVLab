package com.lq.video.gl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class ShaderConfig {

    private val originVertexData = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )


    private val originTexData = floatArrayOf(
        0f, 0f,
        1f, 0f,
        0f, 1f,
        1f, 1f
    )


    val verTexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(originVertexData.size * 4).order(
            ByteOrder.nativeOrder()
        ).asFloatBuffer().apply {
            put(originVertexData)
            position(0)
        }

    val texBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(originTexData.size * 4).order(
            ByteOrder.nativeOrder()
        ).asFloatBuffer().apply {
            put(originTexData)
            position(0)
        }


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

    private val oesFragmentShader = """
       #extension GL_OES_EGL_image_external : require
precision mediump float;

varying vec2 vTexCoord;
uniform samplerExternalOES uTexture;

void main() {
    vec4 color = texture2D(uTexture, vTexCoord);

    // 灰度计算（经典权重）
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));

    gl_FragColor = vec4(vec3(gray), 1.0);
}
    """.trimIndent()

    private val texture2DFragmentShader = """
precision mediump float;
varying vec2 vTexCoord;
uniform sampler2D uTexture;
void main() {
    vec4 color = texture2D(uTexture, vTexCoord);
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    gl_FragColor = vec4(vec3(gray), 1.0);
}
""".trimIndent()

    val texMatrix = FloatArray(16)

    val identityMatrix = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    val previewVertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(8 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

     fun createOesProgram(): Int {
        val vertexShader = loadShader(
            GLES20.GL_VERTEX_SHADER,
            vertexShader
        )

        val fragmentShader = loadShader(
            GLES20.GL_FRAGMENT_SHADER,
            oesFragmentShader
        )

        val program = GLES20.glCreateProgram()

        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)

        GLES20.glLinkProgram(program)

        return program
    }

    fun createTexture2DProgram():Int{
        val vertexShader = loadShader(
            GLES20.GL_VERTEX_SHADER,
            vertexShader
        )

        val fragmentShader = loadShader(
            GLES20.GL_FRAGMENT_SHADER,
            texture2DFragmentShader
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
            println("Compile Error:$error")
            throw RuntimeException(error)
        }

        return shader
    }

    fun initHandler(program: Int): ProgramHandles {
        return ProgramHandles(
            positionHandle = GLES20.glGetAttribLocation(program, "aPosition"),
            texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord"),
            textureHandle = GLES20.glGetUniformLocation(program, "uTexture"),
            texMatrixHandle = GLES20.glGetUniformLocation(program, "uTexMatrix")
        )
    }

    fun drawShader(handles: ProgramHandles, matrix: FloatArray, vertexBuffer: FloatBuffer = verTexBuffer) {
        GLES20.glEnableVertexAttribArray(handles.positionHandle)
        GLES20.glUniform1i(handles.textureHandle, 0)
        GLES20.glVertexAttribPointer(
            handles.positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer
        )

        GLES20.glEnableVertexAttribArray(handles.texCoordHandle)
        GLES20.glVertexAttribPointer(
            handles.texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texBuffer
        )

        GLES20.glUniformMatrix4fv(
            handles.texMatrixHandle, 1, false, matrix, 0
        )

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    fun updatePreviewVertexBuffer(
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int
    ) {  //通过放大，裁剪即CenterCrop效果，避免不同尺寸发生预览变形
        val srcRatio = srcWidth.toFloat() / srcHeight
        val dstRatio = dstWidth.toFloat() / dstHeight

        val scaleX: Float
        val scaleY: Float

        if (srcRatio > dstRatio) {
            scaleX = srcRatio / dstRatio
            scaleY = 1f
        } else {
            scaleX = 1f
            scaleY = dstRatio / srcRatio
        }

        previewVertexBuffer.clear()
        previewVertexBuffer.put(
            floatArrayOf(
                -scaleX, -scaleY,
                scaleX, -scaleY,
                -scaleX,  scaleY,
                scaleX,  scaleY
            )
        )
        previewVertexBuffer.position(0)
    }

}
