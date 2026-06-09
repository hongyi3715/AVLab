package com.lq.audio.data

data class AudioFrame(
    val data: ByteArray,
    val ptsUs:Long,
    val trace: AudioTrace?=null
)
