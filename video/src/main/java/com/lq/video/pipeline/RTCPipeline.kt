package com.lq.video.pipeline

import com.lq.video.decode.EncodedVideoFrame
import com.lq.video.encode.EncoderEvent
import com.lq.video.net.VideoUdpSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer


/*
* 录制编码后生成的h64发送到服务端
* */
class RTCPipeline : VideoBaseEncoderPipeline(){

    fun startSend(scope: CoroutineScope) = scope.launch(Dispatchers.IO) {
        try {
            eventFlow.collect {
                when (it) {
                    is EncoderEvent.VideoFrame -> {
                        handleVideoFrame(it.frame)
                    }

                    else -> Unit
                }
            }
        }catch (e: Throwable){
            println("StartSendError:${e.message}")
            e.printStackTrace()
        }

    }


    private var videoFrameId = 0
    private var videoSeq = 0
    private var lastCodecConfig: ByteArray? = null

    private fun nextVideoSeq(): Int = ++videoSeq
    private fun nextFrameId(): Int = ++videoFrameId
    private fun handleVideoFrame(frame: EncodedVideoFrame){
        if (frame.isConfig) {
            lastCodecConfig = frame.data
            return
        }

        val payload = if (frame.isKeyFrame) {
            lastCodecConfig?.let { config ->
                config + frame.data
            } ?: frame.data
        } else {
            frame.data
        }
        sendAsPackets(payload, frame.flags, frame.ptsUs)
    }

    private  val MAX_PACKET_SIZE = 1200

    private fun sendAsPackets(
        byteArray: ByteArray,
        flags: Int,
        ptsUs: Long = 0L
    ) {
        val frameId = nextFrameId()
        val packetCount = (byteArray.size + MAX_PACKET_SIZE - 1) / MAX_PACKET_SIZE

        for (packetIndex in 0 until packetCount) {
            val start = packetIndex * MAX_PACKET_SIZE
            val end = minOf(start + MAX_PACKET_SIZE, byteArray.size)
            val chunk = byteArray.copyOfRange(start, end)

            sendVideoUnit(
                data = chunk,
                ptsUs = ptsUs,
                frameId = frameId,
                flags = flags,
                packetIndex = packetIndex,
                packetCount = packetCount
            )
        }
    }

    private fun sendVideoUnit(
        data: ByteArray,
        ptsUs: Long,
        frameId: Int,
        flags: Int,
        packetIndex: Int,
        packetCount: Int
    ) {
        val seq = nextVideoSeq()

        val buffer = ByteBuffer.allocate(
            4 + 4 + 4 + 4 + 8 + 4 + data.size
        )

        buffer.putInt(seq)
        buffer.putInt(frameId)
        buffer.putInt(packetIndex)
        buffer.putInt(packetCount)
        buffer.putLong(ptsUs)
        buffer.putInt(flags)
        buffer.put(data)

        val packetBytes = buffer.array()
        VideoUdpSocket.sendVideoPacket(packetBytes)
    }


}
