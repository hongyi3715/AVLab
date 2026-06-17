//
// Created by 111 on 2026/6/9.
//

#pragma once

#include <atomic>
#include "audio_ring_buffer.h"

class AudioEstimator {

public:
    AudioEstimator(int windowSize = 100);

    long executeEstimate(LibRingBuffer &ringBuffer);

private:
    void slideWindow(long newOffset);

    int windowSize_;              // 窗口长度（100）
    std::vector<long> window_{};    // 长度固定为 100 的普通历史记录数组
    int win_write_ptr_;           // 覆写双指针：指向当前窗口最老的数据

    long current_sum_;            // 窗口内所有数据的总和，用于 O(1) 增量更新
    long last_estimate_value_;    // 缓存上一次算出来的预估值
};

