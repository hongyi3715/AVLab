package com.lq.webrtc

import okhttp3.*
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.concurrent.TimeUnit

class SignalingClient(
    private val serverUrl: String,   // 例如 "ws://192.168.1.100:8080"
    private val roomId: String,
    private val listener: Listener
) {
    interface Listener {
        fun onRemoteOffer(sdp: SessionDescription)
        fun onRemoteAnswer(sdp: SessionDescription)
        fun onRemoteIceCandidate(candidate: IceCandidate)
        fun onConnected()
        fun onDisconnected()
    }

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)   // 保活，避免被中间设备断连
        .build()

    fun connect() {
        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                println("Signaling: connected")
                // 连上后立刻加入房间
                val join = JSONObject().apply {
                    put("type", "join")
                    put("room", roomId)
                }
                webSocket.send(join.toString())
                listener.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                println("Signaling: received $text")
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("Signaling: failure ${t.message}")
                listener.onDisconnected()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                println("Signaling: closed $reason")
                listener.onDisconnected()
            }
        })
    }

    private fun handleMessage(text: String) {
        val json = JSONObject(text)
        when (json.optString("type")) {
            "offer" -> {
                val sdp = SessionDescription(SessionDescription.Type.OFFER, json.getString("sdp"))
                listener.onRemoteOffer(sdp)
            }
            "answer" -> {
                val sdp = SessionDescription(SessionDescription.Type.ANSWER, json.getString("sdp"))
                listener.onRemoteAnswer(sdp)
            }
            "candidate" -> {
                val candidate = IceCandidate(
                    json.getString("sdpMid"),
                    json.getInt("sdpMLineIndex"),
                    json.getString("candidate")
                )
                listener.onRemoteIceCandidate(candidate)
            }
        }
    }

    fun sendOffer(sdp: SessionDescription) {
        val json = JSONObject().apply {
            put("type", "offer")
            put("sdp", sdp.description)
        }
        webSocket?.send(json.toString())
    }

    fun sendAnswer(sdp: SessionDescription) {
        val json = JSONObject().apply {
            put("type", "answer")
            put("sdp", sdp.description)
        }
        webSocket?.send(json.toString())
    }

    fun sendIceCandidate(candidate: IceCandidate) {
        val json = JSONObject().apply {
            put("type", "candidate")
            put("sdpMid", candidate.sdpMid)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
            put("candidate", candidate.sdp)
        }
        webSocket?.send(json.toString())
    }

    fun close() {
        webSocket?.close(1000, "bye")
    }
}
