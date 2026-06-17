package com.lq.audio.buffer

import android.os.SystemClock
import com.lq.audio.data.AudioPacket
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

    private var expectedSeq = -1  // -1 表示未初始化

    private val maxBufferSize = 200 //最大缓冲数量

    private val maxExpiredTime = 3000 //罪大超时时间

    private var missingStartTime = 0L

    @Volatile var dynamicMissTimeoutMs = 40L

    private val maxJump = 4

    @Synchronized
    fun add(packet: AudioPacket) {
        clearExpiredPackets() //超过最大超时时间需要清理
        if (expectedSeq == -1) {
            expectedSeq = packet.seq
        }
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
        if (now - missingStartTime < dynamicMissTimeoutMs) {
            return PollResult.Wait
        }

        // 超时，认为 expected 丢了
        expectedSeq++
        missingStartTime = 0L
        return PollResult.Lost
    }

    private fun clearExpiredPackets() {
        val now = System.currentTimeMillis()
        while (buffer.isNotEmpty()) {
            val firstKey = buffer.firstKey()
            val packet = buffer[firstKey]
            if (packet != null && now - packet.trace!!.receiveTime >= maxExpiredTime && firstKey < expectedSeq) {
                buffer.remove(firstKey)
            } else {
                break // 因为是有序的，第一个没过期，后面的更不可能过期
            }
        }
    }

    @Synchronized
    fun peekFirst(): AudioPacket? = buffer.firstEntry()?.value

    @Synchronized
    fun peekLast(): AudioPacket? = buffer.lastEntry()?.value

    val size get() = buffer.size
}
