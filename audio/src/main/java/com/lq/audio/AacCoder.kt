package com.lq.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AacCoder {

    private val encoder = createEncoder()
    private val sampleRate = 64000
    private val channelCount = 1

    private val decoder = createDecoder()

    private val _encodeFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val encodeFlow = _encodeFlow.asSharedFlow()

    private val _decodeFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val decodeFlow = _decodeFlow.asSharedFlow()

    private fun createEncoder(): MediaCodec {
        val encodeFormat =
            MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount)
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, sampleRate)
        encodeFormat.setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )
        return MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
            configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            start()
        }
    }

    /*
    * 编码*/
    fun encode(pcm: ByteArray, length: Int = pcm.size) {
        val enc = encoder
        val inputIndex = enc.dequeueInputBuffer(1000 * 10)
        val pts = System.nanoTime() / 1000

        if (inputIndex >= 0) {
            enc.getInputBuffer(inputIndex)?.apply {
                clear()
                val remaining = remaining()
                val writeSize = minOf(remaining,length)
                if(writeSize<length){
                    Log.w("AacEncoder","缓冲区太小")
                }
                put(pcm, 0, writeSize)
                enc.queueInputBuffer(inputIndex, 0, writeSize, pts, 0)
            }
        }


        val bufferInfo = MediaCodec.BufferInfo()
        val outputIndex = enc.dequeueOutputBuffer(bufferInfo, 0)
        when {
            outputIndex >= 0 -> {
                val outputBuffer = enc.getOutputBuffer(outputIndex)

                if (bufferInfo.size > 0 && outputBuffer != null) {
                    val aacData = ByteArray(bufferInfo.size)

                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    outputBuffer.get(aacData)
                    Log.i("AacEncoder","AacSize:${aacData.size}")
                    _encodeFlow.tryEmit(aacData)
                }
            }

            outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                val newFormat = enc.outputFormat
                // 这里可以拿到 csd-0 (AudioSpecificConfig) 用来获取当前编码格式的采样率 声道数等信息
            }

            else -> {
            }
        }
    }


    private fun createDecoder(): MediaCodec {
        val decodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_IS_ADTS, 1) // 如果是带 ADTS 头的 AAC
                setInteger(MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            }
        return MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
            configure(decodeFormat, null, null, 0)
            start()
        }
    }

    fun decode(aacData: ByteArray,pts:Long) {
        val inputIndex = decoder.dequeueInputBuffer(1000 * 10)
        if (inputIndex >= 0) {
            val inputBuffer = decoder.getInputBuffer(inputIndex)!!
            inputBuffer.clear()
            inputBuffer.put(aacData)
            decoder.queueInputBuffer(
                inputIndex,
                0,
                aacData.size,
                pts,
                0
            )
        }

        val bufferInfo = MediaCodec.BufferInfo()
        var outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 0)

        while (outputIndex >= 0) {

            val outputBuffer = decoder.getOutputBuffer(outputIndex)!!
            val pcmData = ByteArray(bufferInfo.size)
            outputBuffer.get(pcmData)


            decoder.releaseOutputBuffer(outputIndex, false)
            outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 0)
            _decodeFlow.tryEmit(pcmData)

        }
    }

    fun release(){

    }
}
