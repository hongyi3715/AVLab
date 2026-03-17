package com.lq.audio.coder

import android.media.MediaCodec
import android.media.MediaFormat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AacDecoder {

    private val sampleRate = 44100
    private val channelCount = 2

    private val decoder: MediaCodec

    private val _audioFlow = MutableSharedFlow<ByteArray>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val audioFlow = _audioFlow.asSharedFlow()

    init {

        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            sampleRate,
            channelCount
        )

        decoder = MediaCodec.createDecoderByType(
            MediaFormat.MIMETYPE_AUDIO_AAC
        )

        decoder.configure(format, null, null, 0)
        decoder.start()
    }

    fun decode(frame: ByteArray) {

        val inputIndex = decoder.dequeueInputBuffer(10000)
        if (inputIndex >= 0) {

            val inputBuffer = decoder.getInputBuffer(inputIndex)!!
            inputBuffer.clear()
            inputBuffer.put(frame)

            decoder.queueInputBuffer(
                inputIndex,
                0,
                frame.size,
                System.nanoTime() / 1000,
                0
            )
        }

        drain()
    }

    private fun drain() {

        val info = MediaCodec.BufferInfo()

        var index = decoder.dequeueOutputBuffer(info, 0)

        while (index >= 0) {

            val buffer = decoder.getOutputBuffer(index)!!

            val pcm = ByteArray(info.size)

            buffer.position(info.offset)
            buffer.limit(info.offset + info.size)

            buffer.get(pcm)

            _audioFlow.tryEmit(pcm)

            decoder.releaseOutputBuffer(index, false)

            index = decoder.dequeueOutputBuffer(info, 0)
        }
    }
}
