package com.lq.avlab.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lq.avlab.RecordPipeline
import com.lq.avlab.ui.App
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(): ViewModel(){

    private val recordPipeline = RecordPipeline()

    val videoRecordPipeline = recordPipeline.video


    fun startRecord(){
        val output = File(App.instance.externalCacheDir, "${System.currentTimeMillis()}.mp4")
        recordPipeline.start(output,viewModelScope)
    }

    fun stopRecord()=viewModelScope.launch(Dispatchers.IO){
        recordPipeline.stop()
    }

}
