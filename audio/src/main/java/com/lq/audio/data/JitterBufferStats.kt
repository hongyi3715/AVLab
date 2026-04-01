package com.lq.audio.data

import android.util.Log

data class JitterBufferStats(
    var totalPacketsReceived: Int = 0, //接收Packet总数
    var totalPacketsOutput: Int = 0, //输出Packet总数
    var totalPacketsLost: Int = 0, //丢失Packet总数
    var totalFastRecovery: Int = 0, //快速恢复Packet总数
    var currentBufferSize: Int = 0, //当前缓冲区大小
    var maxBufferSizeReached: Int = 0 //
) {
    fun JitterBufferStats.printStats() {
        Log.d("JitterBuffer", """
            统计信息:
            - 接收包: $totalPacketsReceived
            - 输出包: $totalPacketsOutput
            - 丢包数: $totalPacketsLost
            - 丢包率: ${totalPacketsLost.toFloat() / totalPacketsReceived * 100}%
            - 快速恢复: $totalFastRecovery
            - 最大缓冲区: $maxBufferSizeReached
        """.trimIndent())
    }
}
