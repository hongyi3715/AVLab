package com.lq.audio

import java.util.concurrent.atomic.AtomicInteger


/*
* 无锁ByteArray对象池
* */
class LockFreeByteArrayPool(private val poolSize: Int, private val arraySize: Int) {

    private val top = AtomicInteger(poolSize)
    private val items = Array(poolSize) { ByteArray(arraySize) }

    fun acquire(): ByteArray {
        while (true) {
            val size = top.get()
            if (size == 0) {
                return ByteArray(arraySize)
            }
            if (top.compareAndSet(size, size - 1)) {
                return items[size - 1]
            }
        }
    }

    fun release(byteArray: ByteArray) {
        if (byteArray.size != arraySize) throw IllegalArgumentException("当前ByteArray与预设arraySize不一致")

        while (true) {
            val size = top.get()
            if (size >= poolSize) return
            items[size] = byteArray
            if (top.compareAndSet(size, size + 1)) return

        }
    }

}
