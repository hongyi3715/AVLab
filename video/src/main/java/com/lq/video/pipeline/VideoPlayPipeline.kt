package com.lq.video.pipeline

import android.view.Surface
import com.lq.video.decode.FrameBuffer
import com.lq.video.decode.H264Decoder
import com.lq.video.decode.VideoPacket
import com.lq.video.net.VideoUdpSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoPlayPipeline {

    private val decoder = H264Decoder()
    private val frameMap = mutableMapOf<Int, FrameBuffer>()


    fun startDecode(surface: Surface,width:Int,height:Int){
        decoder.start(surface,width,height)
    }

    //接受到服务端给的视频数据
    fun initNetReceiver(scope: CoroutineScope)= scope.launch(Dispatchers.IO) {
        VideoUdpSocket.startReceivePacket { packet ->
            val buffer = frameMap.getOrPut(packet.header.frameId) {
                FrameBuffer(packet.header.fragCount)
            }
            buffer.add(packet.header.fragIndex,packet.payload)
            println("frameId=${packet.header.frameId}, isComplete:${buffer.isComplete()} frag=${packet.header.fragIndex}/${packet.header.fragCount - 1}")
            if(buffer.isComplete()){
                val fullFrame = buffer.merge()
                frameMap.remove(packet.header.frameId)
                val videoPacket = VideoPacket(packet.header,fullFrame)
                //这是完整的一帧，送到解码器去
                decoder.decode(videoPacket.payload,videoPacket.header.timestampUs)
            }
        }
    }


    fun stop(){
        VideoUdpSocket.stopReceivePacket()
    }

}
