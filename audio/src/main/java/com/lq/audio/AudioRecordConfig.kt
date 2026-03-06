package com.lq.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder

data class AudioRecordConfig(
    val audioSource: Int = MediaRecorder.AudioSource.VOICE_COMMUNICATION,
    override val sampleRate: Int = 16000,
    override val channel: Int = AudioFormat.CHANNEL_IN_MONO,
    override val encoding: Int = AudioFormat.ENCODING_PCM_16BIT,
    override val bufferSizeFactor: Int = 2,
    override val sessionId: Int = AudioManager.AUDIO_SESSION_ID_GENERATE,
    private val ringBufferSizeFactory : Int = 8
) : AudioConfig() {
    private val audioFormat by lazy {
        AudioFormat
            .Builder()
            .setEncoding(encoding)
            .setSampleRate(sampleRate)
            .setChannelMask(channel)
            .build()
    }

    val bufferSize by lazy {
        AudioRecord.getMinBufferSize(sampleRate, channel, encoding) * bufferSizeFactor
    }

    val ringBufferSize by lazy {
        bufferSize * ringBufferSizeFactory
    }

    @SuppressLint("MissingPermission")
    fun getAudioRecord(): AudioRecord? {
        if (bufferSize <= 0) return null
        return AudioRecord
            .Builder()
            .setAudioSource(audioSource)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .build()
    }
}
