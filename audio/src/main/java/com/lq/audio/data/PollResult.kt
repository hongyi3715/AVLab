package com.lq.audio.data

import com.lq.audio.AudioPacket

sealed interface PollResult {
    data class Packet(val packet: AudioPacket) : PollResult
    object Wait : PollResult          // 还有机会等到，不要补静音
    object Lost : PollResult          // 确认丢包，可以补静音
}
