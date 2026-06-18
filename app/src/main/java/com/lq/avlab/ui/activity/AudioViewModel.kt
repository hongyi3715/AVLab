package com.lq.avlab.ui.activity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lq.audio.record.RecordState
import com.lq.avlab.AudioRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(private val repository: AudioRecordRepository) :
    ViewModel() {

    private var isRecording: Boolean = false

    var recordState: RecordState = RecordState.Idle


    init {
        printAllIPs()
        initAll()
    }

    private fun initAll() = viewModelScope.launch(Dispatchers.IO){
        launch { //监听模拟的udp数据接受
            repository.initPlayPipeline(viewModelScope)
        }

        launch {//监听解码音频
            repository.handleDecodeAudio()
        }

        launch { //监听播放状态
/*            repository.playState.collect {
                println("播放状态:$it")
            }*/
        }
        launch { //监听编码音频
//            repository.initEncoder(viewModelScope)
        }
        launch { //监听录音音频
            repository.initRecordFlow(viewModelScope)
        }
        launch {
            repository.recordState.collect {
                Log.d("录音状态", "State: $it")
                recordState = it
                isRecording = it == RecordState.Recording
            }
        }
    }

    fun toggleRecord() {

        if (isRecording) {
            stopRecord()
        } else {
            startRecord()
        }
    }

    private fun startRecord() = viewModelScope.launch(Dispatchers.IO) {
        launch {
            repository.trackPlay()
        }
        launch {
            repository.startToRecord()
        }
    }

    private fun stopRecord() = viewModelScope.launch(Dispatchers.IO) {
        launch {
            repository.stopTrack()
        }
        launch {
            repository.stopRecord()
        }
    }


    override fun onCleared() {
        super.onCleared()
        stopRecord()
    }


    fun printAllIPs() {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses

                println("接口: ${networkInterface.name} (${if (networkInterface.isUp) "UP" else "DOWN"})")

                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address) {
                        println("  IPv4: ${addr.hostAddress}")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
