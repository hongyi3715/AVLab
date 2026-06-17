//
// Created by 111 on 2026/6/5.
//

#pragma once


void changePcmVolume(const short *input, short *output, int count, float multiple);


void controlPcmChannel(const short *input, short *output, int count, bool left_channel, bool right_channel);
