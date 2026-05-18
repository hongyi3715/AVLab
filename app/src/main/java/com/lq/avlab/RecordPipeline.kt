package com.lq.avlab

import com.lq.audio.AudioRecordPipeline
import com.lq.audio.coder.AudioEncodeEvent
import com.lq.video.encode.EncoderEvent
import com.lq.video.encode.VideoRecordPipeline
import com.lq.video.muxer.Mp4Muxer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

class RecordPipeline {

    private val audio = AudioRecordPipeline()
     val video = VideoRecordPipeline()


    private var muxer: Mp4Muxer? = null

    @Volatile
    private var videoReady = false
    @Volatile
    private var audioReady = false

    fun start(output: File, scope: CoroutineScope) {
        audio.initRecordFlow(scope)

        muxer = Mp4Muxer(output.path).apply { init() }

        // 视频流
        scope.launch {
            video.eventFlow.collect {
                println("VideoEvent:$it")
                when (it) {
                    is EncoderEvent.Format -> {
                        muxer?.addVideoTrack(it.format)
                        videoReady = true
                        tryStart()
                    }

                    is EncoderEvent.Data -> {
                        if (muxer?.isStarted() == true) {
                            muxer?.writeVideoData(it.data, it.bufferInfo)
                        }
                    }
                }
            }
        }

        // 音频流
        scope.launch {
            audio.audioEventFlow.collect {
                println("AudioEvent:$it")
                when (it) {
                    is AudioEncodeEvent.Format -> {
                        muxer?.addAudioTrack(it.format)
                        audioReady = true
                        tryStart()
                    }

                    is AudioEncodeEvent.Data -> {
                        if (muxer?.isStarted() == true) {
                            muxer?.writeAudioData(it.data, it.bufferInfo)
                        }
                    }
                }
            }
        }

        video.start()
        audio.startRecord()
    }

    private fun tryStart() {
        if (videoReady && audioReady && muxer?.isStarted() == false) {
            muxer?.start()
        }
    }

    suspend fun stop() {
        video.stop()
        audio.stopRecord()
        muxer?.stop()
    }

}
