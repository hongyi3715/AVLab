package com.lq.video.net

import com.lq.video.decode.VideoPacket
import com.lq.video.decode.VideoPacketHeader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer

object VideoUdpSocket {

     var host = "127.0.0.1"
     var sendPort = 1935
    private const val receivePort = 1935

    private val sendSocket = DatagramSocket()  // 不需要绑定端口
    private val receiveSocket = DatagramSocket(receivePort).apply {
        receiveBufferSize = 4 * 1024 * 1024
    }  // 接收用
    @Volatile
    private var isRunning = true

    fun sendVideoPacket(packet: ByteArray) {
        try {
            val address = InetAddress.getByName(host)
            val sendPacket = DatagramPacket(packet, packet.size, address, sendPort)
            println("发送UDP -> ${address.hostAddress}:$sendPort len=${packet.size}")
            sendSocket.send(sendPacket)
        } catch (t: Throwable) {
            println("发送异常: ${t.message}")
            t.printStackTrace()
        }
    }


    fun startReceivePacket(callback:(VideoPacket)-> Unit){
        isRunning = true
        println("准备监听 receivePort=$receivePort")
        Thread {
            try {
                println("开始监听 receivePort=$receivePort, thread=${Thread.currentThread().name}")
                while (isRunning) {
                    val packet = DatagramPacket(ByteArray(1500), 1500)
                    receiveSocket.receive(packet)
//                    println("收到原始UDP from=${packet.address.hostAddress}:${packet.port}, len=${packet.length}")

                    val byteBuffer = ByteBuffer.wrap(packet.data, 0, packet.length)
                    val seq = byteBuffer.int
                    val frameId = byteBuffer.int
                    val packetIndex = byteBuffer.int
                    val packageCount = byteBuffer.int
                    val ptsUs = byteBuffer.long
                    val flags = byteBuffer.int
                    val videoData = ByteArray(packet.length - 28)
                    byteBuffer.get(videoData)

//                    println("解析UDP seq=$seq frameId=$frameId index=$packetIndex/$packageCount flags=$flags")

                    val videoHeader = VideoPacketHeader(seq, ptsUs, frameId, packetIndex, packageCount, flags)
                    val videoPacket = VideoPacket(videoHeader, videoData)
                    callback(videoPacket)
                }
            }catch (e: SocketTimeoutException) {
                println("等待UDP中 port=$receivePort")
            } catch (t: Throwable) {
                println("接收异常: ${t.message}")
                t.printStackTrace()
            }
        }.start()
    }

    fun stopReceivePacket(){
        isRunning = false
        receiveSocket.close()
    }


}
