package com.lq.video.decode

data class FrameBuffer(
    val packetCount: Int
) {
    val packets = Array<ByteArray?>(packetCount) { null }

    fun add(index: Int, data: ByteArray) {
        packets[index] = data
    }

    fun isComplete(): Boolean {
        return packets.all { it != null }
    }

    fun merge(): ByteArray {
        return packets.filterNotNull().reduce { acc, bytes -> acc + bytes }
    }
}
