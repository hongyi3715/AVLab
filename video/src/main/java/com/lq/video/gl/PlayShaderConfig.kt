package com.lq.video.gl

import android.opengl.GLES11Ext
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class PlayShaderConfig {

    private val vertexData = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f,  1f,
        1f,  1f
    )

    private val texData = floatArrayOf(
        0f, 0f,
        1f, 0f,
        0f, 1f,
        1f, 1f
    )

    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertexData)
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

    val texMatrix = FloatArray(16)

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
            gl_FragColor = texture2D(uTexture, vTexCoord);
        }
    """.trimIndent()

    fun createProgram(): Int {
        val vertex = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        val fragment = loadShader(GLES20.GL_FRAGMENT_SHADER, oesFragmentShader)

        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertex)
        GLES20.glAttachShader(program, fragment)
        GLES20.glLinkProgram(program)

        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val error = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Program link error: $error")
        }

        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)

        return program
    }

    fun drawOes(program: Int, oesTextureId: Int, matrix: FloatArray) {
        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        val textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
        val texMatrixHandle = GLES20.glGetUniformLocation(program, "uTexMatrix")

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glUniform1i(textureHandle, 0)

        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            vertexBuffer
        )

        texBuffer.position(0)
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(
            texCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            texBuffer
        )

        GLES20.glUniformMatrix4fv(texMatrixHandle, 1, false, matrix, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }

    private fun loadShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)

        if (status[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Shader compile error: $error")
        }

        return shader
    }
}
