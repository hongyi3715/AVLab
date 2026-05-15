package com.lq.video.record

import android.view.Surface
import com.lq.video.encode.Camera264Encoder
import java.io.File

class RecordingPipeline {
    private val encoder = Camera264Encoder()
    private val recorder = CameraRecorder(encoder)

    val inputSurface: Surface?
        get() = encoder.encodeSurface

    fun prepare(width: Int, height: Int, bitrate: Int) {
        encoder.createEncoder(width, height, bitrate)
    }

    fun start(outputFile: File) {
        recorder.startRecording(outputFile)
    }

    fun stop() {
        encoder.stop()
        recorder.stopRecording()
    }

    fun release() {
        stop()
        encoder.stop()
    }
}

