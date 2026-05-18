package com.lq.video.encode

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

sealed class EncoderEvent {
    data class Format(val format: MediaFormat) : EncoderEvent()
    data class Data(val data: ByteBuffer, val bufferInfo: MediaCodec.BufferInfo) : EncoderEvent()
}
