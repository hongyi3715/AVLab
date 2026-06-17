package com.lq.video.encode

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import com.lq.video.decode.EncodedVideoFrame
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

class Camera264Encoder {


    var encodeSurface: Surface? = null
        private set

    private var codec: MediaCodec? = null
    private var outputThread: Thread? = null

    var format: MediaFormat? = null
        private set

    @Volatile
    private var isRunning = false
    private val _stateFlow = MutableStateFlow<EncoderState>(EncoderState.IDLE)
    val stateFlow = _stateFlow.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EncoderEvent>(
        extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val eventFlow = _eventFlow.asSharedFlow()

    fun createEncoder(width: Int, height: Int, bitrate: Int) {
        codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val videoFormat = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            width, height
        ).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)      // 码率，如 2000000
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)         // 帧率
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)    // I帧间隔
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
        }

        codec?.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        encodeSurface = codec?.createInputSurface() // 这个Surface要被OpenGL用来绘制
        codec?.start()
    }




    /*
    * 开启一个线程读取编码数据
    * */
    fun startOutputThread() {
        isRunning = true
        _stateFlow.value = EncoderState.RUNNING
        outputThread = Thread {
            val bufferInfo = MediaCodec.BufferInfo()
            while (isRunning){
                val currentCodec = codec ?: break
                when(val outputIndex = currentCodec.dequeueOutputBuffer(bufferInfo, 10000)){
                    MediaCodec.INFO_TRY_AGAIN_LATER->{ continue }
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED->{
                        format = currentCodec.outputFormat
                        format?.let {
                            _eventFlow.tryEmit(EncoderEvent.Format(it))
                        }
                    }
                    else  -> {
                        if(outputIndex>=0){
                            val outputBuffer = currentCodec.getOutputBuffer(outputIndex) ?: continue
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) { //todo 本地合成mp4不需要config帧
//                                currentCodec.releaseOutputBuffer(outputIndex, false)
//                                continue
                            }
                            handleRTCEncoderData(bufferInfo,outputBuffer,outputIndex,currentCodec)
                        }
                    }
                }
            }
        }.also { it.start() }
    }


    private fun handleRTCEncoderData(bufferInfo: MediaCodec.BufferInfo,outputBuffer: ByteBuffer,outputIndex:Int,currentCodec: MediaCodec){
        try {
            outputBuffer.position(bufferInfo.offset)
            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

            val data = ByteArray(bufferInfo.size)
            outputBuffer.get(data)

            val frame = EncodedVideoFrame(
                data = data,
                ptsUs = bufferInfo.presentationTimeUs,
                flags = bufferInfo.flags
            )
            println("当前视频编码:$frame")
            _eventFlow.tryEmit(EncoderEvent.VideoFrame(frame))
            currentCodec.releaseOutputBuffer(outputIndex, false)
        } catch (e: Exception) {
            _stateFlow.value = EncoderState.ERROR(e)
        }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        _stateFlow.value = EncoderState.STOPPED

        outputThread?.join(300) //超时
        outputThread = null

        codec?.stop()
        codec?.release()

        codec = null
        encodeSurface?.release()
        encodeSurface = null
    }


}
