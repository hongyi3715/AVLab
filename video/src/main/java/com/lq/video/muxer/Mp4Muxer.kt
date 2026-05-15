package com.lq.video.muxer

import android.media.AudioFormat
import android.media.MediaMuxer

class Mp4Muxer(outputPath:String) {

    private val muxer = MediaMuxer(
        outputPath,
        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
    )

    fun addAudioTrack(format: AudioFormat){

    }

}
