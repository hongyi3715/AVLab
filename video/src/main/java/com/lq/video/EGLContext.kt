package com.lq.video

import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.view.Surface

class CameraEGLContext {

    var encodeSurface: Surface? = null
    var encoderEglSurface: EGLSurface? = null
    var eglDisplay: EGLDisplay? = null
    var eglContext: EGLContext? = null
}
