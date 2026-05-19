package com.lq.video.decode

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import java.nio.ByteBuffer

class H264Decoder {

    private var codec: MediaCodec? = null

    fun start(surface: Surface, width: Int, height: Int, csd0: ByteArray? = null, csd1: ByteArray? = null) {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        csd0?.let { format.setByteBuffer("csd-0", ByteBuffer.wrap(it)) }
        csd1?.let { format.setByteBuffer("csd-1", ByteBuffer.wrap(it)) }

        codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            configure(format, surface, null, 0)
            start()
        }
    }

    fun decode(data: ByteArray, ptsUs: Long) {
        val codec = codec ?: return

        val inputIndex = codec.dequeueInputBuffer(10000)
        if (inputIndex >= 0) {
            val inputBuffer = codec.getInputBuffer(inputIndex) ?: return
            inputBuffer.clear()
            inputBuffer.put(data)
            codec.queueInputBuffer(inputIndex, 0, data.size, ptsUs, 0)
        }

        val info = MediaCodec.BufferInfo()
        var outputIndex = codec.dequeueOutputBuffer(info, 0)
        while (outputIndex >= 0) {
            codec.releaseOutputBuffer(outputIndex, true)
            outputIndex = codec.dequeueOutputBuffer(info, 0)
        }
    }

    fun stop() {
        codec?.stop()
        codec?.release()
        codec = null
    }
}
