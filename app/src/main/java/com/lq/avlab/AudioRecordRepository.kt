package com.lq.avlab

import com.lq.audio.record.AudioRecordManager
import com.lq.audio.player.AudioTrackManager
import com.lq.audio.coder.MyCoder

class AudioRecordRepository {
    private val audioRecordManager = AudioRecordManager

    private val audioTrackManager = AudioTrackManager()

    val recordState = audioRecordManager.recordStateFlow

    val playState = audioTrackManager.stateFlow

    private val coder = MyCoder()


     suspend fun initRecordFlow(){
        audioRecordManager.audioFlow.collect { pcm->
/*            val speedUpData = ByteArray(pcm.size / 2)
            var j = 0
            for (i in pcm.indices step 4) {
                if (i + 1 < pcm.size) {
                    speedUpData[j] = pcm[i]
                    speedUpData[j+1] = pcm[i+1]
                    j += 2
                }
            }*/
            println("RecordSize:${pcm.size}")
//            audioTrackManager.setBytesData(pcm)
            coder.encode(pcm)
        }
    }

    suspend fun initCoder(){
        coder.audioFlow.collect {
            println("DecodeFlow Size :${it.size}")
            audioTrackManager.setBytesData(it)
        }
    }

    suspend fun initDecoder(){
        coder.initEncodeFlow()
    }


    suspend fun recordToFile(){
        audioRecordManager.startRecord()
    }

    suspend fun trackPlay(){
        audioTrackManager.play()
    }

    suspend fun stopRecord(){
        audioRecordManager.stopRecord()
    }

    suspend fun stopTrack() = audioTrackManager.stop()

    suspend fun closeRecord(){
        audioRecordManager.close()
    }

    suspend fun closeTrack(){
        audioTrackManager.reset()
    }

}
