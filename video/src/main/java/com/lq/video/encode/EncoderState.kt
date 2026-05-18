package com.lq.video.encode


sealed class EncoderState {
    data object IDLE: EncoderState()
    data object RUNNING: EncoderState()

    data object STOPPED: EncoderState()

    data class ERROR(val exception: Exception): EncoderState()
}

