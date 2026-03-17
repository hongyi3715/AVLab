package com.lq.audio.buffer

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min

class FrameAlignedRingBuffer(
    private val frameSize: Int,
    capacity: Int
) {
    private val buffer = ByteArray(capacity)
    private val lock = ReentrantLock()  // 加锁保证线程安全

    private var writeIndex = 0
    private var readIndex = 0
    private var availableBytes = 0  // 可用字节数

    private val capacity = capacity
    private val maxFrames = capacity / frameSize

    // 预分配对象池
    private val framePool = ArrayDeque<ByteArray>()

    init {
        require(capacity % frameSize == 0) {
            "capacity must align with frameSize"
        }
        // 预创建一些帧对象
        repeat(10) {
            framePool.addLast(ByteArray(frameSize))
        }
    }

    /**
     * 写入数据（字节级）
     */
    fun write(data: ByteArray, offset: Int = 0, length: Int = data.size): Int {
        lock.withLock {
            val free = capacity - availableBytes
            if (free <= 0) return 0

            val writeSize = min(length, free)
            val toEnd = capacity - writeIndex

            if (writeSize <= toEnd) {
                System.arraycopy(data, offset, buffer, writeIndex, writeSize)
            } else {
                System.arraycopy(data, offset, buffer, writeIndex, toEnd)
                System.arraycopy(data, offset + toEnd, buffer, 0, writeSize - toEnd)
            }

            writeIndex = (writeIndex + writeSize) % capacity
            availableBytes += writeSize

            return writeSize
        }
    }

    /**
     * 读取一帧（使用外部传入的数组，避免创建）
     * @return true 读取成功，false 数据不足
     */
    fun readFrame(frame: ByteArray, offset: Int = 0): Boolean {
        require(frame.size - offset >= frameSize) {
            "Frame array too small"
        }

        lock.withLock {
            // 检查是否有完整的一帧
            if (availableBytes < frameSize) {
                return false
            }

            val toEnd = capacity - readIndex

            if (frameSize <= toEnd) {
                System.arraycopy(buffer, readIndex, frame, offset, frameSize)
            } else {
                System.arraycopy(buffer, readIndex, frame, offset, toEnd)
                System.arraycopy(buffer, 0, frame, offset + toEnd, frameSize - toEnd)
            }

            readIndex = (readIndex + frameSize) % capacity
            availableBytes -= frameSize

            return true
        }
    }

    /**
     * 读取一帧（使用对象池，减少GC）
     */
    fun readFrame(): ByteArray? {
        lock.withLock {
            if (availableBytes < frameSize) {
                return null
            }

            // 从池中获取帧对象
            val frame = framePool.removeFirstOrNull() ?: ByteArray(frameSize)

            val toEnd = capacity - readIndex

            if (frameSize <= toEnd) {
                System.arraycopy(buffer, readIndex, frame, 0, frameSize)
            } else {
                System.arraycopy(buffer, readIndex, frame, 0, toEnd)
                System.arraycopy(buffer, 0, frame, toEnd, frameSize - toEnd)
            }

            readIndex = (readIndex + frameSize) % capacity
            availableBytes -= frameSize

            return frame
        }
    }

    /**
     * 归还帧到对象池
     */
    fun recycleFrame(frame: ByteArray) {
        if (frame.size == frameSize && framePool.size < maxFrames) {
            framePool.addLast(frame)
        }
    }

    /**
     * 批量读取帧
     */
    fun readFrames(maxFrames: Int, outFrames: MutableList<ByteArray>): Int {
        var count = 0
        lock.withLock {
            while (count < maxFrames && availableBytes >= frameSize) {
                val frame = framePool.removeFirstOrNull() ?: ByteArray(frameSize)

                val toEnd = capacity - readIndex
                if (frameSize <= toEnd) {
                    System.arraycopy(buffer, readIndex, frame, 0, frameSize)
                } else {
                    System.arraycopy(buffer, readIndex, frame, 0, toEnd)
                    System.arraycopy(buffer, 0, frame, toEnd, frameSize - toEnd)
                }

                readIndex = (readIndex + frameSize) % capacity
                availableBytes -= frameSize

                outFrames.add(frame)
                count++
            }
        }
        return count
    }

    /**
     * 获取可读帧数
     */
    fun availableFrames(): Int = lock.withLock { availableBytes / frameSize }

    /**
     * 获取剩余空间（字节）
     */
    fun remainingBytes(): Int = lock.withLock { capacity - availableBytes }

    /**
     * 是否为空
     */
    fun isEmpty(): Boolean = lock.withLock { availableBytes == 0 }

    /**
     * 是否已满
     */
    fun isFull(): Boolean = lock.withLock { availableBytes == capacity }

    /**
     * 清空缓冲区
     */
    fun clear() {
        lock.withLock {
            writeIndex = 0
            readIndex = 0
            availableBytes = 0
        }
    }

    /**
     * 获取统计信息
     */
    fun getStats(): String = lock.withLock {
        """
        帧对齐缓冲区状态:
          容量: ${capacity / frameSize} 帧
          帧大小: $frameSize 字节
          当前帧数: ${availableBytes / frameSize}
          当前字节: $availableBytes/$capacity
          写指针: $writeIndex
          读指针: $readIndex
        """.trimIndent()
    }
}
