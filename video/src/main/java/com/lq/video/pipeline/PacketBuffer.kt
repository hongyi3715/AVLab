package com.lq.video.pipeline

import android.media.MediaCodec
import com.lq.video.decode.FrameBuffer
import com.lq.video.decode.VideoPacket
import kotlinx.coroutines.channels.Channel



class PacketBuffer {

    private val frameMap = mutableMapOf<Int, FrameBuffer>()
    private val readyFrames = sortedMapOf<Int, DecodePacket>()
    private var nextDecodeFrameId = -1

    private var canDecode = false
    var totalFrames = 0
    var droppedFrames = 0

     val packetChannel = Channel<DecodePacket>(16)


    fun handlePacket(packet: VideoPacket) {
        val frameId = packet.header.frameId
        val fragIndex = packet.header.fragIndex
        val fragCount = packet.header.fragCount

        if (fragCount <= 0 || fragIndex !in 0 until fragCount) {
            println("非法视频分片 frame=$frameId index=$fragIndex count=$fragCount")
            return
        }

        val buffer = frameMap.getOrPut(frameId) {
            FrameBuffer(fragCount)
        }

        buffer.add(fragIndex, packet.payload)

        if (buffer.isComplete()) {
            totalFrames++
            val fullFrame = buffer.merge()
            frameMap.remove(frameId)

            val isKeyFrame = isKeyFrame(packet, fullFrame)
            if (isKeyFrame) {
                println("收到关键帧 frame=$frameId，允许解码 | 总帧:$totalFrames 丢帧:$droppedFrames")
            }

            readyFrames[frameId] = DecodePacket(
                bytes = fullFrame,
                pts = packet.header.timestampUs,
                isKeyFrame = isKeyFrame
            )

            drainReadyFrames()
            cleanupOldFrames(frameId)
        }
    }

    private fun drainReadyFrames() {
        while (readyFrames.isNotEmpty()) {
            val firstFrameId = readyFrames.firstKey()
            val firstPacket = readyFrames[firstFrameId] ?: return

            if (!canDecode) {
                if (!firstPacket.isKeyFrame) {
                    readyFrames.remove(firstFrameId)
                    nextDecodeFrameId = firstFrameId + 1
                    println("跳过非关键帧 frame=$firstFrameId")
                    continue
                }

                canDecode = true
                nextDecodeFrameId = firstFrameId
            }

            if (nextDecodeFrameId < 0) {
                nextDecodeFrameId = firstFrameId
            }

            if (firstFrameId < nextDecodeFrameId) {
                readyFrames.remove(firstFrameId)
                continue
            }

            if (firstFrameId > nextDecodeFrameId) {
                if (readyFrames.size < GAP_WAIT_READY_FRAMES) {
                    return
                }

                val lostCount = firstFrameId - nextDecodeFrameId
                droppedFrames += lostCount
                println("丢完整帧 expected=$nextDecodeFrameId current=$firstFrameId，暂停解码等待关键帧 | 丢帧:$droppedFrames")
                canDecode = false
                continue
            }

            readyFrames.remove(firstFrameId)
            val result = packetChannel.trySend(firstPacket)
            if (result.isFailure) {
                droppedFrames++
                println("JitterBuffer Channel满，丢帧 frame=$firstFrameId")
            }
            nextDecodeFrameId = firstFrameId + 1
        }
    }

    private fun cleanupOldFrames(currentFrameId: Int) {
        frameMap.entries.removeIf { it.key < currentFrameId - MAX_BUFFERED_FRAMES }
        readyFrames.keys.removeIf { it < currentFrameId - MAX_BUFFERED_FRAMES }
    }

    private fun isKeyFrame(packet: VideoPacket, data: ByteArray): Boolean {
        return (packet.header.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0 || isIdrFrame(data)
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
    }

    data class DecodePacket(
        val bytes: ByteArray,
        val pts: Long,
        val isKeyFrame: Boolean = false
    )

    companion object {
        private const val MAX_BUFFERED_FRAMES = 30
        private const val GAP_WAIT_READY_FRAMES = 3
    }

}
