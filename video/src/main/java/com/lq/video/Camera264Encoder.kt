package com.lq.video

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface

class Camera264Encoder {


     var encodeSurface : Surface?=null
         private set


    private fun createEncoder(width:Int,height:Int,bitrate: Int){
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val format = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            width, height
        ).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)      // 码率，如 2000000
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)         // 帧率
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)    // I帧间隔
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        }

        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        encodeSurface = codec.createInputSurface() // 这个Surface要被OpenGL用来绘制
        codec.start()
    }


    /*
    * 开启一个线程读取编码数据
    * */



}
