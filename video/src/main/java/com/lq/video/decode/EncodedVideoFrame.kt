package com.lq.video.decode

import android.media.MediaCodec

data class EncodedVideoFrame(
    val ptsUs: Long,
    val flags: Int,
    val data: ByteArray,
) {
    val isConfig: Boolean
        get() = (flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0

    val isKeyFrame: Boolean
        get() = (flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
}
