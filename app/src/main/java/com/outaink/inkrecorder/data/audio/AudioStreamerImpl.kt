package com.outaink.inkrecorder.data.audio

// in: data/network/AudioStreamerImpl.kt

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioStreamerImpl @Inject constructor() : AudioStreamer {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var streamingJob: Job? = null
    private var socket: DatagramSocket? = null

    private var targetAddress: InetAddress? = null
    private var targetPort: Int = -1

    private val _isStreaming = MutableStateFlow(false)
    override val isStreaming = _isStreaming.asStateFlow()

    companion object {
        private const val TAG = "AudioStreamer"
    }

    override fun setTarget(address: InetAddress, port: Int) {
        this.targetAddress = address
        this.targetPort = port
        Log.d(TAG, "Target set to: ${address.hostAddress}:$port")
    }

    override fun startStreaming() {
        if (_isStreaming.value) {
            Log.w(TAG, "Streamer is already running.")
            return
        }
        if (targetAddress == null || targetPort == -1) {
            Log.e(TAG, "Target address or port not set. Cannot start streaming.")
            return
        }

        // 取消上一个可能存在的任务，确保只有一个推流任务在运行
        stopStreaming()

        try {
            socket = DatagramSocket() // 创建 UDP socket
            _isStreaming.value = true
            Log.i(TAG, "🚀 Audio streaming started to ${targetAddress!!.hostAddress}:$targetPort")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create UDP socket for streaming", e)
            _isStreaming.value = false
        }
    }

    override fun stopStreaming() {
        Log.i(TAG, "🛑 Audio streaming stopped.")
        socket?.close()
        socket = null
        _isStreaming.value = false

        // 取消协程是停止 while(isActive) 循环并触发 finally 块的最安全方式
        streamingJob?.cancel()
        streamingJob = null
    }

    override fun sendAudioData(audioData: ByteArray, length: Int) {
        if (!_isStreaming.value || socket == null || targetAddress == null || targetPort == -1) {
            return // Skip if not streaming or not properly configured
        }

        // Launch on IO dispatcher to avoid NetworkOnMainThreadException
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Create a new array with exact length to avoid sending extra bytes
                val trimmedData = ByteArray(length)
                System.arraycopy(audioData, 0, trimmedData, 0, length)

                val packet = DatagramPacket(
                    trimmedData,
                    length,
                    targetAddress,
                    targetPort
                )

                socket?.send(packet)
                Log.v(TAG, "Sent ${length} bytes of audio data") // Verbose logging for debugging
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send audio data", e)
                // Don't stop streaming on individual packet failures
            }
        }
    }
}