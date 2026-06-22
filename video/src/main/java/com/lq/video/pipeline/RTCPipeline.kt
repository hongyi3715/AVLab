package com.lq.video.pipeline

import com.lq.video.decode.EncodedVideoFrame
import com.lq.video.decode.VideoPacket
import com.lq.video.decode.VideoPacketHeader
import com.lq.video.encode.EncoderEvent
import com.lq.video.net.VideoUdpSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/*
* 录制编码后生成的h64发送到服务端
* */
class RTCPipeline : VideoBaseEncoderPipeline(){

    fun startSend(scope: CoroutineScope) = scope.launch(Dispatchers.IO) {
        resetSendState()
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

    private fun resetSendState() {
        videoFrameId = 0
        videoSeq = 0
        lastCodecConfig = null
        VideoUdpSocket.resetTimestampBase()
    }

    private fun nextVideoSeq(): Int = ++videoSeq
    private fun nextFrameId(): Int = ++videoFrameId
    private fun handleVideoFrame(frame: EncodedVideoFrame){
        if (frame.isConfig) {
            lastCodecConfig = frame.data
            return
        }

        val prefix = if (frame.isKeyFrame) lastCodecConfig else null
        sendAsPackets(frame.data, frame.flags, frame.ptsUs, prefix)
    }

    private  val MAX_PACKET_SIZE = 1200

    private fun sendAsPackets(
        byteArray: ByteArray,
        flags: Int,
        ptsUs: Long = 0L,
        prefix: ByteArray? = null
    ) {
        val frameId = nextFrameId()
        val prefixPacketCount = prefix?.let { packetCount(it.size) } ?: 0
        val payloadPacketCount = packetCount(byteArray.size)
        val packetCount = prefixPacketCount + payloadPacketCount

        prefix?.let {
            for (packetIndex in 0 until prefixPacketCount) {
                sendVideoSlice(it, packetIndex, packetIndex, packetCount, frameId, flags, ptsUs)
            }
        }

        for (packetIndex in 0 until payloadPacketCount) {
            sendVideoSlice(
                data = byteArray,
                sourcePacketIndex = packetIndex,
                packetIndex = prefixPacketCount + packetIndex,
                packetCount = packetCount,
                frameId = frameId,
                flags = flags,
                ptsUs = ptsUs
            )
        }
    }

    private fun sendVideoSlice(
        data: ByteArray,
        sourcePacketIndex: Int,
        packetIndex: Int,
        packetCount: Int,
        frameId: Int,
        flags: Int,
        ptsUs: Long
    ) {
        val start = sourcePacketIndex * MAX_PACKET_SIZE
        val end = minOf(start + MAX_PACKET_SIZE, data.size)
        sendVideoUnit(
            data = data,
            offset = start,
            size = end - start,
            ptsUs = ptsUs,
            frameId = frameId,
            flags = flags,
            packetIndex = packetIndex,
            packetCount = packetCount
        )
    }

    private fun packetCount(size: Int): Int = (size + MAX_PACKET_SIZE - 1) / MAX_PACKET_SIZE

    private fun sendVideoUnit(
        data: ByteArray,
        offset: Int,
        size: Int,
        ptsUs: Long,
        frameId: Int,
        flags: Int,
        packetIndex: Int,
        packetCount: Int
    ) {
        val seq = nextVideoSeq()
        val videoPacket = VideoPacket(
            VideoPacketHeader(seq, ptsUs, frameId, packetIndex, packetCount, flags),
            payload = data,
            payloadOffset = offset,
            payloadSize = size
        )
        VideoUdpSocket.sendVideoPacket(videoPacket)
    }


}
