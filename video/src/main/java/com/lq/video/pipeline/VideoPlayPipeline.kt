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
    private var lastCompleteFrameId = -1

    private var canDecode = false

    fun initNetReceiver(scope: CoroutineScope) = scope.launch(Dispatchers.IO) {
        var totalFrames = 0
        var droppedFrames = 0

        VideoUdpSocket.startReceivePacket { packet ->
            val frameId = packet.header.frameId

            val buffer = frameMap.getOrPut(frameId) {
                FrameBuffer(packet.header.fragCount)
            }

            buffer.add(packet.header.fragIndex, packet.payload)

            if (buffer.isComplete()) {
                totalFrames++
                val fullFrame = buffer.merge()
                frameMap.remove(frameId)

                val nalTypes = findNalTypes(fullFrame)
                val isIdr = nalTypes.contains(5)

                if (lastCompleteFrameId >= 0 && frameId != lastCompleteFrameId + 1) {
                    droppedFrames++
                    println("丢完整帧 last=$lastCompleteFrameId current=$frameId，暂停解码等待IDR | 丢包率: ${droppedFrames * 100 / totalFrames}%")
                    canDecode = false
                }

                lastCompleteFrameId = frameId

                if (isIdr) {
                    println("收到IDR frame=$frameId，恢复解码 | 总帧:$totalFrames 丢帧:$droppedFrames")
                    canDecode = true
                }

                if (canDecode) {
                    decoder.decode(fullFrame, packet.header.timestampUs)
                } else {
                    println("跳过非IDR frame=$frameId nalTypes=$nalTypes")
                }
            }
        }
    }

    private fun findNalTypes(data: ByteArray): List<Int> {
        val result = mutableListOf<Int>()
        var i = 0

        while (i < data.size - 5) {
            val startCodeSize = when {
                data[i] == 0.toByte() &&
                        data[i + 1] == 0.toByte() &&
                        data[i + 2] == 1.toByte() -> 3

                data[i] == 0.toByte() &&
                        data[i + 1] == 0.toByte() &&
                        data[i + 2] == 0.toByte() &&
                        data[i + 3] == 1.toByte() -> 4

                else -> {
                    i++
                    continue
                }
            }

            val nalHeader = data[i + startCodeSize].toInt() and 0xFF
            result.add(nalHeader and 0x1F)

            i += startCodeSize + 1
        }

        return result
    }


    fun stop(){
        VideoUdpSocket.stopReceivePacket()
    }

}
