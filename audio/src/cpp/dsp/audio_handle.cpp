//
// Created by 111 on 2026/6/6.
//


#include <cstddef>
#include <malloc.h>

/*
 * 音频音量处理
 * */
void changePcmVolume(const short* input,short* output,int count,float multiple){
    if(input == nullptr || output== nullptr) return ;

    for (int i = 0; i < count; ++i) {
        output[i] = static_cast<short>(
                input[i] * multiple
        );
    }
}

/*
 * 控制左右声道播放
 * */
void controlPcmChannel(const short* input,short* output,int count,bool left_channel,bool right_channel){
    if(input == nullptr || output== nullptr) return;

    float left_gain = left_channel ? 1.0f : 0.0f;
    float right_gain = right_channel ? 1.0f : 0.0f;
    for (int i = 0; i < count; ++i) {
        if ((i & 1) == 0) {
            // 最低位是0，说明是偶数 -> 左声道
            output[i] = static_cast<short>(input[i] * left_gain);
        } else {
            // 最低位是1，说明是奇数 -> 右声道
            output[i] = static_cast<short>(input[i] * right_gain);
        }
    }
}


/*
 * 混音
 * */



