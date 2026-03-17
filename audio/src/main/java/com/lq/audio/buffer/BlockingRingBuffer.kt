package com.lq.audio.buffer

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BlockingRingBuffer(private val capacity: Int) {

    private val buffer = ByteArray(capacity)

    private val lock = ReentrantLock()

    @Volatile
    private var writePosition: Int = 0

    private var readPosition: Int = 0

    @Volatile
    private var available: Int = 0

    private val notEmpty: Condition = lock.newCondition()

    private val notFull: Condition = lock.newCondition()

    fun write(data: ByteArray, offset: Int = 0, length: Int = data.size): Int {
        lock.withLock {
            var remaining = length
            var totalWritten = 0
            var currentOffset = offset
            while (remaining > 0) {
                while (available == capacity) {
                    if (!notFull.await(500, TimeUnit.MILLISECONDS)) {
                        return totalWritten
                    }
                }
                val canWrite = minOf(remaining, capacity - available)

                val toEnd = capacity - writePosition
                if (canWrite <= toEnd) {
                    System.arraycopy(data, currentOffset, buffer, writePosition, canWrite)
                } else {
                    System.arraycopy(data, currentOffset, buffer, writePosition, toEnd)
                    val secondPart  = canWrite - toEnd
                    System.arraycopy(data, currentOffset + toEnd, buffer, 0, secondPart)
                }

                writePosition = (writePosition + canWrite) % capacity
                available += canWrite
                totalWritten += canWrite
                remaining -= canWrite
                currentOffset += canWrite
                notEmpty.signal()

            }
            return totalWritten
        }
    }

    fun read(data: ByteArray, offset: Int = 0, length: Int = data.size): Int {
        lock.withLock {
            var remaining = length
            var totalRead = 0
            var currentOffset = offset
            while (remaining > 0) {
                while (available == 0) {
                    if (!notEmpty.await(500, TimeUnit.MILLISECONDS)) {
                        return totalRead
                    }
                }
                val canRead = minOf(remaining, available)

                val toEnd = capacity - readPosition
                if (canRead <= toEnd) {
                    System.arraycopy(buffer, readPosition, data, currentOffset, canRead)
                } else {
                    System.arraycopy(buffer, readPosition, data, currentOffset, toEnd)
                    val secondPart  = canRead - toEnd
                    System.arraycopy(buffer, 0, data, currentOffset + toEnd, secondPart )
                }
                readPosition = (readPosition + canRead) % capacity
                available -= canRead
                remaining -= canRead
                totalRead += canRead
                currentOffset += canRead
                notFull.signal()
            }

            return totalRead
        }
    }


}
