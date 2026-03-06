package com.lq.audio

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class NewRingBuffer(private val capacity: Int,frameRate:Int = 1024) {

    private val buffer = ByteArray(capacity)

    private val lock = ReentrantLock()

    @Volatile
    private var writePosition: Int = 0

    @Volatile
    private var readPosition: Int = 0

    @Volatile
    private var available: Int = 0


    fun write(data: ByteArray, offset: Int = 0, length: Int = data.size): Int {
        if (length <= 0) return 0
        lock.withLock {

            val freeSpace = capacity - available //剩余的可用长度
            if (freeSpace == 0) return 0

            val writeLength = minOf(length, freeSpace)

            val toEnd = capacity - writePosition
            val firstPart = minOf(toEnd, writeLength)
            System.arraycopy(data, offset, buffer, writePosition, firstPart) //复制第一段

            if (firstPart < writeLength) {
                System.arraycopy(data, offset + firstPart, buffer, toEnd, writeLength - firstPart)
            }

            writePosition = (writePosition + writeLength) % capacity
            available += writeLength
            return writeLength
        }
    }


    fun read(data: ByteArray, offset: Int = 0, length: Int = data.size): Int {
        if (length <= 0 || offset < 0) return 0
        lock.withLock {
            if (available == 0) return 0
            val actualLength = minOf(data.size - offset,length)
            val readLength = minOf(actualLength,available)
            val toEnd = capacity - readPosition
            val firstPart = minOf(toEnd,readLength)
            System.arraycopy(buffer,readPosition,data,offset,firstPart)

            if(firstPart<readLength){
                System.arraycopy(buffer,0,data,offset+firstPart,readLength-firstPart)
            }
            readPosition = (readPosition + readLength) % capacity
            available -= readLength
            return readLength
        }
    }

}
