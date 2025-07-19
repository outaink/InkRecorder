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
     */
    fun startStreaming()

    /**
     * 停止推流。
     */
    fun stopStreaming()
    
    /**
     * 发送音频数据到目标设备
     * @param audioData 音频数据字节数组
     * @param length 有效数据长度
     */
    fun sendAudioData(audioData: ByteArray, length: Int)
}