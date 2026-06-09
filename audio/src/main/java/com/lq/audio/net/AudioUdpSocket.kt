package com.lq.audio.net

import android.os.SystemClock
import com.lq.audio.data.AudioPacket
import com.lq.audio.data.AudioTrace
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

object AudioUdpSocket {

    var host = "127.0.0.1"
    private val port = 1937
    private val sendSocket = DatagramSocket()  // 不需要绑定端口
    private val receiveSocket = DatagramSocket(port)  // 接收用

    @Volatile
    private var isRunning = true

    //todo send Trace
    fun sendAudioPacket(audioPacket: AudioPacket) {
        val buffer = ByteBuffer.allocate(4 + 8 + audioPacket.payload.size)
        buffer.putInt(audioPacket.seq)
        buffer.putLong(audioPacket.timestamp)
        buffer.put(audioPacket.payload)
        val address = InetAddress.getByName(host)

        val packet = DatagramPacket(
            buffer.array(), buffer.position(), address, port
        )
        audioPacket.trace?.sendTime = SystemClock.elapsedRealtime()
        println("发送AudioUDP -> ${address.hostAddress}:$port len=${audioPacket.trace}")

        try {
            sendSocket.send(packet)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }


    //todo receive Trace
    fun startReceiverAudioPacket(callback:(AudioPacket)->Unit) {
        Thread {
            while (isRunning) {
                val packet = DatagramPacket(ByteArray(1500), 1500)
                receiveSocket.receive(packet)
                val byteBuffer = ByteBuffer.wrap(packet.data, 0, packet.length)
                val seq = byteBuffer.int
                val timestamp = byteBuffer.long
                val aacData = ByteArray(packet.length - 12)
                byteBuffer.get(aacData)
                val audioPacket = AudioPacket(seq, timestamp, aacData, trace = AudioTrace(seq, receiveTime = SystemClock.elapsedRealtime()))
                callback(audioPacket)
            }
        }.start()
    }


    fun stop() {
        isRunning = false
        receiveSocket.close()
    }


}
