package com.lq.audio.coder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.SystemClock
import com.lq.audio.AudioPacket
import com.lq.audio.buffer.FrameAlignedRingBuffer
import com.lq.audio.data.AudioTrace
import com.lq.audio.data.AudioFrame
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AacEncoder {

    private val sampleRate = 44100
    private val channelCount = 2
    private val bitRate = 256000

    private val samplesPerFrame = 1024
    private val pcmFrameSize = samplesPerFrame * channelCount * 2 // 4096 bytes

    private val pcmBuffer = FrameAlignedRingBuffer(
        frameSize = pcmFrameSize,
        capacity = pcmFrameSize * 50
    )

    private val encoder: MediaCodec

    private val _aacFlow = MutableSharedFlow<AudioPacket>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val aacFlow = _aacFlow.asSharedFlow()

    private var pts = 0L
    private val frameDurationUs = 1024_000_000L / sampleRate

    private var currentSequence = 0

    // 可选：给 decoder 用
    var csdData: ByteArray? = null
        private set

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

    /**
     * 输入任意大小 PCM（比如 AudioRecord 回调）
     */
    fun encode(pcm: AudioFrame) {

        pcmBuffer.write(pcm.data)

        val frame = ByteArray(pcmFrameSize)

        while (pcmBuffer.readFrame(frame)) {
            val trace = pcm.trace?.copy()
            encodeFrame(AudioFrame(frame,trace ))
        }
    }

    private fun encodeFrame(frame: AudioFrame) {

        val inputIndex = encoder.dequeueInputBuffer(10000)
        if (inputIndex < 0) return

        val inputBuffer = encoder.getInputBuffer(inputIndex)!!
        inputBuffer.clear()
        inputBuffer.put(frame.data)

        encoder.queueInputBuffer(
            inputIndex,
            0,
            frame.data.size,
            pts,
            0
        )

        drain(frame.trace)
        pts += frameDurationUs
    }

    private fun drain(audioTrace: AudioTrace?) {

        val info = MediaCodec.BufferInfo()

        while (true) {
            val index = encoder.dequeueOutputBuffer(info, 0)

            when {
                index >= 0 -> {
                    val buffer = encoder.getOutputBuffer(index) ?: return

                    val data = ByteArray(info.size)
                    buffer.position(info.offset)
                    buffer.limit(info.offset + info.size)
                    buffer.get(data)

                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        csdData = data
                    } else if (info.size > 0) {
                        currentSequence++
                        val audioPacket = AudioPacket(
                            payload = data,
                            seq = currentSequence,
                            timestamp = info.presentationTimeUs,
                            trace = audioTrace.also {
                                it?.seq = currentSequence
                                it?.encodeDoneTime =
                                    SystemClock.elapsedRealtime()
                            })
                        _aacFlow.tryEmit(audioPacket)
                    }

                    encoder.releaseOutputBuffer(index, false)
                }

                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val newFormat = encoder.outputFormat
                    println("Encoder format changed: $newFormat")
                }

                index == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    break
                }
            }
        }
    }

    fun release() {
        try {
            encoder.stop()
        } catch (_: Exception) {
        }
        encoder.release()
    }
}
