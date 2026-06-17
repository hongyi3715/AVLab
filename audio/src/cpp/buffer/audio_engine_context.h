//
// Created by 111 on 2026/6/10.
//

#pragma once


#include "audio_estimator.h"
#include "audio_ring_buffer.h"

class AudioEngineContext {
public:
    AudioEngineContext(int ringBufferSize = 256, int windowSize = 100)
            : ringBuffer_(ringBufferSize), estimator_(windowSize) {

    }

    ~AudioEngineContext() = default;

    void pushPlayOffset(long offset) {
        ringBuffer_.push(offset);
    }

    long getEstimatedSize() {
        return estimator_.executeEstimate(ringBuffer_);
    }

private:
    LibRingBuffer ringBuffer_;     // 无锁蓄水池
    AudioEstimator estimator_;    // 滑窗预估器
};
