package com.lq.audio.coder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import com.lq.audio.buffer.FrameAlignedRingBuffer
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AacEncoder {

    private val sampleRate = 44100
    private val channelCount = 2
    private val bitRate = 128000

    private val samplesPerFrame = 1024
    private val pcmFrameSize = samplesPerFrame * channelCount * 2

    private val pcmBuffer = FrameAlignedRingBuffer(
        frameSize = pcmFrameSize,
        capacity = pcmFrameSize * 50
    )

    private val encoder: MediaCodec

    private val _aacFlow = MutableSharedFlow<ByteArray>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val aacFlow = _aacFlow.asSharedFlow()

    init {
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            sampleRate,
            channelCount
        )

        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        format.setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )

        encoder = MediaCodec.createEncoderByType(
            MediaFormat.MIMETYPE_AUDIO_AAC
        )

        encoder.configure(
            format,
            null,
            null,
            MediaCodec.CONFIGURE_FLAG_ENCODE
        )

        encoder.start()
    }

    fun encode(pcm: ByteArray) {

        pcmBuffer.write(pcm)

        val frame = ByteArray(pcmFrameSize)

        while (pcmBuffer.readFrame(frame)) {
            encodeFrame(frame)
        }
    }

    private fun encodeFrame(frame: ByteArray) {

        val inputIndex = encoder.dequeueInputBuffer(10000)
        if (inputIndex < 0) return

        val inputBuffer = encoder.getInputBuffer(inputIndex)!!
        inputBuffer.clear()
        inputBuffer.put(frame)

        encoder.queueInputBuffer(
            inputIndex,
            0,
            frame.size,
            System.nanoTime() / 1000,
            0
        )

        drain()
    }

    private fun drain() {

        val info = MediaCodec.BufferInfo()

        var index = encoder.dequeueOutputBuffer(info, 0)

        while (index >= 0) {

            val buffer = encoder.getOutputBuffer(index)!!

            val data = ByteArray(info.size)

            buffer.position(info.offset)
            buffer.limit(info.offset + info.size)

            buffer.get(data)

            _aacFlow.tryEmit(data)

            encoder.releaseOutputBuffer(index, false)

            index = encoder.dequeueOutputBuffer(info, 0)
        }
    }
}

