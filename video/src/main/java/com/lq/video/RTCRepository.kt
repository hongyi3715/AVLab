package com.lq.video

import com.lq.video.pipeline.RTCPipeline
import com.lq.video.pipeline.VideoPlayPipeline
import kotlinx.coroutines.CoroutineScope

class RTCRepository {

    val recordPipeline = RTCPipeline()

    val playPipeline = VideoPlayPipeline()



    fun startRecord(scope: CoroutineScope){
        recordPipeline.startSend(scope) //编码成功后发送
        recordPipeline.startEncode() //开始编码
    }

    fun stopRecord(){
        recordPipeline.stopEncode()
    }

}
