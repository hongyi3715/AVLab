package com.lq.audio

import com.lq.audio.coder.AacEncoder
import com.lq.audio.data.AudioFrame
import com.lq.audio.net.AudioUdpSocket
import com.lq.audio.record.AudioRecordManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class AudioRecordPipeline {

    private val audioRecordManager = AudioRecordManager

    val recordState = audioRecordManager.recordStateFlow

    private val encoder: AacEncoder = AacEncoder() //aac编码

    val encodeAudioFlow = encoder.aacFlow

    val audioEventFlow = encoder.audioEncodeEventFlow

    //录音的音频去编码
     fun initRecordFlow(scope: CoroutineScope)= scope.launch(Dispatchers.IO){
        audioRecordManager.audioFlow.collect { pcm->
            encode(pcm)
        }
    }

    //编码完成发送到服务端
     fun initEncoder(scope: CoroutineScope)= scope.launch(Dispatchers.IO){
        encodeAudioFlow.collect {
            AudioUdpSocket.sendAudioPacket(it)
        }
    }

    //开始录音
    fun startRecord(){
        audioRecordManager.startRecord()
    }

    suspend fun stopRecord(){
        audioRecordManager.stopRecord()
    }

    //开始编码
    fun  encode(pcm: AudioFrame) = encoder.encode(pcm)




}
