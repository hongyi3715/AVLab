package com.lq.audio.data

data class AudioEncodedFrame(
    val data: ByteArray,
    val ptsUs: Long,
    val trace: AudioTrace? = null
)
