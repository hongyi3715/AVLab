package com.lq.audio.buffer

import android.os.SystemClock
import com.lq.audio.AudioPacket
import com.lq.audio.data.JitterBufferStats
import com.lq.audio.data.PollResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
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

    private val missTimeoutMs =  40  // 约33ms

    private val maxJump = 4

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
    fun pollFirst(): PollResult {

        // 正常拿到 expected
        buffer.remove(expectedSeq)?.let {
            expectedSeq++
            missingStartTime = 0L
            return PollResult.Packet(it)
        }

        // 当前完全没包
        if (buffer.isEmpty()) {
            missingStartTime = 0L
            return PollResult.Wait
        }

        val nextSeq = buffer.ceilingKey(expectedSeq)
            ?: return PollResult.Wait

        // 差距太大，认为 expected 已经丢了，直接追到 nextSeq
        if (nextSeq - expectedSeq > maxJump) {
            expectedSeq = nextSeq
            missingStartTime = 0L

            return buffer.remove(expectedSeq)
                ?.also { expectedSeq++ }
                ?.let { PollResult.Packet(it) }
                ?: PollResult.Wait
        }

        val now = SystemClock.elapsedRealtime()

        // 第一次发现 expected 没到，开始计时
        if (missingStartTime == 0L) {
            missingStartTime = now
            return PollResult.Wait
        }

        // 还没等够，不补静音
        if (now - missingStartTime < missTimeoutMs) {
            return PollResult.Wait
        }

        // 超时，认为 expected 丢了
        expectedSeq++
        missingStartTime = 0L
        return PollResult.Lost
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
