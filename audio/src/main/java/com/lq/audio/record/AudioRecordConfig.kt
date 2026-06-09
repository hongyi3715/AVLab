package com.lq.audio.record

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import com.lq.audio.record.AudioConfig

data class AudioRecordConfig(
    val audioSource: Int = MediaRecorder.AudioSource.VOICE_COMMUNICATION,
    override val sampleRate: Int = 44100,
    override val channel: Int = AudioFormat.CHANNEL_IN_STEREO,
    override val encoding: Int = AudioFormat.ENCODING_PCM_16BIT,
    override val bufferSizeFactor: Int = 2,
    override val sessionId: Int = AudioManager.AUDIO_SESSION_ID_GENERATE,
    private val ringBufferSizeFactory : Int = 8,
    private val frameDurationMs: Int = 20  // 明确定义帧长
) : AudioConfig() {
    private val audioFormat by lazy {
        AudioFormat
            .Builder()
            .setEncoding(encoding)
            .setSampleRate(sampleRate)
            .setChannelMask(channel)
            .build()
    }


    private val samplesPerFrame by lazy {
        sampleRate * frameDurationMs / 1000  // 44100 * 20 / 1000 = 882
    }

    val frameSize by lazy {
        val bytesPerSample = when (encoding) {
            AudioFormat.ENCODING_PCM_16BIT -> 2
            AudioFormat.ENCODING_PCM_8BIT -> 1
            AudioFormat.ENCODING_PCM_FLOAT -> 4
            else -> 2
        }
        val channelCount = if (channel == AudioFormat.CHANNEL_IN_STEREO) 2 else 1
        samplesPerFrame * bytesPerSample * channelCount
    }

    private val hwBufferSize by lazy {
        maxOf(
            AudioRecord.getMinBufferSize(sampleRate, channel, encoding) * bufferSizeFactor,
            frameSize * 4  // 至少能装4帧
        )
    }

    @SuppressLint("MissingPermission")
    fun getAudioRecord(): AudioRecord? {
        if (hwBufferSize <= 0) return null
        return AudioRecord
            .Builder()
            .setAudioSource(audioSource)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(hwBufferSize)
            .build()
    }
}
