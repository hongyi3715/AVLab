package com.lq.video.decode

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface

class H264Decoder {

    private var codec: MediaCodec? = null

    fun start(outputSurface: Surface, width: Int, height: Int) {
        val format = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            width,
            height
        )

        codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            configure(format, outputSurface, null, 0)
            start()
        }
    }

    fun decode(data: ByteArray, timestampUs: Long) {
        val c = codec ?: return

        val inputIndex = c.dequeueInputBuffer(10_000)
        if (inputIndex >= 0) {
            val inputBuffer = c.getInputBuffer(inputIndex) ?: return
            inputBuffer.clear()
            inputBuffer.put(data)

            c.queueInputBuffer(
                inputIndex,
                0,
                data.size,
                timestampUs,
                0
            )
        }

        val info = MediaCodec.BufferInfo()
        while (true) {
            val outputIndex = c.dequeueOutputBuffer(info, 0)
            if (outputIndex >= 0) {
                // 重点：true 表示把这一帧渲染到 configure 时传入的 Surface
                c.releaseOutputBuffer(outputIndex, true)
            } else {
                break
            }
        }
    }

    fun stop() {
        codec?.stop()
        codec?.release()
        codec = null
    }
}
