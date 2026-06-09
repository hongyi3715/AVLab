package com.lq.audio.buffer

import com.lq.audio.data.AudioFrame
import java.util.concurrent.ArrayBlockingQueue

/*
* AudioFrame
* AudioRecord RingBuffer 录音数据环形缓冲区
* */
class AudioRingBuffer(private val frameSize: Int) {

    private val bufferPool = ArrayBlockingQueue<ByteArray>(8).apply {
        repeat(8) { offer(ByteArray(frameSize)) }
    }
    private val frameQueue = ArrayBlockingQueue<AudioFrame>(16)

    fun borrowBuffer(): ByteArray = bufferPool.poll() ?: ByteArray(frameSize)

    fun write(audioFrame: AudioFrame) {
        if (!frameQueue.offer(audioFrame)) {
            bufferPool.offer(audioFrame.data) // 丢帧但归还内存
        }
    }

    fun read(): AudioFrame = frameQueue.take()

    fun returnBuffer(data: ByteArray) = bufferPool.offer(data)
}
