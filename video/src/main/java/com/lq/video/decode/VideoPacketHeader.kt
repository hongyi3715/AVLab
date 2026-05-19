package com.lq.video.decode

data class VideoPacketHeader(
    val seq: Int,
    val timestampUs: Long,
    val frameId: Int,
    val fragIndex: Int,
    val fragCount: Int,
    val flags: Int
)
