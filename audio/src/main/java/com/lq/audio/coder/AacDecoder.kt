package com.lq.audio.coder

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.SystemClock
import com.lq.audio.data.AudioFrame
import com.lq.audio.data.AudioTrace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class AacDecoder {

    private val sampleRate = 44100
    private val channelCount = 2

    private val decoder: MediaCodec

    private val _audioFlow = Channel<AudioFrame>(
        capacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val audioFlow = _audioFlow.consumeAsFlow()

    private var pts = 0L
    private val frameDurationUs = 1024L * 1_000_000L / sampleRate

    // 输入 trace 队列，保证输出时对应正确
    private val traceQueue = ArrayDeque<AudioTrace?>()

    init {
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            sampleRate,
            channelCount
        )

        format.setByteBuffer(
            "csd-0",
            ByteBuffer.wrap(byteArrayOf(0x12, 0x10))
        )

        decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        decoder.configure(format, null, null, 0)
        decoder.start()

        initDecode()
    }

    private fun initDecode() = CoroutineScope(Dispatchers.IO).launch {
        val info = MediaCodec.BufferInfo()
        while(true) {
            val outputIndex = decoder.dequeueOutputBuffer(info, 10_000)
            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    // 不delay，下一次循环继续尝试
                    continue
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {}
                outputIndex >= 0 -> {
                    val buffer = decoder.getOutputBuffer(outputIndex) ?: continue
                    val pcm = ByteArray(info.size)
                    buffer.get(pcm)
                    val trace = traceQueue.removeFirstOrNull()
                    trace?.decodeOutputTime = SystemClock.elapsedRealtime()

                    _audioFlow.send(AudioFrame(pcm, trace))
                    decoder.releaseOutputBuffer(outputIndex, false)
                }
            }
        }
    }

     fun decode(frame: AudioFrame) {
        val trace = frame.trace
        trace?.decodeStartTime = SystemClock.elapsedRealtime()

        val inputIndex = decoder.dequeueInputBuffer(5_000)
        if (inputIndex < 0) return

        val inputBuffer = decoder.getInputBuffer(inputIndex) ?: return
        inputBuffer.clear()
        inputBuffer.put(frame.data)

        // 先保存 trace，对应这个输入 AAC 帧
        traceQueue.addLast(trace)

        decoder.queueInputBuffer(
            inputIndex,
            0,
            frame.data.size,
            pts,
            0
        )
        pts += frameDurationUs

    }

}
