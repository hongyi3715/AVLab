package com.lq.video.decode

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class H264Decoder {

    private var codec: MediaCodec? = null

    @Volatile
    private var isRunning = false

    private val _dataFlow = MutableSharedFlow<ByteArray>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val dataFlow = _dataFlow.asSharedFlow()

    fun start(
        scope: CoroutineScope,
        width: Int = 1200,
        height: Int = 1600,
        csd0: ByteArray? = null,
        csd1: ByteArray? = null
    ) {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        csd0?.let { format.setByteBuffer("csd-0", ByteBuffer.wrap(it)) }
        csd1?.let { format.setByteBuffer("csd-1", ByteBuffer.wrap(it)) }

        codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            configure(format, null, null, 0)
            start()
        }
        startDecodeLoop(scope)
    }

    fun decode(data: ByteArray, ptsUs: Long) {
        val codec = codec ?: return

        val inputIndex = codec.dequeueInputBuffer(10000)
        if (inputIndex < 0) return

        val inputBuffer = codec.getInputBuffer(inputIndex) ?: return
        inputBuffer.clear()
        inputBuffer.put(data)
        codec.queueInputBuffer(inputIndex, 0, data.size, ptsUs, 0)
    }

    private fun startDecodeLoop(scope: CoroutineScope) = scope.launch(Dispatchers.IO){
        isRunning = true
        val info = MediaCodec.BufferInfo()
        while (isRunning && codec != null) {
            val outputIndex = codec!!.dequeueOutputBuffer(info, 0)
            when{
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> continue
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                outputIndex >= 0 -> {
                    val buffer = codec?.getOutputBuffer(outputIndex) ?: continue
                    val yuv = ByteArray(info.size)
                    buffer.get(yuv)

                    _dataFlow.tryEmit(yuv) //发送yuv数据出去

                    codec?.releaseOutputBuffer(outputIndex, false)
                }
            }
        }

    }

    fun stop() {
        isRunning = false
        codec?.stop()
        codec?.release()
        codec = null
    }
}
