package com.lq.video.decode

data class VideoPacket(
    val header: VideoPacketHeader,
    val payload: ByteArray
)
