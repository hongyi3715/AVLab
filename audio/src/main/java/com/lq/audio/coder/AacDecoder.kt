package com.lq.audio.coder

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.SystemClock
import com.lq.audio.data.AudioTrace
import com.lq.audio.data.AudioEncodedFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class AacDecoder {

    private val sampleRate = 44100
    private val channelCount = 2

    private val decoder: MediaCodec

    private val _audioFlow = Channel<AudioEncodedFrame>(
        capacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val audioFlow = _audioFlow.receiveAsFlow()

    private val audioDispatcher = Executors
        .newSingleThreadExecutor { r ->
            Thread(r, "AudioDecoderThread").apply {
                priority = Thread.MAX_PRIORITY
            }
        }
        .asCoroutineDispatcher()

    private val codecScope = CoroutineScope(
        audioDispatcher + SupervisorJob()
    )


    // 输入 trace 队列，保证输出时对应正确
    private val traceQueue = ArrayDeque<AudioTrace?>()

    @Volatile
    private var running = true

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

    private fun initDecode() = codecScope.launch {
        val info = MediaCodec.BufferInfo()

        while (running) {

            // ① 先尽可能 drain output（关键改动）
            drainOutput(info)

            // ② 再稍微让出 CPU（避免100%空转）
            Thread.sleep(1)
        }
    }

    private fun drainOutput(info: MediaCodec.BufferInfo) {

        while (true) {

            val index = decoder.dequeueOutputBuffer(info, 0)

            when {
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    return
                }

                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    continue
                }

                index >= 0 -> {

                    val buffer = decoder.getOutputBuffer(index)
                    if (buffer != null && info.size > 0) {

                        val pcm = ByteArray(info.size)
                        buffer.get(pcm)
                        buffer.clear()

                        val trace = traceQueue.removeFirstOrNull()
                        trace?.decodeOutputTime = SystemClock.elapsedRealtime()

                        _audioFlow.trySend(
                            AudioEncodedFrame(pcm, info.presentationTimeUs, trace)
                        )
                    }

                    decoder.releaseOutputBuffer(index, false)

                    continue
                }
            }
        }
    }


    fun decode(frame: AudioEncodedFrame) {

        val inputIndex = decoder.dequeueInputBuffer(5_000)
        if (inputIndex < 0) return

        frame.trace?.decodeStartTime = SystemClock.elapsedRealtime()

        val inputBuffer = decoder.getInputBuffer(inputIndex) ?: return
        inputBuffer.clear()
        inputBuffer.put(frame.data)

        traceQueue.addLast(frame.trace)

        decoder.queueInputBuffer(
            inputIndex,
            0,
            frame.data.size,
            frame.ptsUs,
            0
        )
    }

}
