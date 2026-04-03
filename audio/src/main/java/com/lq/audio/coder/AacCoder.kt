package com.lq.audio.coder

import com.lq.audio.data.AudioFrame

class AacCoder {


    private val encoder: AacEncoder = AacEncoder()

    private val decoder: AacDecoder = AacDecoder()

    val audioFlow = decoder.audioFlow

    val encodeAudioFlow = encoder.aacFlow

    fun  encode(pcm: AudioFrame) = encoder.encode(pcm)

    suspend fun decode(frame: AudioFrame) = decoder.decode(frame)


}
