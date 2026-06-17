package com.lq.audio.record

import android.media.AudioRecord
import android.media.AudioTimestamp
import com.lq.audio.buffer.AudioRingBuffer
import com.lq.audio.data.AudioFrame
import com.lq.audio.data.AudioTrace
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.coroutines.cancellation.CancellationException

object AudioRecordManager {
    private val _recordStateFlow = MutableStateFlow<RecordState>(RecordState.Idle)

    val recordStateFlow = _recordStateFlow.asStateFlow()
    private var audioRecord: AudioRecord? = null

    /*
     * SUSPEND 数据完整但可能高延迟,适用于离线处理
     * DROP_OLDEST 丢弃旧数据,适用于实时处理
     */
    private val _audioFlow = MutableSharedFlow<AudioFrame>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val audioFlow = _audioFlow.asSharedFlow()

    private val config = AudioRecordFactory.createCallAudioConfig()

    private val mutex = Mutex()

    private val audioDispatcher = Executors
        .newSingleThreadExecutor { r ->
            Thread(r, "AudioThread").apply {
                priority = Thread.MAX_PRIORITY
            }
        }
        .asCoroutineDispatcher()


    private val coroutineScope = CoroutineScope(audioDispatcher + SupervisorJob())

    private var job: Job? = null

    private val audioBuffer by lazy { AudioRingBuffer(config.frameSize) }

    private var recordJob: Job? = null


    init {
        recordJob = getRecordJob().apply { start() }
    }

    private fun getRecordJob(): Job {
        return if (recordJob != null && !recordJob!!.isCancelled && !recordJob!!.isCompleted) recordJob!!
        else
            coroutineScope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
                while (isActive) {
                    if (_recordStateFlow.value is RecordState.Recording) {
                        val audioFrame = audioBuffer.read()
                        if (audioFrame.data.isNotEmpty()) _audioFlow.tryEmit(audioFrame)
                        //todo 归还数据不太合理,考虑不增加池化
                        //audioBuffer.returnBuffer(audioFrame.data)
                    } else {
                        delay(60)
                    }
                }
            }
    }

    fun startRecord() = coroutineScope.launch(recordCoroutineExceptionHandler) {
        mutex.withLock {
            if (job?.isActive == true) return@launch
            if (audioRecord == null) audioRecord = config.getAudioRecord()
            val record = audioRecord ?: return@launch
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                _recordStateFlow.value = RecordState.Error(code = record.state)
                return@launch
            }

            record.startRecording()
            _recordStateFlow.value = RecordState.Recording
            job = coroutineScope.launch(Dispatchers.IO) {
                try {
                    while (isActive && _recordStateFlow.value == RecordState.Recording) {
                        val readBuffer = audioBuffer.borrowBuffer()

                        val size = record.read(readBuffer, 0, readBuffer.size)
                        if (size > 0) {
                            val pts = getAudioRecordTime()
                            val audioFrame = AudioFrame(data = readBuffer, ptsUs = pts, trace = AudioTrace(recordTime = pts))
                            audioBuffer.write(audioFrame)
                        } else {
                            audioBuffer.returnBuffer(readBuffer)
                            _recordStateFlow.value = RecordState.Error(code = size)
                        }
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        _recordStateFlow.value = RecordState.Error(throwable = e)
                        //为了语义化正确不使用stopRecord
                        withContext(NonCancellable) {
                            mutex.withLock {
                                stopRecordInternal()
                            }
                        }
                    }
                }
            }
        }
    }


    private fun stopRecordInternal() {
        job?.cancel()
        job = null

        runCatching {
            audioRecord?.run {
                stop()
                release()
            }
        }
        audioRecord = null
    }

    /*
    * 防止部分厂商的状态出现stop之后start崩溃，况且重新启动一个audioRecord的反应也很快
    * 这里直接清除当前引用，使用后续重建对象
    * */
    suspend fun stopRecord() =
        mutex.withLock {
            stopRecordInternal()
        }


    suspend fun close() =
        mutex.withLock {
            stopRecordInternal()
            recordJob?.cancel()
            recordJob = null
        }


    /*
    * AudioTimestamp.TIMEBASE_BOOTTIME 系统启动到捕获音频的经过时间（含休眠）
    * AudioTimestamp.TIMEBASE_MONOTONIC	系统运行时间（不含休眠）
    * elapsedRealtime 含休眠时间
    * nanoTime 不含休眠时间
    * */
    private val reusableAudioTimestamp = AudioTimestamp()
    private fun getAudioRecordTime(): Long {
        audioRecord?.getTimestamp(reusableAudioTimestamp, AudioTimestamp.TIMEBASE_MONOTONIC)
        return reusableAudioTimestamp.framePosition * 1_000_000 / config.sampleRate
    }

    private val recordCoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is Exception) {
            throwable.printStackTrace()
        }
    }
}
