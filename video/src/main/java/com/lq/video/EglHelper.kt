package com.lq.video

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.view.Surface

class EglHelper {

    private var eglDisplay: EGLDisplay? = null
    private var eglConfig: EGLConfig? = null
    private var previewEglSurface: EGLSurface? = null
    private var encoderEglSurface: EGLSurface? = null

    private var previewContext: EGLContext? = null

    private var encoderContext: EGLContext? = null


    fun initEgl() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        EGL14.eglInitialize(eglDisplay, null, 0, null, 0)

        val attribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val num = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, attribs, 0, configs, 0, 1, num, 0)
        eglConfig = configs[0]

        val ctxAttrib = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )

        previewContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, ctxAttrib, 0)

        encoderContext = EGL14.eglCreateContext(eglDisplay,eglConfig,previewContext,ctxAttrib,0)

    }

    fun initSurfaces(previewSurface: Surface, encoderInputSurface: Surface) {
        previewEglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay, eglConfig, previewSurface, intArrayOf(EGL14.EGL_NONE), 0
        )
        encoderEglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay, eglConfig, encoderInputSurface, intArrayOf(EGL14.EGL_NONE), 0
        )
    }


    fun makeCurrent2Screen() {
        EGL14.eglMakeCurrent(eglDisplay, previewEglSurface, previewEglSurface, previewContext)
    }

    fun swapBuffers2Screen() {
        EGL14.eglSwapBuffers(eglDisplay, previewEglSurface)
    }

    fun makeCurrent2Encoder() {
        EGL14.eglMakeCurrent(eglDisplay, encoderEglSurface, encoderEglSurface, encoderContext)
    }

    fun swapBuffers2Encoder() {
        EGL14.eglSwapBuffers(eglDisplay, encoderEglSurface)
    }

    fun eglPresentationTime(timestamp: Long) {
        EGLExt.eglPresentationTimeANDROID(
            eglDisplay,
            encoderEglSurface,
            timestamp
        )
    }

}
