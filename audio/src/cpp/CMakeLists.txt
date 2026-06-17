cmake_minimum_required(VERSION 3.22.1)


project("audio_native")


add_library(
        audio_native
        SHARED
        audio_bridge.cpp
        dsp/audio_handle.cpp
        buffer/audio_estimator.cpp
        buffer/audio_ring_buffer.cpp
)


find_library(
    log-lib
    log
)


target_link_libraries(
    audio_native
    ${log-lib}
)
