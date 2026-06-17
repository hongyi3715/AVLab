package com.lq.video.pipeline

import android.view.Surface
import com.lq.common.MediaClock
import com.lq.video.decode.H264Decoder
import com.lq.video.net.VideoUdpSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

class VideoPlayPipeline {

    private val decoder = H264Decoder()

    private var jitterBuffer: VideoScheduler?=null

    fun startDecode(surface: Surface, width: Int, height: Int) {
        decoder.start(surface, width, height)
    }

    fun initBufferListener(scope: CoroutineScope, clock: MediaClock) {
        jitterBuffer = VideoScheduler(clock).apply {
            scope.launch(Dispatchers.IO) {
                decodeQueue.consumeEach {
                    decoder.decode(it)
                }
            }
        }
    }

    fun initNetReceiver(scope: CoroutineScope) = scope.launch(Dispatchers.IO) {
        VideoUdpSocket.startReceivePacket { packet ->
            jitterBuffer?.pushFrame(data = packet.payload,pts = packet.header.timestampUs)
        }
    }

    fun stop(){
        VideoUdpSocket.stopReceivePacket()
    }

}
