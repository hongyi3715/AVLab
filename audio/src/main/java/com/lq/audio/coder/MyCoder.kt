package com.lq.audio.coder

class MyCoder {


    private val encoder: AacEncoder = AacEncoder()

    private val decoder: AacDecoder = AacDecoder()

    val audioFlow = decoder.audioFlow

    fun  encode(pcm: ByteArray) = encoder.encode(pcm)

    private fun decode(frame: ByteArray) = decoder.decode(frame)

    suspend fun initEncodeFlow(){
        encoder.aacFlow.collect {
            decode(it)
        }
    }
}
