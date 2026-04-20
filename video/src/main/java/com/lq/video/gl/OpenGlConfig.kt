package com.lq.video.gl

import android.opengl.GLES11Ext
import android.opengl.GLES20

class OpenGlConfig {

    fun createOESTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val oesTextureId = textures[0]

        GLES11Ext.GL_TEXTURE_EXTERNAL_OES.also { target ->
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glBindTexture(target, oesTextureId)
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        }
        return oesTextureId
    }


    fun drawFrame(
        program: Int,
        oesTextureId: Int
    ) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
    }


}
