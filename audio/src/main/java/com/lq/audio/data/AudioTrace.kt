package com.lq.audio.data

data class AudioTrace(
    var seq: Int = 0,
    var recordTime: Long = 0L,      // AudioRecord 读出 PCM
    var encodeDoneTime: Long = 0L,  // AAC 编码完成
    var sendTime: Long = 0L,        // UDP send
    var receiveTime: Long = 0L,     // UDP receive
    var bufferInTime: Long = 0L,    // 放入 jitter buffer
    var decodeStartTime: Long = 0L, // 喂给 MediaCodec decode
    var decodeOutputTime:Long = 0L,  // 解码完成时间
    var playTime: Long = 0L,         // AudioTrack.write
)
