package com.lq.video.decode

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import com.lq.video.pipeline.VideoScheduler
import kotlinx.coroutines.Job

class H264Decoder {

    private var codec: MediaCodec? = null

    private var renderJob: Job? = null

    fun start(outputSurface: Surface, width: Int, height: Int) {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            configure(format, outputSurface, null, 0)
            start()
        }
    }

     fun decode(packet: VideoScheduler.DecodePacket) {
        val c = codec ?: return
        println("当前解码:$packet")
        val index = c.dequeueInputBuffer(-1)
        val buffer = c.getInputBuffer(index) ?: return

        buffer.clear()
        buffer.put(packet.bytes)

        c.queueInputBuffer(
            index,
            0,
            packet.bytes.size,
            packet.pts,
            0
        )
    }



    fun stop() {
        renderJob?.cancel()
        renderJob = null
        codec?.stop()
        codec?.release()
        codec = null
    }
}
