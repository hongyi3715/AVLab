package com.lq.audio.net

import com.lq.audio.AudioPacket
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

object UdpSocket {

    private val host = "127.0.0.1"
    private val port = 1937
    private val sendSocket = DatagramSocket()  // 不需要绑定端口
    private val receiveSocket = DatagramSocket(port)  // 接收用

    @Volatile
    private var isRunning = true

    interface OnPacketListener{
        fun onPacket(audioPacket: AudioPacket)
    }

    var onPacketListener: OnPacketListener?=null



    fun sendAudioPacket(audioPacket: AudioPacket) {
        val buffer = ByteBuffer.allocate(4 + 8 + audioPacket.payload.size)
        buffer.putInt(audioPacket.seq)
        buffer.putLong(audioPacket.timestamp)
        buffer.put(audioPacket.payload)
        val packet = DatagramPacket(
            buffer.array(), buffer.position(), InetAddress.getByName(host), port
        )
        sendSocket.send(packet)
    }


    fun startReceiverAudioPacket() {
        Thread {
            while (isRunning) {
                val packet = DatagramPacket(ByteArray(1500), 1500)
                receiveSocket.receive(packet)
                val byteBuffer = ByteBuffer.wrap(packet.data, 0, packet.length)
                val seq = byteBuffer.int
                val timestamp = byteBuffer.long
                val aacData = ByteArray(packet.length - 12)
                byteBuffer.get(aacData)
                val audioPacket = AudioPacket(seq, timestamp, aacData)
                onPacketListener?.onPacket(audioPacket)
            }
        }.start()
    }


    fun stop() {
        isRunning = false
        receiveSocket.close()
    }


}
