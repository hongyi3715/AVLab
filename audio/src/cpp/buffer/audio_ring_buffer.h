//
// Created by 111 on 2026/6/9.
//

#pragma once


#include <atomic>
#include <vector>

class LibRingBuffer {

public:
    LibRingBuffer(int size = 256);

    bool push(long offset);

    bool pop(long &offset); // 增加一个出队方法，供计算器取数据

    int availableRead();

private:
    int capacity;
    std::vector<long> buffer_;
    std::atomic<int> head_;
    std::atomic<int> tail_;
};
