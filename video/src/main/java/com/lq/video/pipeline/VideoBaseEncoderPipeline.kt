package com.lq.video.pipeline

import android.view.Surface
import com.lq.video.encode.Camera264Encoder


open class VideoBaseEncoderPipeline : VideoEncoderPipeline {

    protected val encoder = Camera264Encoder()

    override val inputSurface: Surface?
        get() = encoder.encodeSurface

    override val eventFlow = encoder.eventFlow

    override fun prepare(width: Int, height: Int, bitrate: Int) {
        encoder.createEncoder(width, height, bitrate)
    }

    override fun startEncode() {
        encoder.startOutputThread()
    }

    override fun stopEncode() {
        encoder.stop()
    }

    override fun release() {
        stopEncode()
    }

    protected fun collectFrames() {

    }
}
