package com.lq.audio.player

import android.media.AudioTrack
import android.os.Process
import com.lq.common.MediaClock
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.concurrent.Executors
import kotlin.coroutines.cancellation.CancellationException

class AudioTrackManager : MediaClock {
    private val config: AudioTrackConfig = AudioTrackConfig()
    var audioTrack: AudioTrack? = null

    private val _stateFlow = MutableStateFlow<PlayState>(PlayState.Idle)
    val stateFlow = _stateFlow.asStateFlow()

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is Exception) {
            throwable.printStackTrace()
        }
    }

    // 专属单线程调度器
    private val audioDispatcher = Executors
        .newSingleThreadExecutor { r ->
            Thread(r, "AudioTrackThread").apply {
                // 在线程启动时直接确立音频级优先级
                priority = Thread.MAX_PRIORITY
            }
        }
        .asCoroutineDispatcher()

    private val audioTrackScope = CoroutineScope(audioDispatcher + SupervisorJob())

    // 🚀 优化1：防堆积的 Channel
    // 容量设为 16 帧，满了自动丢弃最老的数据（DROP_OLDEST），防止播放端卡顿后引发持续性的回放延迟
    private val channel = Channel<ByteArray>(capacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        playLoop()
    }

    private fun playLoop() = audioTrackScope.launch(coroutineExceptionHandler) {
        // 在专属线程里提升底层 Linux 线程优先级
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

        channel.consumeEach {
            if (_stateFlow.value is PlayState.Playing) {
                audioTrack?.write(it, 0, it.size) // WRITE_BLOCKING 卡在这里也是安全的
            }
        }
    }

    /* 🚀 优化2：利用 trySend 消除协程 launch 的开销 */
    fun setBytesData(byteArray: ByteArray) {
        if (_stateFlow.value is PlayState.Playing) {
            // trySend 是纯同步、非阻塞的，不需要启动新协程，开销极低
            channel.trySend(byteArray)
        }
    }

    // 🚀 优化3：所有控制流全部 launch 到同一个单线程调度器中，彻底废除 Mutex 锁
    fun play() = audioTrackScope.launch {
        if (_stateFlow.value == PlayState.Playing) return@launch
        if (audioTrack == null) audioTrack = config.getAudioTrack()
        try {
            audioTrack?.play()
            _stateFlow.value = PlayState.Playing
        } catch (e: Exception) {
            e.printStackTrace()
            if (e !is CancellationException) {
                resetInternal()
                _stateFlow.value = PlayState.Error(e)
            }
        }
    }

    private fun resetInternal() {
        audioTrack?.stop()
        audioTrack = null
        channel.tryReceive() // 清空一下队列
    }

    fun pause() = audioTrackScope.launch {
        if (_stateFlow.value !is PlayState.Playing) return@launch
        audioTrack?.pause()
        _stateFlow.value = PlayState.Paused
    }

    fun stop() = audioTrackScope.launch {
        if (_stateFlow.value !is PlayState.Playing) return@launch
        audioTrack?.stop()
        _stateFlow.value = PlayState.Stopped
    }

    fun reset() = audioTrackScope.launch {
        audioTrack?.stop()
        audioTrack?.flush()
        audioTrack?.release()
        audioTrack = null
        _stateFlow.value = PlayState.Idle
    }

    private fun getAudioPlaybackTimeUs(): Long? {
        val track = audioTrack ?: return null
        return track.playbackHeadPosition * 1_000_000L / config.sampleRate
    }

    override fun audioPlayCurrentPts(): Long? {
        return getAudioPlaybackTimeUs()
    }
}
