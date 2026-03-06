package com.lq.avlab.ui.activity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lq.audio.RecordState
import com.lq.avlab.AudioRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val repository: AudioRecordRepository) :
    ViewModel() {

    private var isRecording: Boolean = false

     var recordState: RecordState = RecordState.Idle

    init {
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                repository.initEncoder()
            }
            launch {
                repository.initCoder()
            }
            launch {
                repository.initRecordFlow()
            }
            launch {
                repository.recordState.collect {
                    Log.d("RecordState","State: $it")
                    recordState = it
                    isRecording = it == RecordState.Recording
                }
            }
            launch {
                repository.playState.collect {
                }
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
            repository.recordToFile()
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

}
