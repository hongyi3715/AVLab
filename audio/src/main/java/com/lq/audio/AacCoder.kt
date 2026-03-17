package com.lq.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import com.lq.audio.buffer.FrameAlignedRingBuffer
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.nio.ByteBuffer

class AacCoder {

    // 音频参数
    private val sampleRate = 44100  // 标准采样率：44100Hz
    private val channelCount = 2     // 立体声
    private val bitRate = 128000     // 比特率：128kbps
    private val pcmBitDepth = 16     // 16-bit PCM

    // AAC编码参数（固定值）
    private val aacFrameSamples = 1024  // AAC每帧采样数（固定）
    private val aacProfile = MediaCodecInfo.CodecProfileLevel.AACObjectLC  // LC profile

    // 计算PCM帧大小
    private val pcmFrameSize = aacFrameSamples * channelCount * (pcmBitDepth / 8)
    // 1024 * 2 * 2 = 4096 字节/帧

    // 缓冲区
    private val pcmBuffer = FrameAlignedRingBuffer(
        frameSize = pcmFrameSize,
        capacity = pcmFrameSize * 50  // 50帧缓冲区
    )

    private val aacBuffer = FrameAlignedRingBuffer(
        frameSize = 1024,  // AAC帧通常几百字节，用1024作为缓冲区
        capacity = 1024 * 50
    )

    // 编码器
    private val encoder: MediaCodec = createEncoder()

    // 输出Flow
    private val _aacFlow = MutableSharedFlow<ByteArray>(
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    val aacFlow = _aacFlow.asSharedFlow()

    // 统计信息
    private var totalPcmBytes = 0L
    private var totalAacBytes = 0L
    private var encodedFrames = 0L

    private fun createEncoder(): MediaCodec {
        // 1. 创建音频格式
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            sampleRate,
            channelCount
        )

        // 2. 设置关键参数
        format.apply {
            // 比特率（决定压缩比）
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)

            // AAC Profile
            setInteger(MediaFormat.KEY_AAC_PROFILE, aacProfile)

            // 最大输入大小（重要！）
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, pcmFrameSize * 2)

            // 采样率（已经设置过，但可以再确认）
            setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate)

            // 声道数
            setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount)


        }

        // 3. 创建并配置编码器
        return MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            start()

            // 获取编码器信息
            val codecInfo = codecInfo
            Log.i("AacEncoder", """
                编码器初始化:
                名称: ${codecInfo.name}
                采样率: $sampleRate Hz
                声道数: $channelCount
                比特率: ${bitRate / 1000} kbps
                PCM帧大小: $pcmFrameSize 字节
            """.trimIndent())
        }
    }

    /**
     * 编码PCM数据
     */
    fun encode(pcmData: ByteArray, offset: Int = 0, length: Int = pcmData.size) {
        // 1. 写入缓冲区
        pcmBuffer.write(pcmData, offset, length)
        totalPcmBytes += length

        // 2. 处理完整的PCM帧
        val pcmFrame = ByteArray(pcmFrameSize)
        while (pcmBuffer.readFrame(pcmFrame)) {
            encodeFrame(pcmFrame)
        }
    }

    /**
     * 编码单帧PCM
     */
    private fun encodeFrame(pcmFrame: ByteArray) {
        try {
            // 1. 获取输入缓冲区
            val inputIndex = encoder.dequeueInputBuffer(10000)  // 10ms超时
            if (inputIndex < 0) {
                Log.w("AacEncoder", "无法获取输入缓冲区")
                return
            }

            val inputBuffer = encoder.getInputBuffer(inputIndex) ?: return
            inputBuffer.clear()

            // 2. 写入PCM数据
            inputBuffer.put(pcmFrame)

            // 3. 提交给编码器
            val presentationTimeUs = System.nanoTime() / 1000
            encoder.queueInputBuffer(
                inputIndex,
                0,
                pcmFrame.size,
                presentationTimeUs,
                0
            )

            // 4. 获取编码输出
            drainEncoder()

        } catch (e: Exception) {
            Log.e("AacEncoder", "编码帧错误", e)
        }
    }

    /**
     * 从编码器取出所有输出
     */
    private fun drainEncoder() {
        val bufferInfo = MediaCodec.BufferInfo()
        var outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)

        while (outputIndex >= 0) {
            val outputBuffer = encoder.getOutputBuffer(outputIndex) ?: continue

            if (bufferInfo.size > 0) {
                // 读取AAC数据
                val aacData = ByteArray(bufferInfo.size)
                outputBuffer.position(bufferInfo.offset)
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                outputBuffer.get(aacData)

                // 更新统计
                totalAacBytes += aacData.size
                encodedFrames++

                // 计算压缩比
                val compressionRatio = pcmFrameSize.toDouble() / aacData.size

                Log.i("AacEncoder", """
                    编码成功:
                    AAC大小: ${aacData.size} 字节
                    压缩比: ${String.format("%.2f", compressionRatio)} : 1
                    总PCM: ${totalPcmBytes / 1024} KB
                    总AAC: ${totalAacBytes / 1024} KB
                    帧数: $encodedFrames
                """.trimIndent())


                // 发送到Flow
                _aacFlow.tryEmit(aacData)

                // 写入AAC缓冲区（供解码使用）
                aacBuffer.write(aacData)
            }

            // 释放输出缓冲区
            encoder.releaseOutputBuffer(outputIndex, false)

            // 继续获取下一个输出
            outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
        }

        // 检查输出格式变化
        val outputFormat = encoder.outputFormat
        if (outputFormat != null && bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
            Log.i("AacEncoder", "编码器格式: $outputFormat")
        }
    }

    /**
     * 获取编码统计
     */
    fun getStats(): String = """
        AAC编码器统计:
        采样率: $sampleRate Hz
        声道: $channelCount
        比特率: ${bitRate / 1000} kbps
        PCM帧大小: $pcmFrameSize 字节
        编码帧数: $encodedFrames
        总PCM: ${totalPcmBytes / 1024} KB
        总AAC: ${totalAacBytes / 1024} KB
        平均压缩比: ${String.format("%.2f",
        if (totalAacBytes > 0) totalPcmBytes.toDouble() / totalAacBytes else 0.0)} : 1
    """.trimIndent()

    /**
     * 释放资源
     */
    fun release() {
        try {
            // 发送结束标志
            val inputIndex = encoder.dequeueInputBuffer(10000)
            if (inputIndex >= 0) {
                encoder.queueInputBuffer(
                    inputIndex,
                    0,
                    0,
                    System.nanoTime() / 1000,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }

            // 清空输出
            drainEncoder()

            encoder.stop()
            encoder.release()
        } catch (e: Exception) {
            Log.e("AacEncoder", "释放错误", e)
        }
    }
}
