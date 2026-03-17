package com.lq.audio.record

import android.media.MediaRecorder
import com.lq.audio.record.AudioRecordType

object AudioRecordFactory {
    private fun createAudioConfig(type: AudioRecordType): AudioRecordConfig =
        when (type) {
            AudioRecordType.NORMAL -> AudioRecordConfig(audioSource = MediaRecorder.AudioSource.MIC)
            AudioRecordType.CALL -> AudioRecordConfig()
        }

    fun createCallAudioConfig(): AudioRecordConfig = createAudioConfig(AudioRecordType.CALL)

    fun createNormalAudioConfig(): AudioRecordConfig = createAudioConfig(AudioRecordType.NORMAL)
}
