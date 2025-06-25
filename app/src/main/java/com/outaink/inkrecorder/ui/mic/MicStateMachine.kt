package com.outaink.inkrecorder.ui.mic

import android.os.Build
import android.util.Log
import com.freeletics.flowredux.dsl.FlowReduxStateMachine
import com.outaink.inkrecorder.data.audio.AudioRecorder
import com.outaink.inkrecorder.data.audio.AudioStreamer
import com.outaink.inkrecorder.data.discovery.ClientInfo
import com.outaink.inkrecorder.data.discovery.MicServiceAdvertiser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

sealed interface MicUiState {

    data object WaitingForClient: ActiveState

    data class ClientConnected(val clientInfo: ClientInfo) : ActiveState

    data object Streaming : MicUiState

    data class Initial(
        val broadcastPort: Int = 12346,
        val deviceName: String = Build.MODEL,
        val isRecording: Boolean = false,
        val recordedDataChunks: List<ByteArray> = emptyList(),
        val error: String? = null
    ) : MicUiState
}

sealed interface ActiveState : MicUiState

sealed interface MicAction {
    data object RequestAudioPermission : MicAction
    data object AudioPermissionGranted : MicAction
    data object StartStreaming : MicAction
    data object StopStreaming : MicAction
    data class Connect(val clientInfo: ClientInfo) : MicAction
    data object Disconnect : MicAction
}


@OptIn(ExperimentalCoroutinesApi::class)
class MicStateMachine @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val serviceAdvertiser: MicServiceAdvertiser,
    private val audioStreamer: AudioStreamer
): FlowReduxStateMachine<MicUiState, MicAction>(
    initialState = MicUiState.Initial()
) {
    companion object {
        const val TAG = "MicStateMachine"
    }
    init {
        spec {
            inState<ActiveState> {
                collectWhileInState(serviceAdvertiser.clientAddressFlow) { clientInfo, state ->
                    state.override {
                        if (clientInfo != null) {
                            MicUiState.ClientConnected(clientInfo)
                        } else {
                            MicUiState.WaitingForClient
                        }
                    }
                }

                on<MicAction.Connect> { action, state ->
                    state.override { MicUiState.ClientConnected(action.clientInfo) }
                }

                on<MicAction.Disconnect> { _, state ->
                    state.override { MicUiState.WaitingForClient }
                }
            }

            inState<MicUiState.ClientConnected> {
                onEnter { state ->
                    val clientInfo = state.snapshot.clientInfo
                    Log.d(TAG, "Entering Connected State. Starting stream for ${clientInfo.address}")
                    audioStreamer.setTarget(clientInfo.address, clientInfo.port)
                    audioStreamer.startStreaming()
                    state.override { MicUiState.Streaming }
                }
            }

            inState<MicUiState.ClientConnected> {
                on<MicAction.Connect> { _, state ->
                    Log.d(TAG, "Exiting Connected State. Stopping stream.")
                    audioStreamer.stopStreaming()
                    state.override { MicUiState.WaitingForClient }
                }
            }

            inState<MicUiState.Initial> {
                on<MicAction.StartStreaming> { action, state ->
                    Log.d(TAG, "Initital, sd to ${state.snapshot.broadcastPort} with device name ${state.snapshot.deviceName}.")
                    sendUdpMessage(state.snapshot)
                    state.override { MicUiState.Streaming }
                }
            }

            inState<MicUiState.Streaming> {
                on<MicAction.StopStreaming> { action, state ->
                    state.override { MicUiState.Initial() }
                }
            }
        }
    }

    /**
     * 使用协程在 IO 线程异步发送 UDP 消息。
     *
     * @param ipAddress 目标设备的 IP 地址 (例如，你的 Mac 的 IP)。
     * @param port 目标设备监听的端口。
     * @param message 要发送的字符串消息。
     */
    private fun sendUdpMessage(state: MicUiState.Initial) {
        val isCurrentlyStreaming = state.isRecording

        if (!isCurrentlyStreaming) {
            serviceAdvertiser.registerService(state.broadcastPort, state.deviceName)
        } else {
            serviceAdvertiser.cleanup()
        }
    }

}