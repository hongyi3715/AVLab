package com.lq.avlab.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lq.avlab.RecordPipeline
import com.lq.avlab.ui.App
import com.lq.video.RTCRepository
import com.lq.video.net.VideoUdpSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(): ViewModel(){

    private val recordPipeline = RecordPipeline()

    val videoRecordPipeline = recordPipeline.video

    val rtcRepository = RTCRepository()

    val encodePipeline = rtcRepository.recordPipeline



    fun setHost(host:String){
        val ip = host.substringBefore(":")
        val port = host.substringAfter(":", "1935").toInt()
        VideoUdpSocket.host = ip
        VideoUdpSocket.sendPort = port
        println("当前接收端ip:$ip")
    }

    fun initReceiver() = rtcRepository.initReceiver(viewModelScope)

    fun startRecord(){
//        val output = File(App.instance.externalCacheDir, "${System.currentTimeMillis()}.mp4")
//        recordPipeline.start(output,viewModelScope)
        rtcRepository.startRecord(viewModelScope)
    }

    fun stopRecord()=viewModelScope.launch(Dispatchers.IO){
        rtcRepository.stopRecord()
//        recordPipeline.stop()
    }

}
