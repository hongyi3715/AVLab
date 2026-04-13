package com.lq.video

import android.media.MediaCodec
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class CameraRecorder(val coder: Camera264Encoder) {

    private var fos: FileOutputStream? = null
    private var outputStream: BufferedOutputStream? = null

    // 保存 SPS/PPS
    private var sps: ByteArray? = null
    private var pps: ByteArray? = null

    // 开始录制时打开一次
    fun startRecording(outputFile: File) {
        outputFile.parentFile?.mkdirs()
        fos = FileOutputStream(outputFile, false)
        outputStream = BufferedOutputStream(fos, 1024 * 64) // 64KB 缓冲

        coder.startOutputThread(object : Camera264Encoder.AudioBytesMediaCodeCallback {
            override fun onEncodedData(data: ByteArray, info: MediaCodec.BufferInfo) {
                val isKeyFrame = info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0

                if (sps == null) {
                    val format = coder.format
                    sps = format?.getByteBuffer("csd-0")?.toByteArray()
                    pps = format?.getByteBuffer("csd-1")?.toByteArray()
                    println("SPS=${sps?.take(8)?.joinToString(" ") { "%02X".format(it) }}")
                    println("PPS=${pps?.take(8)?.joinToString(" ") { "%02X".format(it) }}")
                }
                // 打印每帧头部字节，看格式是否正确
                val header = data.take(8).joinToString(" ") { "%02X".format(it) }
                println("isKeyFrame=$isKeyFrame size=${data.size} header=$[$header]")
                if (isKeyFrame) {
                    // I帧前先写 SPS + PPS
                    sps?.let { outputStream?.write(it) }
                    pps?.let { outputStream?.write(it) }
                }

                writeToFile(data)
            }
        })
    }

    private fun writeToFile(bytes: ByteArray) {
        try {
            outputStream?.write(bytes)
            // 不用每次 flush，BufferedOutputStream 会自动管理
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun ByteBuffer.toByteArray(): ByteArray {
        rewind()  // ← 加这一行！
        val copy = ByteArray(remaining())  // remaining = limit - position
        get(copy)
        return copy
    }

    // 结束录制时关闭
    fun stopRecording() {
        coder.stop()
        outputStream?.flush()
        outputStream?.close()
        fos?.close()
        outputStream = null
        fos = null
    }
}
