package com.lq.audio.coder

import android.media.MediaCodec
import android.media.MediaFormat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.nio.ByteBuffer

class AacDecoder {

    private val sampleRate = 44100
    private val channelCount = 2

    private val decoder: MediaCodec

    private val _audioFlow = MutableSharedFlow<ByteArray>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val audioFlow = _audioFlow.asSharedFlow()

    // ✔ 用稳定递增 pts（关键）
    private var pts = 0L
    private val frameDurationUs = 1024_000_000L / sampleRate // 1024 samples

    init {
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            sampleRate,
            channelCount
        )

        val csd = byteArrayOf(0x12.toByte(), 0x10.toByte())
        format.setByteBuffer("csd-0", ByteBuffer.wrap(csd))

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

            // ✔ 使用递增时间戳（关键）
            pts += frameDurationUs

            decoder.queueInputBuffer(
                inputIndex,
                0,
                frame.size,
                pts,
                0
            )
        }

        drain()
    }

    private fun drain() {

        val info = MediaCodec.BufferInfo()

        while (true) {

            val index = decoder.dequeueOutputBuffer(info, 10000)

            when (index) {

                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    // ✔ 没数据是正常的
                    return
                }

                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val newFormat = decoder.outputFormat
                    println("Decoder format changed: $newFormat")
                }

                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                    // 老设备才会走这里，一般不用管
                }

                else -> {
                    if (index >= 0) {

                        val buffer = decoder.getOutputBuffer(index)!!

                        val pcm = ByteArray(info.size)

                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)

                        buffer.get(pcm)

                        println("✅ PCM output size: ${pcm.size}")

                        _audioFlow.tryEmit(pcm)

                        decoder.releaseOutputBuffer(index, false)
                    }
                }
            }
        }
    }
}
