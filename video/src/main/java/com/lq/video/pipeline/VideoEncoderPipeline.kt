package com.lq.video.pipeline

import android.view.Surface
import com.lq.video.encode.EncoderEvent
import kotlinx.coroutines.flow.Flow

interface VideoEncoderPipeline {
    val inputSurface: Surface?
    val eventFlow : Flow<EncoderEvent>
    fun prepare(width: Int, height: Int, bitrate: Int)
    fun startEncode()
    fun stopEncode()
    fun release()
}
