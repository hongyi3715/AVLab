package com.lq.audio.buffer



/*
* 滑动窗口+权重 预估当前缓冲量
* */
class JitterEstimator {


    private val WINDOWS_SIZE = 100 //固定窗口大小
    private val WEIGHT = 0.8 //权重
    private val window = ArrayDeque<Long>(WINDOWS_SIZE)

    private var writeIndex = 0


    fun push(pts: Long) {

    }


    fun pop(): Long {
        return 0
    }




}
