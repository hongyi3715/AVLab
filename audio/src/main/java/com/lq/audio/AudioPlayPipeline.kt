package com.lq.audio

import android.os.SystemClock
import com.lq.audio.buffer.JitterBuffer
import com.lq.audio.coder.AacDecoder
import com.lq.audio.data.AudioFrame
import com.lq.audio.data.AudioPacket
import com.lq.audio.data.PollResult
import com.lq.audio.net.UdpSocket
import com.lq.audio.player.AudioTrackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class AudioPlayPipeline {

    private val audioTrackManager = AudioTrackManager()

    val playState = audioTrackManager.stateFlow

    private val decoder: AacDecoder = AacDecoder()

    val jitterBuffer = JitterBuffer()

    private val simulateQueue = Channel<AudioPacket>(Channel.UNLIMITED)

    suspend fun setBytesData(byteArray: ByteArray) = audioTrackManager.setBytesData(byteArray)

    suspend fun play() = audioTrackManager.play()

    suspend fun stop() = audioTrackManager.stop()

    suspend fun reset() = audioTrackManager.reset()


    //接收成功，存放进抖动缓存
    fun initReceiver(scope: CoroutineScope) {
        UdpSocket.onPacketListener = object : UdpSocket.OnPacketListener {
            override fun onPacket(audioPacket: AudioPacket) {
                simulateQueue.trySend(audioPacket)
            }
        }

        UdpSocket.startReceiverAudioPacket()

        scope.launch(Dispatchers.IO) {

            val baseDelay = 20L // 基础延迟 20ms
            val jitter = 10L    // 抖动 ±10ms
            val lossRate = 0.01f // 1% 丢包

            for (packet in simulateQueue) {

                // 丢包
                if (Random.nextFloat() < lossRate) continue

                // 抖动（围绕 baseDelay）
                val delayMs = baseDelay + Random.nextLong(-jitter, jitter)

                delay(delayMs.coerceAtLeast(0))

                jitterBuffer.add(packet.also {
                    it.trace?.bufferInTime = SystemClock.elapsedRealtime()
                })
            }
        }
    }

    //持续拿抖动缓存区，进行解码
    fun initJitterBuffer(scope: CoroutineScope) = scope.launch(Dispatchers.IO) {
        val frameDuration = 10L
        while (jitterBuffer.size < 5) {
            delay(10)
        }

        while (true) {
            when (val result = jitterBuffer.pollFirst()) {

                is PollResult.Packet -> {
                    decoder.decode(AudioFrame(result.packet.payload, result.packet.trace))
                    delay(frameDuration)
                }

                PollResult.Wait -> { // 给迟到包 2~3ms 机会，不补发静音帧
                    delay(3)
                }

                PollResult.Lost -> {
                    println("等待空音频，填充静音帧")
                    setBytesData(ByteArray(4096))
                    delay(frameDuration)
                }
            }
        }
    }


    //解码成功，直接播放
    suspend fun handleDecodeAudio() {
        decoder.audioFlow.collect {
            it.trace?.playTime = SystemClock.elapsedRealtime()
            println("AudioPlayTrace:${it.trace}")
            setBytesData(it.data)
        }
    }
}
