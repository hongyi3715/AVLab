package com.lq.video.view

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.core.content.ContextCompat

class MyTextureView(context: Context) : TextureView(context), Preview.SurfaceProvider,
    TextureView.SurfaceTextureListener {

    private var pendingRequest: SurfaceRequest? = null
    private var currentSurface: Surface? = null

    init {
        surfaceTextureListener = this
    }

    interface SurfaceFrameAvailable {
        fun onFrame()
    }

    var surfaceFrameAvailableListener: SurfaceFrameAvailable? = null

    override fun onSurfaceRequested(request: SurfaceRequest) {
        val st = surfaceTexture
        if (st != null && isAvailable) {
            provideSurface(request, st)
        } else {
            pendingRequest = request
        }
    }

    override fun onSurfaceTextureAvailable(
        surface: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        pendingRequest?.let {
            provideSurface(it, surface)
            pendingRequest = null
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        pendingRequest?.willNotProvideSurface()
        pendingRequest = null
        currentSurface?.release()
        currentSurface = null
        return true
    }

    override fun onSurfaceTextureSizeChanged(
        surface: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        println("SurfaceTextureSizeChanged width:$width height:$height")
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

    }

    private fun provideSurface(request: SurfaceRequest, st: SurfaceTexture) {
        println("ProvideSurface width:${request.resolution.width} height:${request.resolution.height}")
        st.setDefaultBufferSize(request.resolution.width, request.resolution.height)
        val surface = Surface(st)
        currentSurface = surface
        st.setOnFrameAvailableListener {
            surfaceFrameAvailableListener?.onFrame()
        }
        request.provideSurface(surface, ContextCompat.getMainExecutor(context)) { result ->
            surface.release()
            currentSurface = null
        }
    }
}
