package com.lq.video.pipeline

import com.lq.common.MediaClock

class VideoSyncController(
    private val audioClock: MediaClock
) {

    private val maxEarlyUs = 60_000      // 实时播放里视频最多允许超前 60ms
    private val maxLateUs = -120_000     // 视频落后超过 120ms 再追帧
    private val maxFrameGapUs = 500_000  // 直播场景下认为时间轴断开的阈值
    private val maxWaitUs = 250_000      // 不允许因为视频超前而长时间卡住渲染线程

    private var baseOffsetUs: Long? = null
    private var lastVideoPtsUs: Long? = null

    private var firstFrameRendered = false
    fun decide(videoPtsUs: Long): RenderDecision {
        val audioPtsUs = audioClock.audioPlayCurrentPts()

        // 第一帧：直接渲染，建立 baseline，不参与早晚判断
        if (!firstFrameRendered || isVideoTimelineJump(videoPtsUs)) {
            resetBaseLine(videoPtsUs, audioPtsUs ?: 0L)
            return RenderDecision.Render
        }

        if (audioPtsUs == null) return RenderDecision.Wait(5)

        val offset = baseOffsetUs ?: 0L
        val diff = (videoPtsUs - audioPtsUs) + offset

        if (DEBUG_LOG) {
            println("Video DrawFrame videoPtsUs:${videoPtsUs} audioPts:$audioPtsUs diff:$diff")
        }

        return when {
            diff > maxWaitUs -> {
                resetBaseLine(videoPtsUs, audioPtsUs)
                RenderDecision.Render
            }
            diff > maxEarlyUs -> RenderDecision.Wait((diff - maxEarlyUs) / 1000) // 精确等待差值
            diff < maxLateUs -> RenderDecision.Drop
            else -> {
                lastVideoPtsUs = videoPtsUs
                RenderDecision.Render
            }
        }
    }

    private fun isVideoTimelineJump(videoPtsUs: Long): Boolean {
        val lastPts = lastVideoPtsUs ?: return false
        return kotlin.math.abs(videoPtsUs - lastPts) > maxFrameGapUs
    }

    private fun resetBaseLine(videoPtsUs: Long, audioPtsUs: Long) {
        firstFrameRendered = true
        baseOffsetUs = audioPtsUs - videoPtsUs
        lastVideoPtsUs = videoPtsUs
        if (DEBUG_LOG) {
            println("Video sync reset videoPtsUs:$videoPtsUs audioPts:$audioPtsUs offset:$baseOffsetUs")
        }
    }

    companion object {
        private const val DEBUG_LOG = false
    }
}


sealed class RenderDecision {
    object Render : RenderDecision()
    object Drop : RenderDecision()
    data class Wait(val delayMs: Long) : RenderDecision()
}
