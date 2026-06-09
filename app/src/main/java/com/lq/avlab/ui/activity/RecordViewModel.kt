package com.lq.avlab.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lq.audio.AudioPlayPipeline
import com.lq.audio.AudioRecordPipeline
import com.lq.audio.net.AudioUdpSocket
import com.lq.video.RTCRepository
import com.lq.video.net.VideoUdpSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(): ViewModel(){

    private val audioRecord = AudioRecordPipeline()

    val audioPlayer = AudioPlayPipeline()

    val clock = audioPlayer.audioTrackManager

    val rtcRepository = RTCRepository()

    val encodePipeline = rtcRepository.recordPipeline

    val playPipeline = rtcRepository.playPipeline

    init {
        viewModelScope.launch(Dispatchers.IO) {
            audioRecord.recordState.collect {
                println("当前录音状态:$it")
            }
        }

    }

    fun setHost(host:String){
        val ip = host.substringBefore(":")
        val port = host.substringAfter(":", "1935").toInt()
        VideoUdpSocket.host = ip
        AudioUdpSocket.host = ip
        VideoUdpSocket.sendPort = port
        println("当前接收端ip:$ip")
    }

    fun startAudioPlay(){
        viewModelScope.launch(Dispatchers.IO) {
            audioPlayer.play()
        }
        viewModelScope.launch(Dispatchers.IO) {
            audioPlayer.handleDecodeAudio() //解码成功进行播放
        }
        audioPlayer.initJitterBuffer(viewModelScope) //处理jitterBuffer
        audioPlayer.initReceiver(viewModelScope) //开始接收音频
    }

    fun stopAudioPlay() = viewModelScope.launch(Dispatchers.IO){
        audioPlayer.stop()
    }

    private fun startAudioRecord(){
        audioRecord.initRecordFlow(viewModelScope) //录音音频进行编码
        audioRecord.initEncoder(viewModelScope)
        audioRecord.startRecord()
    }

    private fun stopAudioRecord() = viewModelScope.launch(Dispatchers.IO){
        audioRecord.stopRecord()
    }

    fun startRecord(){
        rtcRepository.startRecord(viewModelScope)
        startAudioRecord()
    }

    fun stopRecord()=viewModelScope.launch(Dispatchers.IO){
        rtcRepository.stopRecord()
        stopAudioRecord()
    }

}
