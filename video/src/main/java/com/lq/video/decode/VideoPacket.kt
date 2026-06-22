package com.lq.video.decode

data class VideoPacket(
    val header: VideoPacketHeader,
    val payload: ByteArray,
    val payloadOffset: Int = 0,
    val payloadSize: Int = payload.size
)
