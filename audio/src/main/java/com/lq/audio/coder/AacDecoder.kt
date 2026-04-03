package com.lq.audio.coder

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.SystemClock
import com.lq.audio.data.AudioFrame
import com.lq.audio.data.AudioTrace
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import java.nio.ByteBuffer

class AacDecoder {

    private val sampleRate = 44100
    private val channelCount = 2

    private val decoder: MediaCodec

    private val _audioFlow = Channel<AudioFrame>(capacity = 3)
    val audioFlow = _audioFlow.consumeAsFlow()

    // ✔ 用稳定递增 pts（关键）
    private var pts = 0L
    private val frameDurationUs = 1024L * 1_000_000L / sampleRate // 1024 samples

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

    // 挂起函数，保证顺序发送
    suspend fun decode(frame: AudioFrame) {
        frame.trace?.decodeStartTime = SystemClock.elapsedRealtime()

        val inputIndex = decoder.dequeueInputBuffer(10_000)
        if (inputIndex >= 0) {
            val inputBuffer = decoder.getInputBuffer(inputIndex)!!
            inputBuffer.clear()
            inputBuffer.put(frame.data)

            decoder.queueInputBuffer(
                inputIndex,
                0,
                frame.data.size,
                pts,
                0
            )
            pts += frameDurationUs
        }

        drain(trace = frame.trace)
    }

    // 串行读取 MediaCodec 输出
    private suspend fun drain(trace: AudioTrace?) {
        val info = MediaCodec.BufferInfo()
        while (true) {
            val index = decoder.dequeueOutputBuffer(info, 0)
            when {
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> return
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    println("Decoder format changed: ${decoder.outputFormat}")
                }
                index >= 0 -> {
                    val buffer = decoder.getOutputBuffer(index)!!
                    val pcm = ByteArray(info.size)
                    buffer.position(info.offset)
                    buffer.limit(info.offset + info.size)
                    buffer.get(pcm)

                    // 挂起发送，避免丢帧
                    _audioFlow.send(AudioFrame(pcm, trace))

                    decoder.releaseOutputBuffer(index, false)
                }
            }
        }
    }
}
