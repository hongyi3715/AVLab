package com.lq.avlab

import com.lq.audio.AudioRecordManager
import com.lq.audio.AudioTrackManager
import com.lq.audio.AacCoder

class AudioRecordRepository {
    private val audioRecordManager = AudioRecordManager

    private val audioTrackManager = AudioTrackManager()

    val recordState = audioRecordManager.recordStateFlow

    val playState = audioTrackManager.stateFlow

    private val coder = AacCoder()


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
            audioTrackManager.setBytesData(pcm)
//            coder.encode(pcm)
        }
    }

    suspend fun initCoder(){
        coder.decodeFlow.collect {
            audioTrackManager.setBytesData(it)
        }
    }

    suspend fun initEncoder(){
        coder.encodeFlow.collect {
            coder.decode(it, System.nanoTime()/1000)
        }
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
