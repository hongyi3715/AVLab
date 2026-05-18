package com.lq.video.encode

import android.view.Surface

class VideoRecordPipeline {
    private val encoder = Camera264Encoder()

    val inputSurface: Surface?
        get() = encoder.encodeSurface

    val eventFlow = encoder.eventFlow

    fun prepare(width: Int, height: Int, bitrate: Int) {
        encoder.createEncoder(width, height, bitrate)
    }

    fun start() {
        encoder.startOutputThread()
    }

    fun stop() {
        encoder.stop()
    }

    fun release() {
        stop()
    }
}
