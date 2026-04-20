package com.lq.video.gl
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.view.Surface


class EGLSurfaceManager {

    private var display = EGLCore.display
    private var config = EGLCore.config
    private var context = EGLCore.context

    private var previewSurface: EGLSurface? = null
    private var encoderSurface: EGLSurface? = null

    fun initPreview(surface: Surface) {
        previewSurface = EGL14.eglCreateWindowSurface(
            display, config, surface, intArrayOf(EGL14.EGL_NONE), 0
        )
    }

    fun initEncoder(surface: Surface) {
        encoderSurface = EGL14.eglCreateWindowSurface(
            display, config, surface, intArrayOf(EGL14.EGL_NONE), 0
        )
    }

    fun makeCurrentPreview() {
        EGL14.eglMakeCurrent(display, previewSurface, previewSurface, context)
    }

    fun makeCurrentEncoder() {
        EGL14.eglMakeCurrent(display, encoderSurface, encoderSurface, context)
    }

    fun swapPreview() {
        EGL14.eglSwapBuffers(display, previewSurface)
    }

    fun presentationTime(timeStamp: Long?){
        timeStamp?.let {
            EGLExt.eglPresentationTimeANDROID(
                EGLCore.display,
                encoderSurface,
                it
            )
        }
    }

    fun swapEncoder() {
        EGL14.eglSwapBuffers(display, encoderSurface)
    }
}
