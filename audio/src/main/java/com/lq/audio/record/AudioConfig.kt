package com.lq.audio.record

abstract class AudioConfig {
    abstract val sampleRate: Int
    abstract val channel: Int
    abstract val encoding: Int
    abstract val bufferSizeFactor: Int
    abstract val sessionId: Int
}
