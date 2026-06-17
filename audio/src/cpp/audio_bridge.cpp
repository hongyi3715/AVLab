//
// Created by 111 on 2026/6/9.
//
#include <jni.h>
#include <string>
#include "buffer/audio_engine_context.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_lq_audio_AudioJniBridge_nativeCreate(JNIEnv *env, jobject thiz,int ringBufferSize,int windowSize) {
    auto* context = new AudioEngineContext(ringBufferSize, windowSize);
    return reinterpret_cast<jlong>(context);
}

/*
 * 写入数据：直接交给上下文
 */
JNIEXPORT void JNICALL
Java_com_lq_audio_AudioJniBridge_addPlayOffset(JNIEnv *env, jobject thiz, jlong contextHandle, jlong offset) {
    auto* context = reinterpret_cast<AudioEngineContext*>(contextHandle);
    if (context != nullptr) {
        context->pushPlayOffset(static_cast<long>(offset));
    }
}


JNIEXPORT jlong JNICALL
Java_com_lq_audio_AudioJniBridge_getTimeTriggered(JNIEnv *env, jobject thiz, jlong contextHandle) {
    auto* context = reinterpret_cast<AudioEngineContext*>(contextHandle);
    if (context != nullptr) {
        return static_cast<jlong>(context->getEstimatedSize());
    }
    return 0;
}


/*
 * 销毁内存
 */
JNIEXPORT void JNICALL
Java_com_lq_audio_AudioJniBridge_nativeDestroy(JNIEnv *env, jobject thiz, jlong contextHandle) {
    auto* context = reinterpret_cast<AudioEngineContext*>(contextHandle);
    delete context; // 安全释放
}

}


