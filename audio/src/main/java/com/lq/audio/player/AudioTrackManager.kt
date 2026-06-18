package com.lq.audio.player

import android.media.AudioTrack
import android.os.Process
import com.lq.common.MediaClock
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.Executors
import kotlin.coroutines.cancellation.CancellationException

class AudioTrackManager : MediaClock {

    private val config = AudioTrackConfig()

    @Volatile
    var audioTrack: AudioTrack? = null

    // 音频数据队列（不丢旧数据，保证连续性）
    private val channel = Channel<ByteArray>(capacity = 64)

    private val dispatcher = Executors
        .newSingleThreadExecutor { r ->
            Thread(r, "AudioTrackThread").apply {
                priority = Thread.MAX_PRIORITY
            }
        }
        .asCoroutineDispatcher()

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    @Volatile
    private var started = false

    private var startNanoTime: Long = 0L

    init {
        startLoop()
    }


    private fun startLoop() = scope.launch {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

        for (data in channel) {
            val track = audioTrack ?: continue

            try {
                track.write(data, 0, data.size)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun push(data: ByteArray) {
        channel.trySend(data)
    }


    fun play() = scope.launch {
        if (started) return@launch

        if (audioTrack == null) {
            audioTrack = config.getAudioTrack()
        }

        audioTrack?.play()

        startNanoTime = System.nanoTime()
        started = true
    }

    fun pause() = scope.launch {
        audioTrack?.pause()
    }

    fun stop() = scope.launch {
        audioTrack?.stop()
        started = false
    }

    fun release() = scope.launch {
        audioTrack?.apply {
            stop()
            flush()
            release()
        }
        audioTrack = null
        started = false
        channel.close()
    }

    private fun getAudioPlaybackTimeUs(): Long? {
        val track = audioTrack ?: return null
        return track.playbackHeadPosition * 1_000_000L / config.sampleRate
    }

    override fun audioPlayCurrentPts(): Long? {
        return getAudioPlaybackTimeUs()
    }
}
