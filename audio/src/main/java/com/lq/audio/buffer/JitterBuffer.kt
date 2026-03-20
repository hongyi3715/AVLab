package com.lq.audio.buffer

import com.lq.audio.AudioPacket
import java.util.TreeMap

class JitterBuffer {

    private val buffer = TreeMap<Int, AudioPacket>()

    private var expectedSeq = 0

    @Synchronized
    fun add(packet: AudioPacket) {
        buffer[packet.seq] = packet
    }

    @Synchronized
    fun poll(): AudioPacket? {
        val packet = buffer.firstEntry()
        if (packet != null) {
            buffer.remove(packet.key)
            expectedSeq = packet.key + 1
            return packet.value
        }
        // 丢包：返回 null 或补偿
        return null
    }

    @Synchronized
    fun peekFirst() : AudioPacket? = buffer.firstEntry()?.value

    @Synchronized
    fun peekLast() : AudioPacket? = buffer.lastEntry()?.value

    val size get() = buffer.size
}
