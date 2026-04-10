package com.lq.avlab.view

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView


@Composable
fun AndroidTextureView(
    modifier: Modifier = Modifier,
    onSurfaceAvailable :(Surface)-> Unit = {}
){
    AndroidView(
        modifier = modifier,
        factory = { context->
            TextureView(context).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        onSurfaceAvailable(Surface(surface))
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        TODO("Not yet implemented")
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        TODO("Not yet implemented")
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                        TODO("Not yet implemented")
                    }
                }
            }
        }, update = { textureView ->

        }
    )
}
