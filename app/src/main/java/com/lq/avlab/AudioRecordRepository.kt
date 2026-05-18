package com.lq.avlab

import com.lq.audio.AudioPlayPipeline
import com.lq.audio.AudioRecordPipeline
import kotlinx.coroutines.CoroutineScope


class AudioRecordRepository {
    private val audioRecordPipeline = AudioRecordPipeline()

    val recordState = audioRecordPipeline.recordState

    private val audioPlayPipeline = AudioPlayPipeline()
    val playState = audioPlayPipeline.playState

    //录音音频直接取编码aac
     fun initRecordFlow(scope: CoroutineScope) =  audioRecordPipeline.initRecordFlow(scope)

    //编码aac成功，发送到客户端
    fun initEncoder(scope: CoroutineScope) = audioRecordPipeline.initEncoder(scope)

    fun initPlayPipeline(scope: CoroutineScope){
        audioPlayPipeline.initReceiver(scope) //udp接收
        audioPlayPipeline.initJitterBuffer(scope) //jitterBuffer 缓冲
    }

    //解码成功，直接播放
    suspend fun handleDecodeAudio() = audioPlayPipeline.handleDecodeAudio()

    //录音
    fun startToRecord() =  audioRecordPipeline.startRecord()

    //播放
    suspend fun trackPlay(){
        audioPlayPipeline.play()
    }

    suspend fun stopRecord(){
        audioRecordPipeline.stopRecord()
    }

    suspend fun stopTrack() = audioPlayPipeline.stop()

    suspend fun closeRecord(){
    }

    suspend fun closeTrack(){
        audioPlayPipeline.reset()
    }

}
