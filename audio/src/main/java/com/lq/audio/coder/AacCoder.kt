package com.lq.audio.coder

import com.lq.audio.data.AudioFrame
import com.lq.audio.data.AudioTrace
import com.lq.audio.data.AudioEncodedFrame

class AacCoder {


    private val encoder: AacEncoder = AacEncoder()

    private val decoder: AacDecoder = AacDecoder()

    val audioFlow = decoder.audioFlow

    val encodeAudioFlow = encoder.aacFlow

    fun  encode(pcm: AudioFrame) = encoder.encode(pcm)


    fun decode(data: ByteArray, ptsUs: Long, trace: AudioTrace?) {
        decoder.decode(
            AudioEncodedFrame(
                data = data,
                ptsUs = ptsUs,
                trace = trace
            )
        )
    }
}
