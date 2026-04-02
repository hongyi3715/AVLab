package com.lq.audio

import com.lq.audio.data.AudioTrace

data class AudioPacket(
    val seq: Int,          // 递增序号
    val timestamp: Long,   // 采样时间（或系统时间）
    val payload: ByteArray, // PCM 或 AAC
    var trace: AudioTrace? = null
)
