package com.lq.audio

import org.junit.Test
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class AudioBufferTest {
    private val audioBuffer = NewRingBuffer(190)

    @Test
    fun `环形缓冲区测试`() {
        val latch = CountDownLatch(2)
        val totalData = 1500
        thread(name = "Producer") {
            var writtenTotal = 0
            val data = ByteArray(40) { it.toByte() } // 每次产出 10 字节
            while (writtenTotal < totalData) {
                val n = audioBuffer.write(data)
                if (n > 0) {
                    writtenTotal += n
                    println("[写] 写入 $n 字节，进度: $writtenTotal/$totalData")
                }
                Thread.sleep(400)
            }
            latch.countDown()
        }
        thread(name = "Consumer") {
            var readTotal = 0
            val out = ByteArray(55) // 每次想读 15 字节
            while (readTotal < totalData) {
                val n = audioBuffer.read(out)
                if (n > 0) {
                    readTotal += n
                    println("[读] 读取 $n 字节，累计读出: $readTotal")
                }
                Thread.sleep(80)
            }
            latch.countDown()
        }
        latch.await()
        println("测试结束")
    }
}
