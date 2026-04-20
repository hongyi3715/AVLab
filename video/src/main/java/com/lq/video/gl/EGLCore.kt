package com.lq.video.gl

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt.EGL_RECORDABLE_ANDROID

object EGLCore {

    lateinit var display: EGLDisplay
    lateinit var context: EGLContext
    var config: EGLConfig? = null
        private set


    init {
        init()
    }

    fun init() {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        EGL14.eglInitialize(display, null, 0, null, 0)

        val attrib = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL_RECORDABLE_ANDROID, EGL14.EGL_TRUE,  // FIXME 天坑 兼容性配置
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val num = IntArray(1)
        EGL14.eglChooseConfig(display, attrib, 0, configs, 0, 1, num, 0)

        config = configs[0]

        val ctxAttr = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )

        context = EGL14.eglCreateContext(
            display,
            config,
            EGL14.EGL_NO_CONTEXT,
            ctxAttr,
            0
        )
    }
}
