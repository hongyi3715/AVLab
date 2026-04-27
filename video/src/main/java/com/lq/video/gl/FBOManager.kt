package com.lq.video.gl

import android.opengl.GLES20

class FboManager(val width: Int, val height: Int) {
    private val fboId = IntArray(1)
    private val fboTextureId = IntArray(1)
    init {
        // 1. 生成 FBO
        GLES20.glGenFramebuffers(1, fboId, 0)

        // 2. 生成一张纹理用于存储 FBO 的内容
        GLES20.glGenTextures(1, fboTextureId, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId[0])

        // 分配空间
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
            0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)

        // 设置纹理参数
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        // 3. 将纹理绑定到 FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId[0])
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D, fboTextureId[0], 0)

        // 检查状态
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("FBO 创建失败")
        }

        // 解绑
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun bind() = GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId[0])

    fun unbind() = GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

    fun getTextureId() = fboTextureId[0]

    fun release() {
        GLES20.glDeleteFramebuffers(1, fboId, 0)
        GLES20.glDeleteTextures(1, fboTextureId, 0)
    }
}
