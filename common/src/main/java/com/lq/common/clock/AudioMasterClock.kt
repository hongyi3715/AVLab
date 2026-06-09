package com.lq.common.clock

class AudioMasterClock(
    private val sampleRate: Int
) : AvClock {

    @Volatile
    private var anchorPtsUs: Long = 0L

    @Volatile
    private var anchorPlaybackHeadFrames: Long = 0L

    @Volatile
    private var audioTrackProvider: (() -> Long)? = null

    fun bindPlaybackHead(provider: () -> Long) {
        audioTrackProvider = provider
    }

    override fun updateAudioAnchor(
        audioPtsUs: Long,
        writtenFrames: Long,
        playbackHeadFrames: Long
    ) {
        anchorPtsUs = audioPtsUs
        anchorPlaybackHeadFrames = playbackHeadFrames
    }

    override fun audioClockUs(): Long {
        val currentPlaybackFrames = audioTrackProvider?.invoke() ?: return anchorPtsUs
        val playedFrames = currentPlaybackFrames - anchorPlaybackHeadFrames
        return anchorPtsUs + playedFrames * 1_000_000L / sampleRate
    }
}
