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
        streamingJob?.cancel()

        // 启动一个新的协程来处理网络推流
        streamingJob = coroutineScope.launch {
            _isStreaming.value = true
            var socket: DatagramSocket? = null
            var counter = 0L // 用于生成测试数据的计数器

            try {
                socket = DatagramSocket() // 创建 UDP socket
                Log.i(TAG, "🚀 Streaming started to ${targetAddress!!.hostAddress}:$targetPort")

                // 只要协程是活跃的，就持续推流
                while (isActive) {
                    counter++
                    val message = "Packet No. $counter from Android"
                    val data = message.toByteArray(Charsets.UTF_8)

                    val packet = DatagramPacket(
                        data,
                        data.size,
                        targetAddress,
                        targetPort
                    )

                    socket.send(packet) // 发送数据包
                    Log.d(TAG, "Sent: $message")

                    // 模拟真实音频包的速率，例如每 100 毫秒发一个包
                    delay(100)
                }
            } catch (e: Exception) {
                // 协程被取消时会抛出 CancellationException，这是正常行为，无需打印错误
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.i(TAG, "Streaming cancelled.")
                } else {
                    Log.e(TAG, "An error occurred during streaming", e)
                }
            } finally {
                // 确保资源被释放
                Log.i(TAG, "🛑 Streaming stopped.")
                socket?.close()
                _isStreaming.value = false
            }
        }
    }

    override fun stopStreaming() {
        if (!_isStreaming.value) {
            Log.d(TAG, "Streamer is not running.")
            return
        }
        // 取消协程是停止 while(isActive) 循环并触发 finally 块的最安全方式
        streamingJob?.cancel()
        streamingJob = null
    }
}