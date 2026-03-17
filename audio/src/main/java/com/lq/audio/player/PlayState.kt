package com.lq.audio.player

sealed class PlayState {

    data object Idle: PlayState()

    data object Playing: PlayState()

    data object Paused: PlayState()

    data object Stopped : PlayState()
    data class Error(val throwable: Throwable): PlayState()

}
