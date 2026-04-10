package com.lq.video

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet


class GLCameraView(context: Context, attrs: AttributeSet? = null) :
    GLSurfaceView(context, attrs) {

    val render = CameraTriangleRender(context)


    init {
        setEGLContextClientVersion(2)
        setRenderer(render)
        renderMode = RENDERMODE_CONTINUOUSLY
    }


}
