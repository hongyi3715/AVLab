package com.lq.audio.data

data class AudioFrame(
    val ptsUs:Long,
    val trace: AudioTrace?=null,
    val data: ByteArray,
)
