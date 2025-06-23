package com.outaink.inkrecorder.viewmodel

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.outaink.inkrecorder.data.discovery.MicServiceAdvertiser
import com.outaink.inkrecorder.data.audio.AudioRecorder
import com.outaink.inkrecorder.data.audio.AudioStreamer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecorderUiState(
    val broadcastPort: Int = 12346,
    val deviceName: String = android.os.Build.MODEL,
    val isRecording: Boolean = false,
    val recordedDataChunks: List<ByteArray> = emptyList(),
    val error: String? = null
)

sealed interface RecorderIntent {
    object StartRecording : RecorderIntent
    object StopRecording : RecorderIntent
}

@HiltViewModel
class RecorderViewModel @Inject constructor(
    private val serviceAdvertiser: MicServiceAdvertiser,
    private val audioRecorder: AudioRecorder,
    private val audioStreamer: AudioStreamer
) : ViewModel() {

    companion object {
        const val TAG = "RecorderViewModel"
    }

    private val _uiState = MutableStateFlow(RecorderUiState())
    val uiState: StateFlow<RecorderUiState> = _uiState.asStateFlow()


    init {
        observeClientConnection()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun onEvent(intent: RecorderIntent) = when (intent) {
        RecorderIntent.StartRecording -> startAudioRecording()
        RecorderIntent.StopRecording -> stopAudioRecording()
    }


    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startAudioRecording() {
        if (_uiState.value.isRecording) return

        _uiState.update { it.copy(
            isRecording = true,
            error = null,
        ) }

        viewModelScope.launch {
            audioRecorder.startRecording(
                onDataReceived = { data, size ->
                    _uiState.update { currentState ->
                        currentState.copy(recordedDataChunks = currentState.recordedDataChunks + data)
                    }
                },
                onError = { exception ->
                    _uiState.update {
                        it.copy(
                            isRecording = false,
                            error = "Recording error: ${exception.localizedMessage ?: "Unknown error"}"
                        )
                    }

                    audioRecorder.stopRecording()
                }
            )
        }
    }

    /**
     * 使用协程在 IO 线程异步发送 UDP 消息。
     * 这是一个最佳实践，可以防止 NetworkOnMainThreadException。
     *
     * @param ipAddress 目标设备的 IP 地址 (例如，你的 Mac 的 IP)。
     * @param port 目标设备监听的端口。
     * @param message 要发送的字符串消息。
     */
    fun sendUdpMessage() {
        val isCurrentlyStreaming = _uiState.value.isRecording
        _uiState.update { it.copy(isRecording = !isCurrentlyStreaming) }

        if (!isCurrentlyStreaming) {
            serviceAdvertiser.registerService(_uiState.value.broadcastPort, _uiState.value.deviceName)
        } else {
            serviceAdvertiser.cleanup()
        }
    }

    fun onIntent(event: RecorderIntent) {
        when (event) {
            RecorderIntent.StartRecording -> {
                viewModelScope.launch {
                    sendUdpMessage()
                }
            }
            RecorderIntent.StopRecording -> {

            }
        }
    }

    private fun stopAudioRecording() {
        if (!_uiState.value.isRecording) return

        audioRecorder.stopRecording()
        _uiState.update { it.copy(isRecording = false) }
    }

    override fun onCleared() {
        audioRecorder.stopRecording()
        Log.d(TAG, "ViewModel cleared, unregistering service.")
        stopEverything()
    }

    private fun observeClientConnection() {
        viewModelScope.launch {
            serviceAdvertiser.clientAddressFlow.collect { clientInfo ->
                if (clientInfo != null) {
                    // 我们拿到了 Mac 客户端的地址！
                    Log.d(TAG, "Client connected: ${clientInfo.address}:${clientInfo.port}")
                    // 在这里，你可以命令你的音频流发送器开始工作
                    audioStreamer.setTarget(clientInfo.address, clientInfo.port)
                    audioStreamer.startStreaming()

                    // 更新 UI 状态
                    // _state.update { it.copy(clientConnected = true, ...) }
                } else {
                    // Client 断开或服务已清理
                    Log.d(TAG, "Client disconnected.")
                    audioStreamer.stopStreaming()
                    // 更新 UI 状态
                    // _state.update { it.copy(clientConnected = false, ...) }
                }
            }
        }
    }

    private fun stopEverything() {
        serviceAdvertiser.cleanup()
        audioStreamer.stopStreaming()
    }

}