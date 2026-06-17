//
// Created by 111 on 2026/6/9.
//

#include "audio_estimator.h"
#include "audio_ring_buffer.h"

AudioEstimator::AudioEstimator(int windowSize)
        : windowSize_(windowSize),
          window_(windowSize, 0),
          win_write_ptr_(0),
          current_sum_(0),
          last_estimate_value_(0) {
}

void AudioEstimator::slideWindow(long newOffset) {
    // 1. 动态减去即将被覆盖掉的那个“100步以前”的老数据
    current_sum_ -= window_[win_write_ptr_];

    // 2. 将最新的数据覆盖写入这个位置
    window_[win_write_ptr_] = newOffset;

    // 3. 加上这个新数据
    current_sum_ += newOffset;

    // 4. 移动覆写指针（到达 100 时自动回绕到 0，实现环形复用）
    win_write_ptr_ = (win_write_ptr_ + 1) % windowSize_;
}



/*
 * 什么时候取？怎么取？就在这个接口实现！
 * 通常在你的 JNI getTime() 方法对应的 C++ 函数中调用此接口。
 */
long AudioEstimator::executeEstimate(LibRingBuffer &ringBuffer) {
    long tempOffset = 0;
    bool hasNewData = false;

    // 1. 开闸放水！利用 while 循环，把无锁队列（蓄水池）里攒的所有偏移量一次性抽干
    while (ringBuffer.pop(tempOffset)) {
        hasNewData = true;

        // 2. 每抽出来一个，就塞进滑动窗口，推动双指针向前滑一步
        slideWindow(tempOffset);
    }

    // 3. 如果这次确实抽到了新数据，重新计算权重预估值
    if (hasNewData) {
        // 这里采用简单的均值预估（你可以根据需要在这里乘以权重系数，实现真正的加权预估）
        last_estimate_value_ = current_sum_ / windowSize_;
    }

    // 4. 如果没有新数据，说明音频卡顿了或者没更新，直接返回上一次缓存的预估值
    return last_estimate_value_;
}



