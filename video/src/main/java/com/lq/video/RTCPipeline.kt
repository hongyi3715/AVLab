package com.lq.video

import com.lq.video.decode.EncodedVideoFrame
import com.lq.video.encode.EncoderEvent
import com.lq.video.encode.VideoRecordPipeline
import com.lq.video.net.VideoUdpSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/*
* 录制编码后生成的h64发送到服务端
* */
class RTCPipeline {

    val videoPipeline = VideoRecordPipeline()


    fun startSend(scope: CoroutineScope) = scope.launch(Dispatchers.IO) {
        videoPipeline.eventFlow.collect {
            when (it) {
                is EncoderEvent.VideoFrame -> {
                    handleVideoFrame(it.frame)
                }

                else -> Unit
            }
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

        val frameId = nextFrameId()

        if (frame.isKeyFrame) {
            lastCodecConfig?.let { config -> //发送I帧前优先发送一次config数据
                sendAsPackets(config,frame.flags)
            }
            sendAsPackets(frame.data,frame.flags)
        }else{ //P帧 B帧不做特殊处理
            sendAsPackets(frame.data,frame.flags)
        }
    }

    private fun sendAsPackets(byteArray:ByteArray,flags:Int){

    }

    private fun sendVideoUnit(
        data: ByteArray,
        ptsUs: Long,
        frameId: Int,
        flags: Int
    ) {

    }


}
