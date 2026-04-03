package com.lq.audio.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.lq.audio.record.AudioConfig

data class AudioTrackConfig(
    override val sampleRate: Int = 44100,
    override val channel: Int = AudioFormat.CHANNEL_OUT_STEREO,
    override val encoding: Int = AudioFormat.ENCODING_PCM_16BIT,
    override val bufferSizeFactor: Int = 1,
    val usage: Int = AudioAttributes.USAGE_MEDIA,
    val contentType: Int = AudioAttributes.CONTENT_TYPE_MUSIC,
    val transferMode: Int = AudioTrack.MODE_STREAM,
    override val sessionId: Int = AudioManager.AUDIO_SESSION_ID_GENERATE,
) : AudioConfig() {
    private val audioAttributes by lazy {
        AudioAttributes
            .Builder()
            .setUsage(usage)
            .setContentType(contentType)
            .build()
    }

    private val audioFormat by lazy {
        AudioFormat
            .Builder()
            .setEncoding(encoding)
            .setSampleRate(sampleRate)
            .setChannelMask(channel)
            .build()
    }

    private val bufferSize by lazy {
        AudioTrack.getMinBufferSize(sampleRate, channel, encoding) * bufferSizeFactor
    }

    fun getAudioTrack(): AudioTrack =
        AudioTrack
            .Builder()
            .setAudioFormat(audioFormat)
            .setAudioAttributes(audioAttributes)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(transferMode)
            .build()
}
