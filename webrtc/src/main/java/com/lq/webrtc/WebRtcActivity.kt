package com.lq.webrtc

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.lq.webrtc.WebRtcUtil.factory
import org.webrtc.*

class WebRtcActivity : AppCompatActivity(), SignalingClient.Listener {

    private var signalingClient: SignalingClient? = null
    private var peerConnection: PeerConnection? = null

    val setSdpObserver = object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) {}
        override fun onSetSuccess() {
            println("set description success")
        }

        override fun onCreateFailure(error: String?) {
            println("create sdp fail:$error")
        }

        override fun onSetFailure(error: String?) {
            println("set fail: $error")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_rtc)

        val surfaceRender = findViewById<SurfaceViewRenderer>(R.id.surfaceRender)
        surfaceRender.init(WebRtcUtil.eglBase.eglBaseContext, null)
        surfaceRender.setEnableHardwareScaler(true)
        surfaceRender.setMirror(true)

        val answerSurfaceRender = findViewById<SurfaceViewRenderer>(R.id.answerSurfaceRender)
        answerSurfaceRender.init(WebRtcUtil.eglBase.eglBaseContext, null)
        answerSurfaceRender.setEnableHardwareScaler(true)
        answerSurfaceRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        answerSurfaceRender.setZOrderMediaOverlay(true)

        val enumerator = Camera2Enumerator(this)
        val cameraName = enumerator.deviceNames.first { enumerator.isFrontFacing(it) }
        val capturer = enumerator.createCapturer(cameraName, null)

        val surfaceTextureHelper = SurfaceTextureHelper.create(
            "capture-thread", WebRtcUtil.eglBase.eglBaseContext
        )

        val videoSource = factory.createVideoSource(false)
        capturer.initialize(surfaceTextureHelper, this, videoSource.capturerObserver)

        val videoTrack = factory.createVideoTrack("video0", videoSource)
        videoTrack.addSink(surfaceRender)
        capturer.startCapture(1280, 720, 30)


        val audioSource = factory.createAudioSource(MediaConstraints())
        val audioTrack = factory.createAudioTrack("audio0", audioSource)
        audioTrack.setEnabled(true)

        signalingClient = SignalingClient("ws://192.168.76.252:8080", "room123", this)
        signalingClient?.connect()

        peerConnection = createPeerConnection(answerSurfaceRender) { candidate ->
            signalingClient?.sendIceCandidate(candidate)
        }

        val transceiverInit = RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_RECV)
        peerConnection?.addTransceiver(videoTrack, transceiverInit)
        peerConnection?.addTransceiver(audioTrack, transceiverInit)

        findViewById<Button>(R.id.sendBt).apply {
            visibility = View.VISIBLE
            setOnClickListener {
                createAndSendOffer()
            }
        }
    }

    private fun createPeerConnection(
        renderTarget: SurfaceViewRenderer?,
        onLocalIceCandidate: (IceCandidate) -> Unit
    ): PeerConnection? {
        val rtcConfig = PeerConnection.RTCConfiguration(emptyList())
        return factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
                println("SignalingState: $newState")
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                println("IceConnectionState: $newState")
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
                println("Gathering=$newState")
                if (newState == PeerConnection.IceGatheringState.COMPLETE) {
                    println("Gathering Full Sdp")
                }
            }

            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate ?: return
                println("onIceCandidate:$candidate")
                onLocalIceCandidate(candidate) // 转发给对方
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate?>?) {}

            override fun onTrack(transceiver: RtpTransceiver?) {
                super.onTrack(transceiver)
                val track = transceiver?.receiver?.track()
                println("onTrack ${track?.kind()}")

                if (track is VideoTrack && renderTarget != null) {
                    runOnUiThread {
                        track.addSink(renderTarget)
                    }
                }
            }

            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dataChannel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
        })
    }

    private fun createAndSendOffer() {
        peerConnection?.createOffer(object : SdpObserver by setSdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp ?: return
                peerConnection?.setLocalDescription(setSdpObserver, sdp)
                signalingClient?.sendOffer(sdp)
            }
        }, MediaConstraints())
    }

    override fun onRemoteOffer(sdp: SessionDescription) {
        println("收到对方的 Offer 了！")
        //作为answer
        peerConnection?.setRemoteDescription(setSdpObserver, sdp)
        peerConnection?.createAnswer(object : SdpObserver by setSdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp ?: return
                peerConnection?.setLocalDescription(setSdpObserver, sdp)
                signalingClient?.sendAnswer(sdp)
            }
        }, MediaConstraints())
    }

    override fun onRemoteAnswer(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(setSdpObserver, sdp)
    }

    override fun onRemoteIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    override fun onConnected() {
    }

    override fun onDisconnected() {
    }


    override fun onDestroy() {
        super.onDestroy()
        signalingClient?.close()
    }
}
