package com.lq.video.pipeline

import com.lq.common.MediaClock
import kotlinx.coroutines.channels.Channel

class VideoScheduler(
    private val audioClock: MediaClock
) {

    private val frameMap = sortedMapOf<Long, ByteArray>() // 用 pts 排序更合理

    private var lastDecodedPts = -1L

    private val MAX_LATENCY_US = 120_000L
    private val LEAD_US = 30_000L

    fun pushFrame(pts: Long, data: ByteArray) {
        frameMap[pts] = data

        schedule()
    }

    private fun schedule() {
        val audioPts = audioClock.audioPlayCurrentPts() ?: return
        val targetPts = audioPts + LEAD_US

        val iterator = frameMap.entries.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            val pts = entry.key

            // ❌ 太老的帧：直接丢（关键）
            if (pts < audioPts - MAX_LATENCY_US) {
                iterator.remove()
                continue
            }

            // ✔ 只处理“应该解码的帧”
            if (pts <= targetPts) {

                // ❗ 保证顺序
                if (pts > lastDecodedPts) {
                    decodeQueue.trySend(
                        DecodePacket(entry.value, pts)
                    )
                    lastDecodedPts = pts
                }

                iterator.remove()
            } else {
                break
            }
        }
    }

    // 外部提供
    val decodeQueue = Channel<DecodePacket>(Channel.BUFFERED)

    data class DecodePacket(
        val bytes: ByteArray,
        val pts: Long
    )
}
