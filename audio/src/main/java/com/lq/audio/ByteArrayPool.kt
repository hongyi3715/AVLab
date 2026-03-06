package com.lq.audio

import java.util.concurrent.ArrayBlockingQueue

class ByteArrayPool(private val poolSize: Int, private val arraySize: Int) {

    private val pool = ArrayBlockingQueue<ByteArray>(poolSize)

    init {
        repeat(poolSize) {
            pool.put(ByteArray(arraySize))
        }

    }


    fun acquire(): ByteArray {
        return pool.poll() ?: ByteArray(arraySize)
    }

    fun release(array: ByteArray) {
        if (array.size == arraySize) {
            pool.offer(array)
        }
    }
}
