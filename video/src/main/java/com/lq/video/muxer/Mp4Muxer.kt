package com.lq.video.muxer

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

class Mp4Muxer(val mp4Path: String) {

    private var muxer: MediaMuxer? = null
    private val trackIndices = mutableMapOf<String, Int>()  // 存储轨道名称对应的索引
    private var muxerStarted = false

    // 轨道类型常量
    companion object {
        const val TRACK_VIDEO = "video"
        const val TRACK_AUDIO = "audio"
    }

    fun init() {
        try {
            // 确保目录存在
            val file = File(mp4Path)
            file.parentFile?.mkdirs()

            muxer = MediaMuxer(
                mp4Path,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Failed to initialize MediaMuxer: ${e.message}")
        }
    }


    fun addVideoTrack(format: MediaFormat): Int{
        return addTrack(TRACK_VIDEO,format)
    }

    fun addAudioTrack(format: MediaFormat):Int{
        return addTrack(TRACK_AUDIO,format)
    }


    /**
     * 添加轨道
     * @param trackType 轨道类型 (TRACK_VIDEO 或 TRACK_AUDIO)
     * @param format MediaFormat 格式
     * @return 轨道索引，-1 表示失败
     */
    private fun addTrack(trackType: String, format: MediaFormat): Int {
        if (muxerStarted) {
            throw IllegalStateException("Cannot add track after muxer started")
        }

        return try {
            val trackIndex = muxer?.addTrack(format) ?: -1
            if (trackIndex >= 0) {
                trackIndices[trackType] = trackIndex
            }
            trackIndex
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    /**
     * 获取轨道索引
     */
    fun getTrackIndex(trackType: String): Int {
        return trackIndices[trackType] ?: -1
    }

    /**
     * 写入样本数据
     * @param trackType 轨道类型
     * @param byteBuffer 数据缓冲区
     * @param bufferInfo 缓冲区信息（包含 offset, size, presentationTimeUs, flags）
     */
    private fun writeSampleData(trackType: String, byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (!muxerStarted) {
            throw IllegalStateException("Muxer not started. Call start() first.")
        }

        val trackIndex = trackIndices[trackType]
            ?: throw IllegalArgumentException("Track type '$trackType' not found")

        try {
            muxer?.writeSampleData(trackIndex, byteBuffer, bufferInfo)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 便捷方法：写入视频数据
     */
    fun writeVideoData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        writeSampleData(TRACK_VIDEO, byteBuffer, bufferInfo)
    }

    /**
     * 便捷方法：写入音频数据
     */
    fun writeAudioData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        writeSampleData(TRACK_AUDIO, byteBuffer, bufferInfo)
    }

    fun start() {
        if (muxerStarted) {
            return
        }
        if (trackIndices.isEmpty()) {
            throw IllegalStateException("No tracks added. Call addTrack() first.")
        }

        try {
            muxer?.start()
            muxerStarted = true
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Failed to start muxer: ${e.message}")
        }
    }

    fun stop() {
        if (muxerStarted) {
            try {
                muxer?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                release()
            }
        } else {
            release()
        }
    }

    private fun release() {
        try {
            muxer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            muxer = null
            muxerStarted = false
            trackIndices.clear()
        }
    }

    /**
     * 检查 muxer 是否已启动
     */
    fun isStarted(): Boolean = muxerStarted

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = muxer != null
}
