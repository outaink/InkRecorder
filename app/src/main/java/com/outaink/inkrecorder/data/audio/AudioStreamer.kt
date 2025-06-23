package com.outaink.inkrecorder.data.audio

import kotlinx.coroutines.flow.StateFlow
import java.net.InetAddress

interface AudioStreamer {
    /**
     * 一个只读的 StateFlow，用于向外部暴露当前是否正在推流的状态。
     */
    val isStreaming: StateFlow<Boolean>

    /**
     * 设置音频流的目标地址和端口。
     * @param address 目标设备的 IP 地址。
     * @param port 目标设备的端口。
     */
    fun setTarget(address: InetAddress, port: Int)

    /**
     * 开始向已设置的目标推流。
     * 为了测试，它会内部生成并发送一个字节流。
     */
    fun startStreaming()

    /**
     * 停止推流。
     */
    fun stopStreaming()
}