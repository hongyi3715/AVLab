package com.lq.audio

class AudioJniBridge(ringBufferSize:Int = 256,windowSize:Int = 100) {

    private var nativeContextHandle: Long = 0

    init {
        System.loadLibrary("audio_native")

        nativeContextHandle = nativeCreate(ringBufferSize,windowSize)
    }

    fun addPlayOffset(offset:Long){
        if (nativeContextHandle != 0L) {
            addPlayOffset(nativeContextHandle, offset)
        }
    }

    fun getTimeTriggered(): Long {
        return if (nativeContextHandle != 0L) {
            getTimeTriggered(nativeContextHandle)
        } else {
            0L
        }
    }


    fun release() {
        if (nativeContextHandle != 0L) {
            nativeDestroy(nativeContextHandle)
            nativeContextHandle = 0L
        }
    }

    private external fun nativeCreate(ringBufferSize:Int,windowSize:Int): Long
    private external fun addPlayOffset(handle: Long, offset: Long)
    private external fun getTimeTriggered(handle: Long): Long
    private external fun nativeDestroy(handle: Long)
}
