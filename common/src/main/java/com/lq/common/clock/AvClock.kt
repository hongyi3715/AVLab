package com.lq.common.clock

interface AvClock {
    fun audioClockUs(): Long
    fun updateAudioAnchor(
        audioPtsUs: Long,
        writtenFrames: Long,
        playbackHeadFrames: Long
    )
}
