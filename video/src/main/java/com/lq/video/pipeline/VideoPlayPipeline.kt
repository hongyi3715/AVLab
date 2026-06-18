package com.lq.video.pipeline

import android.view.Surface
import com.lq.common.MediaClock
import com.lq.video.decode.VideoDecoder
import com.lq.video.net.VideoUdpSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

class VideoPlayPipeline {

    private val decoder = VideoDecoder()

    private var packetBuffer = PacketBuffer()

    fun startDecode(surface: Surface, width: Int, height: Int) {
        decoder.start(surface, width, height)
    }

    fun initBufferListener(scope: CoroutineScope, clock: MediaClock) {
        scope.launch {
            packetBuffer.packetChannel.consumeEach {
                decoder.decode(it.pts, it.bytes)
            }
        }
    }

    fun initNetReceiver(scope: CoroutineScope) = scope.launch(Dispatchers.IO) {
        VideoUdpSocket.startReceivePacket { packet ->
            packetBuffer.handlePacket(packet)
        }
    }

    fun stop() {
        VideoUdpSocket.stopReceivePacket()
    }

}
