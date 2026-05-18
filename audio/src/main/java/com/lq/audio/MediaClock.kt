package com.lq.audio

object MediaClock {
    private var startNs = -1L

    fun reset() {
        startNs = -1L
    }

    fun ptsNs(nowNs: Long): Long {
        if (startNs < 0) startNs = nowNs
        return nowNs - startNs
    }
}
