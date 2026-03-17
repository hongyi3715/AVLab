package com.lq.audio.player

import android.media.AudioTrack
import com.lq.audio.player.PlayState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.Exception
import kotlin.coroutines.cancellation.CancellationException

class AudioTrackManager {
    private val config: AudioTrackConfig = AudioTrackConfig()
    private var audioTrack: AudioTrack? = null
    private val mutex = Mutex()
    private val _stateFlow = MutableStateFlow<PlayState>(PlayState.Idle)

    val stateFlow = _stateFlow.asStateFlow()

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is Exception) {
            throwable.printStackTrace() //添加日志记录
        }
    }


    /* 默认在io线程执行 */
    suspend fun setBytesData(byteArray: ByteArray) = mutex.withLock {
        if (_stateFlow.value is PlayState.Playing) {
            audioTrack?.write(byteArray, 0, byteArray.size)
        }
    }

    suspend fun play() = mutex.withLock {
        if (_stateFlow.value == PlayState.Playing) return@withLock
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

    private suspend fun resetInternal() = mutex.withLock {
        audioTrack?.stop()
        audioTrack = null
    }


    suspend fun pause() = mutex.withLock {
        if (_stateFlow.value !is PlayState.Playing) return@withLock
        audioTrack?.pause()
        _stateFlow.value = PlayState.Paused
    }

    suspend fun stop() = mutex.withLock {
        if (_stateFlow.value !is PlayState.Playing) return@withLock
        audioTrack?.stop()
        _stateFlow.value = PlayState.Stopped
    }

    suspend fun reset() = mutex.withLock {
        audioTrack?.stop()
        audioTrack?.flush()
        audioTrack?.release()
        audioTrack = null
        _stateFlow.value = PlayState.Idle
    }

}
