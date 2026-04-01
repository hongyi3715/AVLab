package com.lq.audio.buffer

import android.util.Log
import com.lq.audio.AudioPacket
import com.lq.audio.data.JitterBufferStats
import java.util.TreeMap

/*
* 抖动缓冲
* */
class JitterBuffer {

    private val buffer = TreeMap<Int, AudioPacket>()

    private var expectedSeq = 0

    private val maxBufferSize = 200 //最大缓冲数量

    private val maxExpiredTime = 3000 //罪大超时时间

    private val maxMissPacketSize = 3 //超过三帧后放弃

    private var currentMissCount = 0

    private val status = JitterBufferStats()

    @Synchronized
    fun add(packet: AudioPacket) {
        clearExpiredPackets() //超过最大超时时间需要清理
        if (packet.seq < expectedSeq) return //过老的packet，丢弃
        if (buffer.size >= maxBufferSize) { //超过最大缓存限制，优先舍弃最老的packet
            val oldestSeq = buffer.firstKey()
            buffer.remove(oldestSeq)

            if (oldestSeq == expectedSeq) {
                expectedSeq = oldestSeq + 1
            }
        }

        packet.receiveTime = System.currentTimeMillis()
        buffer[packet.seq] = packet
    }

    @Synchronized
    fun poll(): AudioPacket? {
        if(buffer.isEmpty()) return null
        buffer.remove(expectedSeq)?.let {
            expectedSeq++
            return it
        }
        val nextSeq = buffer.firstKey()
        val gap = nextSeq - expectedSeq
        //连续丢失多个packet 直接跳过
        if (currentMissCount >= maxMissPacketSize) {
            Log.w("JitterBuffer", "连续丢包超过$maxMissPacketSize，跳过 $gap 个包")
            expectedSeq = nextSeq
            currentMissCount = 0

            return buffer.remove(expectedSeq)?.also {
                expectedSeq++
            }
        }
        currentMissCount++
        return null //跳过，播放静音帧
    }

    private fun clearExpiredPackets() {
        val now = System.currentTimeMillis()
        val expired = buffer.filter {
            now - it.value.receiveTime >= maxExpiredTime && it.key < expectedSeq
        }
        expired.forEach { (i, _) ->
            buffer.remove(i)
        }
    }

    @Synchronized
    fun peekFirst(): AudioPacket? = buffer.firstEntry()?.value

    @Synchronized
    fun peekLast(): AudioPacket? = buffer.lastEntry()?.value

    val size get() = buffer.size
}
