package com.lq.audio

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/*
* 环形缓冲区
* 数组做循环队列，通过取模计算下标
* */
class RingBuffer(val capacity: Int) {
    private val bufferBytes = ByteArray(capacity)
    private var available = 0

    @Volatile
    private var writeIndex = 0

    @Volatile
    private var readIndex = 0
    private val lock = ReentrantLock()
    private val notEmpty: Condition = lock.newCondition() // 非空条件（消费者等待）
    private val notFull: Condition = lock.newCondition() // 非满条件（生产者等待）

    init {
        require(capacity > 0) { "Capacity must be positive" }
    }

    fun write(
        data: ByteArray,
        offset: Int = 0,
        length: Int = 0,
    ): Int {
        val writeData = data.copyOf()
        lock.withLock {
            var remaining = writeData.size
            var currentOffset = offset

            while (remaining > 0) {
                while (available == capacity) {
                    if (!notFull.await(500, TimeUnit.MILLISECONDS)) {
                        return length - remaining
                    }
                }
                val canWrite = minOf(remaining, capacity - available)
                if (canWrite == 0) break

                performWrite(writeData, currentOffset, canWrite)

                currentOffset += canWrite
                remaining -= canWrite
                available += canWrite

                notEmpty.signal()
            }

            return length - remaining
        }
    }

    fun read(
        data: ByteArray,
        offset: Int = 0,
        length: Int = data.size,
    ): Int {
        if (length <= 0 ) return 0

        lock.withLock {
            var remaining = length
            var currentOffset = offset

            while (remaining > 0) {
                while (available == 0 ) {
                    if (!notEmpty.await(500, TimeUnit.MILLISECONDS)) {
                        return length - remaining
                    }
                }
                val canRead = minOf(remaining,  available)

                performRead(data, currentOffset, canRead)

                currentOffset += canRead
                remaining -= canRead
                available -= canRead

                notFull.signal()
            }

            return length - remaining
        }
    }

    private fun performWrite(
        data: ByteArray,
        offset: Int,
        length: Int,
    ) {
        val spaceToEnd = capacity - writeIndex
        if (length <= spaceToEnd) {
            System.arraycopy(data, offset, bufferBytes, writeIndex, length)
            writeIndex += length
        } else {
            System.arraycopy(data, offset, bufferBytes, writeIndex, spaceToEnd)

            val secondPart = length - spaceToEnd
            System.arraycopy(data, offset + spaceToEnd, bufferBytes, 0, secondPart)

            writeIndex = secondPart
        }
        if (writeIndex == capacity) {
            writeIndex = 0
        }
    }

    private fun performRead(
        dest: ByteArray,
        offset: Int,
        length: Int,
    ){
        val dataToEnd = capacity - readIndex

        if (length <= dataToEnd) {
            System.arraycopy(bufferBytes, readIndex, dest, offset, length)
            readIndex += length
        } else {
            System.arraycopy(bufferBytes, readIndex, dest, offset, dataToEnd)

            val secondPart = length - dataToEnd
            System.arraycopy(bufferBytes, 0, dest, offset + dataToEnd, secondPart)

            readIndex = secondPart
        }

        if (readIndex == capacity) {
            readIndex = 0
        }
    }
}
