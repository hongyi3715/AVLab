package com.lq.audio.coder

import com.lq.audio.buffer.JitterBuffer

class AacCoder {


    private val encoder: AacEncoder = AacEncoder()

    private val decoder: AacDecoder = AacDecoder()

    val audioFlow = decoder.audioFlow

    val encodeAudioFlow = encoder.aacFlow

    fun  encode(pcm: ByteArray) = encoder.encode(pcm)

    fun decode(frame: ByteArray) = decoder.decode(frame)


}
