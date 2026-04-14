package com.lq.video

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface

class Camera264Encoder {


    var encodeSurface: Surface? = null
        private set

    private var codec: MediaCodec? = null
    private var outputThread: Thread? = null

    var format: MediaFormat? = null
        private set

    @Volatile
    private var isRunning = false

    fun createEncoder(width: Int, height: Int, bitrate: Int) {
        codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val videoFormat = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            width, height
        ).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)      // 码率，如 2000000
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)         // 帧率
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)    // I帧间隔
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
        }

        codec?.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        encodeSurface = codec?.createInputSurface() // 这个Surface要被OpenGL用来绘制
        codec?.start()
    }

    interface AudioBytesMediaCodeCallback {
        fun onEncodedData(data: ByteArray, info: MediaCodec.BufferInfo)
    }


    /*
    * 开启一个线程读取编码数据
    * */
    fun startOutputThread(callback: AudioBytesMediaCodeCallback) {
        isRunning = true
        outputThread = Thread {
            val bufferInfo = MediaCodec.BufferInfo()

            while (isRunning) {
                while (true) {
                    val outputIndex = codec?.dequeueOutputBuffer(bufferInfo, 1000) ?: break

                    if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    }

                    if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        format = codec?.outputFormat
                    } else if (outputIndex >= 0) {
                        val outputBuffer = codec?.getOutputBuffer(outputIndex) ?: continue

                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            codec?.releaseOutputBuffer(outputIndex, false)
                            continue
                        }

                        val data = ByteArray(bufferInfo.size)
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        outputBuffer.get(data)

                        callback.onEncodedData(data, bufferInfo)

                        codec?.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
        }.also { it.start() }
    }

    fun stop() {
        isRunning = false
        outputThread?.join()
        outputThread = null

        codec?.stop()
        codec?.release()
        codec = null

        encodeSurface?.release()
        encodeSurface = null
    }


}
