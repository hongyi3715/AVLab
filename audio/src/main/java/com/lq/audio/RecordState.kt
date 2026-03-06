package com.lq.audio

sealed class RecordState {
    data object Idle : RecordState()

    data object Recording : RecordState()

    data class Error(
        val code: Int? = null,
        val throwable: Throwable? = null,
    ) : RecordState()
}
