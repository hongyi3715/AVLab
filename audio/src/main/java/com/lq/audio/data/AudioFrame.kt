package com.lq.audio.data

data class AudioFrame(
    val data: ByteArray,
    val trace: AudioTrace?=null
)
