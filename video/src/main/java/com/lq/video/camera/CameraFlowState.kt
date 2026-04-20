package com.lq.video.camera

sealed class CameraFlowState {
    object Idle : CameraFlowState()

    object CameraInitializing: CameraFlowState()

    object EncoderInitializing: CameraFlowState()
    object RenderInitializing : CameraFlowState()

    object Binding: CameraFlowState()
    object PreviewRunning : CameraFlowState()

    data class Error(val exception: Exception): CameraFlowState()
}
