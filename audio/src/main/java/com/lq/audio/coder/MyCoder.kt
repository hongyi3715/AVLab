package com.lq.audio.coder

class MyCoder {


    private val encoder: AacEncoder = AacEncoder()

    private val decoder: AacDecoder = AacDecoder()

    val audioFlow = decoder.audioFlow

    fun  encode(pcm: ByteArray) = encoder.encode(pcm)

    private fun decode(frame: ByteArray) = decoder.decode(frame)

    suspend fun initEncodeFlow(){
        encoder.aacFlow.collect { aacFrame ->
            println("Encoder AAC Size:${aacFrame.size}")
            if(aacFrame.size<=7){
                val firstBytes = aacFrame.take(aacFrame.size).joinToString { "%02X".format(it) }
                println("CSD :$firstBytes")
            }
            if (aacFrame.size >= 7) {
                val firstBytes = aacFrame.take(7).joinToString { "%02X".format(it) }
                println("AAC first 7 bytes: $firstBytes")

                // 检查是否是 ADTS 头部 (应该以 0xFF 0xF1 开头)
                val hasAdtsHeader = aacFrame[0] == 0xFF.toByte() &&
                        (aacFrame[1].toInt() and 0xF0) == 0xF0
                println("Has ADTS header: $hasAdtsHeader")
            }
            decode(aacFrame)
        }
    }
}
