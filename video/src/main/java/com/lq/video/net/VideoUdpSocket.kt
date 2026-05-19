package com.lq.video.net

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object VideoUdpSocket {

    private val host = "127.0.0.1"
    private val port = 1935

    private val sendSocket = DatagramSocket()  // 不需要绑定端口
    private val receiveSocket = DatagramSocket(port)  // 接收用


    fun sendVideoPacket(packet: ByteArray) {
        val sendPacket = DatagramPacket(packet, packet.size, InetAddress.getByName(host), port)
        sendSocket.send(sendPacket)
    }
}
