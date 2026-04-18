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

    private val isClient = false

    init {
        printAllIPs()
        initAll()
    }

    private fun initData() {
        viewModelScope.launch(Dispatchers.IO) {
            if (isClient) {
                launch {
                    repository.initEncoder()
                }
                launch {
                    repository.initRecordFlow()
                }
                launch {
                    repository.recordState.collect {
                        Log.d("录音状态", "State: $it")
                        recordState = it
                        isRecording = it == RecordState.Recording
                    }
                }
            } else {
                launch {
                    repository.initReceiver(viewModelScope)
                }

                launch { //持续接收
                    repository.initJitterBuffer()
                }

                launch {//解码
                    repository.handleDecodeAudio()
                }

                launch {
                    repository.playState.collect {
                        println("播放状态:$it")
                    }
                }
            }
        }
    }

    private fun initAll() = viewModelScope.launch(Dispatchers.IO){
        launch {
            repository.initReceiver(viewModelScope)
        }

        launch { //持续接收
            repository.initJitterBuffer()
        }

        launch {//解码
            repository.handleDecodeAudio()
        }

        launch {
            repository.playState.collect {
                println("播放状态:$it")
            }
        }
        launch {
            repository.initEncoder()
        }
        launch {
            repository.initRecordFlow()
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
