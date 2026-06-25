package com.lq.webrtc

import android.content.Context
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RTCStats
import org.webrtc.audio.JavaAudioDeviceModule

object WebRtcUtil {

    lateinit var factory: PeerConnectionFactory

    val eglBase by lazy { EglBase.create() }

    fun init(applicationContext: Context) {
        val options = PeerConnectionFactory.InitializationOptions.builder(applicationContext)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        val audioDeviceModule = JavaAudioDeviceModule.builder(applicationContext)
            .setUseHardwareAcousticEchoCanceler(false) // 🌟 禁用硬件回音消除
            .setUseHardwareNoiseSuppressor(false)      // 🌟 禁用硬件降噪
            .createAudioDeviceModule()

        factory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory) // 确保这一步正确
            .createPeerConnectionFactory()
    }

    fun handleStats(stat: RTCStats){
        println("Stat Type:${stat.type}")
        when(stat.type){
            "remote-inbound-rtp" -> {
                // 这是对端告诉你的：你发的包它收到的情况
                println("丢包数: ${stat.members["packetsLost"]}")
                println("丢包率(jitter): ${stat.members["jitter"]}")
                println("往返时延RTT: ${stat.members["roundTripTime"]}")
                println("对应的本地outbound id: ${stat.members["localId"]}")
            }
            "outbound-rtp" -> {
                println("已发送包数: ${stat.members["packetsSent"]}")
                println("已发送字节: ${stat.members["bytesSent"]}")
                println("已编码帧数: ${stat.members["framesEncoded"]}")
                println("关键帧数: ${stat.members["keyFramesEncoded"]}")
                // 丢包率 = packetsLost / (packetsSent) 需要结合 remote-inbound-rtp 算
                println("编码格式: ${stat.members["codecId"]}")
                println("已编码帧数: ${stat.members["framesEncoded"]}")
                println("已发送字节: ${stat.members["bytesSent"]}")
                println("编码器实现: ${stat.members["encoderImplementation"]}")
            }
            "inbound-rtp" -> {
                // 如果你是接收端，这里能看到你自己收到的丢包情况
                println("收到包数: ${stat.members["packetsReceived"]}")
                println("丢包数: ${stat.members["packetsLost"]}")
                println("已解码帧数: ${stat.members["framesDecoded"]}")
                println("丢帧数: ${stat.members["framesDropped"]}")
                println("是否在等关键帧: ${stat.members["freezeCount"]}") // 卡顿次数
            }
        }
    }

}
