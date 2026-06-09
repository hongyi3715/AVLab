package com.lq.video.pipeline

import android.view.Surface
import com.lq.common.MediaClock
import com.lq.video.decode.H264Decoder
import com.lq.video.net.VideoUdpSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoPlayPipeline {

    private val decoder = H264Decoder()

    private val jitterBuffer: VideoJitterBuffer = VideoJitterBuffer()

    fun startDecode(surface: Surface,width:Int,height:Int){
        decoder.start(surface,width,height)
    }

    fun initBufferListener(scope: CoroutineScope,clock: MediaClock){
        jitterBuffer.initListener(scope,clock){packet->
            decoder.decode(packet.bytes,packet.pts)
        }
    }

    fun initNetReceiver(scope: CoroutineScope) = scope.launch(Dispatchers.IO) {
        VideoUdpSocket.startReceivePacket { packet ->
            jitterBuffer.handlePacket(packet)
        }
    }

    fun stop(){
        VideoUdpSocket.stopReceivePacket()
    }

}
