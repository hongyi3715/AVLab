package com.lq.video

import android.view.TextureView
import androidx.lifecycle.LifecycleOwner


sealed class CameraFlowState {

    data object Idle: CameraFlowState()

    data object Ready: CameraFlowState()

    data class Preview(val preview: TextureView,val lifecycleOwner: LifecycleOwner): CameraFlowState()

    data class Encode(val screen:Any): CameraFlowState()

    data class Error(val exception: Exception): CameraFlowState()


}
