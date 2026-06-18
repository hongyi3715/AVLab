package com.lq.video.decode

import android.media.Image
import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface

class VideoDecoder {

    private var codec: MediaCodec? = null
    private val bufferInfo = MediaCodec.BufferInfo()

    private var firstPts = 0L

    fun start(outputSurface: Surface, width: Int, height: Int) {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            configure(format, outputSurface, null, 0)
            start()
        }
    }

    fun decode(pts: Long, data: ByteArray) {
        val c = codec ?: return

        val index = c.dequeueInputBuffer(10_000)
        if (index < 0) {
            drainOutput(c)
            return
        }
        if(firstPts ==0L){
            firstPts = pts
        }
        val realPts = pts - firstPts

        val buffer = c.getInputBuffer(index)
        if (buffer == null) {
            c.queueInputBuffer(index, 0, 0, realPts, 0)
            drainOutput(c)
            return
        }

        buffer.clear()
        if (data.size > buffer.capacity()) {
            println("H264Decoder input buffer太小 capacity=${buffer.capacity()} frame=${data.size}")
            c.queueInputBuffer(index, 0, 0, realPts, 0)
            drainOutput(c)
            return
        }
        buffer.put(data)

        c.queueInputBuffer(index, 0, data.size, realPts, 0)
        drainOutput(c)
    }

    private fun drainOutput(c: MediaCodec) {
        while (true) {
            when (val outputIndex = c.dequeueOutputBuffer(bufferInfo, 0)) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> return
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                else -> {
                    if (outputIndex >= 0) {
                        println("Video bufferInfo pts:${bufferInfo.presentationTimeUs}")
                        c.releaseOutputBuffer(outputIndex, true)
                    }
                }
            }
        }
    }

}
