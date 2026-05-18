package com.lq.audio.coder

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

sealed class AudioEncodeEvent {
    data class Format(val format: MediaFormat) : AudioEncodeEvent()
    data class Data(val data: ByteBuffer, val bufferInfo: MediaCodec.BufferInfo) : AudioEncodeEvent()
}

