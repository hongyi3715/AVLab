package com.lq.avlab

import com.lq.audio.AudioPacket
import com.lq.audio.net.UdpSocket
import com.lq.audio.buffer.JitterBuffer
import com.lq.audio.record.AudioRecordManager
import com.lq.audio.player.AudioTrackManager
import com.lq.audio.coder.AacCoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class AudioRecordRepository {
    private val audioRecordManager = AudioRecordManager

    private val audioTrackManager = AudioTrackManager()

    val recordState = audioRecordManager.recordStateFlow

    val playState = audioTrackManager.stateFlow

    private val coder = AacCoder()

    val jitterBuffer = JitterBuffer()

    private val simulateQueue = Channel<AudioPacket>(Channel.UNLIMITED)
    //录音音频直接取编码aac
     suspend fun initRecordFlow(){
        audioRecordManager.audioFlow.collect { pcm->
            coder.encode(pcm)
        }
    }


    //编码aac成功，发送到客户端
    suspend fun initEncoder(){
        coder.encodeAudioFlow.collect {
            UdpSocket.sendAudioPacket(it)
        }
    }


    //接收成功，存放进抖动缓存
     fun initReceiver(scope: CoroutineScope){
        UdpSocket.onPacketListener = object : UdpSocket.OnPacketListener{
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
//                val delayMs = baseDelay + Random.nextLong(-jitter, jitter)

//                delay(delayMs.coerceAtLeast(0))

                jitterBuffer.add(packet)
            }
        }
    }

    //持续拿抖动缓存区，进行解码，如何缓存重排序？静音丢弃？
    suspend fun initJitterBuffer(){
        val frameDuration = 21L

        while (jitterBuffer.size<5){
            delay(10)
        }

        while (true) {
            val start = System.nanoTime()
            val packet = jitterBuffer.poll()
            if (packet != null) {
                 coder.decode(packet.payload)
            } else {// 计算方式是 采样率 * 字节 = 一秒的字节数，当前间隔*字节数=当前间隔的字节数
                println("等待空音频，填充静音帧")
                audioTrackManager.setBytesData(ByteArray(4096))
            }
            val cost = (System.nanoTime() - start) / 1_000_000
            val sleep = frameDuration - cost

            if (sleep > 0) {
                delay(sleep)
            }
        }
    }

    //解码成功，直接播放
    suspend fun handleDecodeAudio(){
        coder.audioFlow.collect {
            println("DecodeFlow Size :${it.size}")
            audioTrackManager.setBytesData(it)
        }
    }


    //录音
    suspend fun startToRecord(){
        audioRecordManager.startRecord()
    }

    //播放
    suspend fun trackPlay(){
        audioTrackManager.play()
    }

    suspend fun stopRecord(){
        audioRecordManager.stopRecord()
    }

    suspend fun stopTrack() = audioTrackManager.stop()

    suspend fun closeRecord(){
        audioRecordManager.close()
    }

    suspend fun closeTrack(){
        audioTrackManager.reset()
    }

}
