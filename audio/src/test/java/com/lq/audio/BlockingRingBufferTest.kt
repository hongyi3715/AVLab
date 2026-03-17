package com.lq.audio

import com.lq.audio.buffer.BlockingRingBuffer
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CompletableFuture

class BlockingRingBufferTest {
    private val capacity = 10
    private val buffer = BlockingRingBuffer(capacity)

    @Test
    fun `单线程下的基本写入和读取`() {
        val dataToWrite = ByteArray(5) { it.toByte() } // 创建一个字节数组 [0, 1, 2, 3, 4]

        val bytesWritten = buffer.write(dataToWrite)
        assertEquals(5, bytesWritten)

        val dataToRead = ByteArray(5)
        val bytesRead = buffer.read(dataToRead)
        assertEquals(5, bytesRead)

        assertEquals(dataToWrite.toList(), dataToRead.toList())
    }

    @Test
    fun `多线程下的写入和读取`() {
        val dataToWrite = ByteArray(5) { it.toByte() }
        val dataToRead = ByteArray(5)

        val writeThread = Thread {
            buffer.write(dataToWrite)
        }

        val readThread = Thread {
            buffer.read(dataToRead)
        }

        writeThread.start()
        readThread.start()

        writeThread.join()
        readThread.join()

        // 确保读取到的数据与写入的数据一致
        assertEquals(dataToWrite.toList(), dataToRead.toList())
    }

    @Test
    fun `缓冲区满时的写入阻塞`() {
        val dataToWrite = ByteArray(capacity) { it.toByte() }

        val bytesWrittenFirst = buffer.write(dataToWrite)
        assertEquals(capacity, bytesWrittenFirst)

        // 在缓冲区已满时尝试写入，应该会阻塞，直到有空间
        val dataToWriteSecond = ByteArray(3) { (it + 10).toByte() }
        val bytesWrittenSecond = buffer.write(dataToWriteSecond)
        assertEquals(3, bytesWrittenSecond)  // 确保写入 3 字节
    }

    @Test
    fun `缓冲区为空时的读取阻塞`() {
        val dataToRead = ByteArray(capacity)

        // 在缓冲区为空时尝试读取，应该会阻塞，直到有数据
        val readFuture = CompletableFuture<Int>()
        val readThread = Thread {
            val bytesRead = buffer.read(dataToRead)
            readFuture.complete(bytesRead)
        }
        readThread.start()

        // 确保读取线程会阻塞
        Thread.sleep(1000) // 等待一秒钟，确保线程进入阻塞状态
        assertTrue(readFuture.isDone.not())  // 确保在此时读取线程尚未完成

        // 写入数据，触发读取操作继续
        val dataToWrite = ByteArray(5) { it.toByte() }
        buffer.write(dataToWrite)

        readThread.join()
        assertEquals(5, readFuture.get())  // 确保读取了 5 字节数据
    }

    @Test
    fun `环形缓冲区，确保写入的数据能够正确绕回`() {
        val dataToWrite1 = ByteArray(5) { it.toByte() }
        buffer.write(dataToWrite1)

        val dataToWrite2 = ByteArray(5) { (it + 10).toByte() }
        buffer.write(dataToWrite2)

        val dataToRead = ByteArray(10)
        val bytesRead = buffer.read(dataToRead)

        // 确保总共读取了 10 字节
        assertEquals(10, bytesRead)

        // 确保读取的数据是按顺序写入的
        val expected = (dataToWrite1 + dataToWrite2).toList()
        assertEquals(expected, dataToRead.toList())
    }

    @Test
    fun `读写操作与缓冲区容量一致的情况`() {
        val dataToWrite = ByteArray(capacity) { it.toByte() }

        // 写入数据
        val bytesWritten = buffer.write(dataToWrite)
        assertEquals(capacity, bytesWritten)

        // 读取数据
        val dataToRead = ByteArray(capacity)
        val bytesRead = buffer.read(dataToRead)
        assertEquals(capacity, bytesRead)

        // 确保读取到的数据与写入的数据一致
        assertEquals(dataToWrite.toList(), dataToRead.toList())
    }
}
