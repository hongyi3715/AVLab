package com.lq.video.encode

import android.view.Surface
import com.lq.video.muxer.Mp4Muxer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class RecordingPipeline {
    private val encoder = Camera264Encoder()

    val inputSurface: Surface?
        get() = encoder.encodeSurface

    private var mp4Muxer: Mp4Muxer? = null

    fun prepare(width: Int, height: Int, bitrate: Int) {
        encoder.createEncoder(width, height, bitrate)
    }

    private var coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var job: Job? = null

    @Volatile
    var isMuxing = false

    fun start(outputFile: File) {
        if (mp4Muxer == null) {
            mp4Muxer = Mp4Muxer(outputFile.path).apply {
                init()
            }
        }
        isMuxing = true
        job = coroutineScope.launch {
            encoder.eventFlow.collect {
                when (it) {
                    is EncoderEvent.Format -> {
                        if (!mp4Muxer!!.isStarted()) {
                            mp4Muxer?.addVideoTrack(it.format)
                            mp4Muxer?.start()
                        }
                    }

                    is EncoderEvent.Data -> {
                        if (!isMuxing) return@collect
                        mp4Muxer?.writeVideoData(it.data, it.bufferInfo)
                    }
                }
            }

        }
        encoder.startOutputThread()
    }

    fun stop() {
        if (!isMuxing) return
        isMuxing = false
        encoder.stop()
        job?.cancel()

        mp4Muxer?.stop()
        mp4Muxer = null
    }

    fun release() {
        stop()
    }
}
