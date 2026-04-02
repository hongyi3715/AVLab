package com.lq.audio.buffer

import android.os.SystemClock
import android.util.Log
import com.lq.audio.AudioPacket
import com.lq.audio.data.JitterBufferStats
import java.util.TreeMap

/*
* 抖动缓冲
* */
class JitterBuffer {

    private val buffer = TreeMap<Int, AudioPacket>()

    private var expectedSeq = 1

    private val maxBufferSize = 200 //最大缓冲数量

    private val maxExpiredTime = 3000 //罪大超时时间

    private var missingStartTime = 0L

    private val frameDurationMs = 1024L * 1000 / 44100 // 23ms

    private val missTimeoutMs =  33  // 约33ms

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

        buffer[packet.seq] = packet
    }

    @Synchronized
    fun poll(): AudioPacket? {
        buffer.remove(expectedSeq)?.let {
            expectedSeq++
            missingStartTime = 0L
            return it
        }

        if (missingStartTime == 0L) {
            missingStartTime = System.currentTimeMillis()
            return null
        }

        if (System.currentTimeMillis() - missingStartTime >= missTimeoutMs) {
            println("等待时长超过,跳过当前帧")
            expectedSeq++
            missingStartTime = 0L
        }
        return null //跳过，播放静音帧
    }

    /*
    * 1 2 3 4 5
    *
    * 1 3  2  5    4
    *
    * 1
    * */
    @Synchronized
    fun pollFirst(): AudioPacket? {
        buffer.remove(expectedSeq)?.let {
            expectedSeq++
            return it
        }
        if (buffer.isEmpty()) {
            missingStartTime = 0L
            return null
        }
        val nextSeq = buffer.ceilingKey(expectedSeq) ?: return null

        // 落后太多，直接追上
        if (nextSeq - expectedSeq > 3) {
            expectedSeq = nextSeq
            missingStartTime = 0L

            return buffer.remove(expectedSeq)?.also {
                expectedSeq++
            }
        }

        // 只落后一点，给它一点时间等迟到包
        if (missingStartTime == 0L) {
            missingStartTime = SystemClock.elapsedRealtime()
            return null
        }

        if (SystemClock.elapsedRealtime() - missingStartTime >= missTimeoutMs) {
            // 超时，认为 expected 丢了
            expectedSeq++
            missingStartTime = 0L
        }
        return null
    }

    private fun clearExpiredPackets() {
        try {
            val now = System.currentTimeMillis()
            val expired = buffer.filter {
                now - it.value.trace!!.receiveTime >= maxExpiredTime && it.key < expectedSeq
            }
            expired.forEach { (i, _) ->
                buffer.remove(i)
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    @Synchronized
    fun peekFirst(): AudioPacket? = buffer.firstEntry()?.value

    @Synchronized
    fun peekLast(): AudioPacket? = buffer.lastEntry()?.value

    val size get() = buffer.size
}
