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

    fun sendVideoPacket(packet: VideoPacket){
        val buffer = ByteBuffer.allocate(
            4 + 4 + 4 + 4 + 8 + 4 + packet.payload.size
        )
        buffer.putInt(packet.header.seq)
        buffer.putInt(packet.header.frameId)
        buffer.putInt(packet.header.fragIndex)
        buffer.putInt(packet.header.fragCount)
        buffer.putLong(createTimeZoom(packet.header))
        buffer.putInt(packet.header.flags)
        buffer.put(packet.payload)
        val packetBytes = buffer.array()
        try {
            val address = InetAddress.getByName(host)
            val sendPacket = DatagramPacket(packetBytes, packetBytes.size, address, sendPort)
            sendSocket.send(sendPacket)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private var baseTime = 0L
    private fun createTimeZoom(packetHeader: VideoPacketHeader): Long{
        val copiedBaseTime = baseTime
        val timeStampUs = packetHeader.timestampUs
        if(copiedBaseTime == 0L ){
            baseTime = timeStampUs
            return 0
        }
        return timeStampUs - copiedBaseTime
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
