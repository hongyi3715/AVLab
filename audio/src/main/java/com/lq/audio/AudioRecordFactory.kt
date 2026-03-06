package com.lq.audio

import android.media.MediaRecorder

object AudioRecordFactory {
    private fun createAudioConfig(type: AudioRecordType): AudioRecordConfig =
        when (type) {
            AudioRecordType.NORMAL -> AudioRecordConfig(audioSource = MediaRecorder.AudioSource.MIC)
            AudioRecordType.CALL -> AudioRecordConfig()
        }

    fun createCallAudioConfig(): AudioRecordConfig = createAudioConfig(AudioRecordType.CALL)

    fun createNormalAudioConfig(): AudioRecordConfig = createAudioConfig(AudioRecordType.NORMAL)
}
