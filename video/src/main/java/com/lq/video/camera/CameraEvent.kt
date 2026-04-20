package com.lq.video.camera

sealed class CameraEvent {
    object InitializeCamera : CameraEvent()

    object InitializeEncoder : CameraEvent()
    object InitializeOpenGL : CameraEvent()
    object BindCamera : CameraEvent()

    object BindSuccess : CameraEvent()

    object StopPreview : CameraEvent()

    object OnRenderDone : CameraEvent()

    class OnError(val exception: Exception) : CameraEvent()
}
