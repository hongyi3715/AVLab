package com.lq.audio

import android.os.SystemClock
import com.lq.audio.buffer.JitterBuffer
import com.lq.audio.coder.AacDecoder
import com.lq.audio.data.AudioPacket
import com.lq.audio.data.AudioEncodedFrame
import com.lq.audio.data.PollResult
import com.lq.audio.net.AudioUdpSocket
import com.lq.audio.player.AudioTrackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AudioPlayPipeline {

     val audioTrackManager = AudioTrackManager()

    val playState = audioTrackManager.stateFlow

    private val decoder: AacDecoder = AacDecoder()

    val jitterBuffer = JitterBuffer()

    private val bufferChannel = Channel<AudioPacket>(capacity = 16,onBufferOverflow = BufferOverflow.DROP_OLDEST)

     fun setBytesData(byteArray: ByteArray) = audioTrackManager.setBytesData(byteArray)

     fun play() = audioTrackManager.play()

     fun stop() = audioTrackManager.stop()

     fun reset() = audioTrackManager.reset()


    private val audioBridge = AudioJniBridge()

    //接收成功，存放进抖动缓存
    private var lastTime = 0L
    fun initReceiver(scope: CoroutineScope) {
        AudioUdpSocket.startReceiverAudioPacket{ audioPacket ->
            //获取播放偏移值
            val sendTime = audioPacket.timestamp
            if (lastTime != 0L) {  // 第一包不计算offset
                val offset = sendTime - lastTime
                audioBridge.addPlayOffset(offset)
            }
            lastTime = sendTime
            bufferChannel.trySend(audioPacket)
        }

        scope.launch(Dispatchers.IO) {
            for (packet in bufferChannel) {
                jitterBuffer.add(packet.also {
                    it.trace?.bufferInTime = SystemClock.elapsedRealtime()
                })
            }
        }
    }

    /*
    * 持续拿抖动缓存区，进行解码
    * 解码后直接播放，rtc中音频不等待直接播
    * */
    fun initJitterBuffer(scope: CoroutineScope) = scope.launch(Dispatchers.IO) {
        val frameDurationMs = 23L
        while (true) {
            val predictedOffsetUs = audioBridge.getTimeTriggered()
            val predictedOffsetMs = predictedOffsetUs / 1000
//            println("预估抖动:$predictedOffsetMs")
            if (predictedOffsetMs > 0) {
                jitterBuffer.dynamicMissTimeoutMs = coerceJitterTimeout(predictedOffsetMs)
            }
/*            println(
                "size=${jitterBuffer.size} " +
                        "expected=${jitterBuffer.expectedSeqForDebug()} " +
                        "first=${jitterBuffer.peekFirst()?.seq} " +
                        "last=${jitterBuffer.peekLast()?.seq} " +
                        "dynamic=${jitterBuffer.dynamicMissTimeoutMs}"
            )*/
            when (val result = jitterBuffer.pollFirst()) {

                is PollResult.Packet -> {
                    decoder.decode(AudioEncodedFrame(result.packet.payload, result.packet.timestamp,result.packet.trace))
//                    delay(frameDurationMs)
                }

                PollResult.Lost -> {
                    println("等待空音频，填充静音帧")
                    setBytesData(ByteArray(4096))
                    delay(frameDurationMs)
                }

                PollResult.Wait -> { // 给迟到包 2~3ms 机会，不补发静音帧

                    delay(3)
                }
            }
        }
    }

    fun coerceJitterTimeout(predictedOffsetMs: Long): Long {
        // 1. 放大因子（抗抖动余量）：网络抖动预估是均值，实际我们需要乘以 1.5 ~ 2.0 的系数来容忍极端峰值
        val safetyTimeout = (predictedOffsetMs * 1.5).toLong()

        // 2. 边界夹逼：
        // MIN_TIMEOUT = 20ms : 即使网络再好，也至少给迟到包 20ms 的生存机会，防止误判
        // MAX_TIMEOUT = 120ms: 即使网络再烂，RTC 音频的等待极限也就是 120ms，再大延迟就没法聊天了
        return safetyTimeout.coerceIn(20L, 120L)
    }


    //解码成功，直接播放
    suspend fun handleDecodeAudio() {
        decoder.audioFlow.collect {
            it.trace?.playTime = SystemClock.elapsedRealtime()
            println("音频trace:${it.trace}")
            setBytesData(it.data)
        }
    }

    fun release(){
        audioBridge.release()
    }
}
