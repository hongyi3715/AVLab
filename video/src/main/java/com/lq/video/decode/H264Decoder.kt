package com.lq.video.decode

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import com.lq.common.MediaClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class H264Decoder {

    private var codec: MediaCodec? = null

    private val VIDEO_DECODE_RENDER_LEAD_US = 30_000L
    private val VIDEO_EARLY_THRESHOLD_US = 50_000L
    private val VIDEO_LATE_THRESHOLD_US = 100_000L

    private var renderJob: Job? = null

    fun start(outputSurface: Surface, width: Int, height: Int, scope: CoroutineScope, audioClock: MediaClock) {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            configure(format, outputSurface, null, 0)
            start()
        }
        startRenderLoop(scope, audioClock)
    }

    // 1. 核心改动：重新恢复成 suspend 函数。当 MediaCodec 满载时，我们 delay 挂起，把压力顶回给 JitterBuffer
    suspend fun decode(data: ByteArray, timestampUs: Long) {
        val c = codec ?: return

        while (true) {
            val inputIndex = c.dequeueInputBuffer(10_000) // 稍微给点等待超时(10ms)
            if (inputIndex >= 0) {
                val inputBuffer = c.getInputBuffer(inputIndex) ?: return
                inputBuffer.clear()
                inputBuffer.put(data)
                c.queueInputBuffer(inputIndex, 0, data.size, timestampUs, 0)
                break // 成功喂入，跳出循环，去处理下一个包
            } else {
                // MediaCodec 内部队列满了（因为输出端正在 delay 憋着画面）
                // 我们在这里 delay(5) 挂起，当前 Channel 消费就会暂停，让包老老实实呆在 JitterBuffer 队列里
                // 绝对不丢弃当前 packet！
                delay(5)
            }
        }

        // 2. 核心改动：把原本在这里的 dequeueOutputBuffer 完全删掉！
        // 任何关于输出的动作都【不要】写在这个方法里，全权交给后台的 renderLoop
    }

    // 独立的渲染同步循环（保持不变，微调超时提升灵敏度）
    private fun startRenderLoop(scope: CoroutineScope, audioClock: MediaClock) {
        renderJob?.cancel()
        renderJob = scope.launch(Dispatchers.IO) {
            val info = MediaCodec.BufferInfo()
            while (isActive) {
                val c = codec ?: break

                // 降低超时到 5ms，更加频繁地去勾引硬件吐出数据，防止输入端饿死
                val outputIndex = c.dequeueOutputBuffer(info, 5000)

                if (outputIndex >= 0) {
                    var shouldRender = false

                    while (isActive) {
                        val audioPtsUs = audioClock.audioPlayCurrentPts()
                        val diffUs = if (audioPtsUs != null) {
                            val targetAudioPtsUs = audioPtsUs + VIDEO_DECODE_RENDER_LEAD_US
                            info.presentationTimeUs - targetAudioPtsUs
                        } else {
                            0
                        }

                        when {
                            // 画面出早了：后台独立循环挂起，等时间到
                            diffUs > VIDEO_EARLY_THRESHOLD_US -> {
                                val waitTimeMs = diffUs / 1000
                                delay(minOf(10, waitTimeMs))
                            }
                            // 过期了：丢弃不渲染
                            diffUs < -VIDEO_LATE_THRESHOLD_US -> {
                                println("画面过期，丢弃 画面pts=${info.presentationTimeUs} audio=$audioPtsUs diff=$diffUs")
                                shouldRender = false
                                break
                            }
                            // 时间刚好
                            else -> {
                                shouldRender = true
                                break
                            }
                        }
                    }

                    c.releaseOutputBuffer(outputIndex, shouldRender)

                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    continue
                }
            }
        }
    }

    fun stop() {
        renderJob?.cancel()
        renderJob = null
        codec?.stop()
        codec?.release()
        codec = null
    }
}
