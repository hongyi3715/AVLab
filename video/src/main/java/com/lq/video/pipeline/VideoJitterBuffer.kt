package com.lq.video.pipeline

import com.lq.video.decode.FrameBuffer
import com.lq.video.decode.VideoPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch



class VideoJitterBuffer {

    private val frameMap = mutableMapOf<Int, FrameBuffer>()
    private var lastCompleteFrameId = -1

    private var canDecode = false
    var totalFrames = 0
    var droppedFrames = 0

    private val packetChannel = Channel<DecodePacket>(16)

    private var job: Job? = null

    fun initListener(
        scope: CoroutineScope,
        decode:suspend (DecodePacket) -> Unit
    ) {
        stop()
        job = scope.launch(Dispatchers.IO) {
            for (packet in packetChannel) {
                decode(packet)
            }
        }
    }

    fun handlePacket(packet: VideoPacket) {
        val frameId = packet.header.frameId

        val buffer = frameMap.getOrPut(frameId) {
            FrameBuffer(packet.header.fragCount)
        }

        buffer.add(packet.header.fragIndex, packet.payload)

        if (buffer.isComplete()) {
            totalFrames++
            val fullFrame = buffer.merge()
            frameMap.remove(frameId)

            val isIdr = isIdrFrame(fullFrame)

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

            //清除过时的数据
            frameMap.entries.removeIf { it.key < frameId - 30 }
            if (canDecode) {
                val result = packetChannel.trySend(DecodePacket(fullFrame, packet.header.timestampUs))
                if (result.isFailure) {
                    println("JitterBuffer Channel满，丢帧 frame=$frameId")
                }
            } else {
                println("跳过非IDR frame=$frameId")
            }
        }
    }

    private fun isIdrFrame(data: ByteArray): Boolean {
        var i = 0
        while (i <= data.size - 4) {
            val startCodeSize = when {
                data[i] == 0.toByte() && data[i + 1] == 0.toByte() && data[i + 2] == 1.toByte() -> 3
                data[i] == 0.toByte() && data[i + 1] == 0.toByte() && data[i + 2] == 0.toByte() && data[i + 3] == 1.toByte() -> 4
                else -> {
                    i++; continue
                }
            }
            if (i + startCodeSize < data.size) {
                val nalType = data[i + startCodeSize].toInt() and 0x1F
                if (nalType == 5) return true  // IDR，直接返回
            }
            i += startCodeSize + 1
        }
        return false
    }


    fun stop() {
        job?.cancel()
//        packetChannel.close()
    }

    data class DecodePacket(
        val bytes: ByteArray,
        val pts: Long
    )

}
