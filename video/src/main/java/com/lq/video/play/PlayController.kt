package com.lq.video.play

import android.content.Context
import android.view.Surface
import androidx.lifecycle.LifecycleOwner
import com.lq.video.gl.PlayGlRenderer
import com.lq.video.view.PlayerTextureView
import android.util.Size
import androidx.lifecycle.lifecycleScope
import com.lq.common.MediaClock
import com.lq.video.pipeline.VideoPlayPipeline
import kotlinx.coroutines.launch

class PlayController(context: Context) {

    private val glRender = PlayGlRenderer()


    fun play(textureView: PlayerTextureView,lifecycleOwner: LifecycleOwner,pipeline: VideoPlayPipeline,clock: MediaClock) {

        textureView.callback = object : PlayerTextureView.Callback {
            override fun onSurfaceAvailable(surface: Surface, width: Int, height: Int) {
                println("play surface=$surface width=$width height=$height")
                lifecycleOwner.lifecycleScope.launch{
                    val decodeSurface = glRender.start(surface, Size(width, height))
                    pipeline.startDecode(lifecycleOwner.lifecycleScope,clock,surface= decodeSurface,width,height)
                    pipeline.initBufferListener(lifecycleOwner.lifecycleScope,clock)
                    pipeline.initNetReceiver(lifecycleOwner.lifecycleScope)
                }
            }

            override fun onSurfaceDestroyed(surface: Surface) {
                    pipeline.stop()
            }
        }
    }


}
