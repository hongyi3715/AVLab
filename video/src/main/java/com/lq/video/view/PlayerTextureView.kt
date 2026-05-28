package com.lq.video.view

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.TextureView
import android.util.AttributeSet
import android.view.Surface

class PlayerTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : TextureView(context, attrs), TextureView.SurfaceTextureListener {

    interface Callback {
        fun onSurfaceAvailable(surface: Surface, width: Int, height: Int)
        fun onSurfaceSizeChanged(width: Int, height: Int) {}
        fun onSurfaceDestroyed(surface: Surface) {}
    }

    var callback: Callback? = null
        set(value) {
            field = value

            // 防止设置 callback 时 TextureView 已经可用了，却错过 available 回调
            val st = surfaceTexture
            if (value != null && isAvailable && st != null && currentSurface == null) {
                val surface = Surface(st)
                currentSurface = surface
                value.onSurfaceAvailable(surface, width, height)
            }
        }

    private var currentSurface: Surface? = null

    init {
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        val surface = Surface(surfaceTexture)
        currentSurface = surface
        callback?.onSurfaceAvailable(surface, width, height)
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        currentSurface?.let { surface ->
            callback?.onSurfaceDestroyed(surface)
            surface.release()
        }
        currentSurface = null
        return true
    }

    override fun onSurfaceTextureSizeChanged(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        callback?.onSurfaceSizeChanged(width, height)
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        // 播放器场景一般不需要处理
    }
}
