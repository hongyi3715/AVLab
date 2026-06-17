//
// Created by 111 on 2026/6/9.
//

#include "audio_ring_buffer.h"

LibRingBuffer::LibRingBuffer(int size) : capacity(size), buffer_(size, 0), head_(0), tail_(0) {
}

bool LibRingBuffer::push(long offset) {
    int current_tail = tail_.load(std::memory_order_relaxed);
    int current_head = head_.load(std::memory_order_relaxed);
    int next_tail = (current_tail + 1) % capacity;
    if (next_tail == current_head) { // 缓冲区满，写入失败
        return false;
    }
    buffer_[current_tail] = offset;
    tail_.store(next_tail, std::memory_order_release);
    return true;
}

bool LibRingBuffer::pop(long &offset) {
    int current_head = head_.load(std::memory_order_relaxed);
    int current_tail = tail_.load(std::memory_order_acquire);
    if (current_head == current_tail) {
        return false; // 队列空
    }
    offset = buffer_[current_head];
    head_.store((current_head + 1) % capacity, std::memory_order_release);
    return true;
}

int LibRingBuffer::availableRead() {
    int current_tail = tail_.load(std::memory_order_relaxed);
    int current_head = head_.load(std::memory_order_relaxed);
    if (current_tail >= current_head) {
        return current_tail - current_head;
    } else {
        return capacity - current_head + current_tail;
    }
}
