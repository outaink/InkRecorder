package com.outaink.inkrecorder.ui.mic

import android.os.Build
import com.freeletics.flowredux.dsl.FlowReduxStateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

sealed interface MicUiState {
    data object Connecting : MicUiState

    data class Error(val error : Throwable): MicUiState

    data object Streaming : MicUiState

    data class Initial(
        val broadcastPort: Int = 12346,
        val deviceName: String = Build.MODEL,
        val isRecording: Boolean = false,
        val recordedDataChunks: List<ByteArray> = emptyList(),
        val error: String? = null
    ) : MicUiState
}

sealed interface MicAction {
    data object StartStreaming : MicAction
    data object StopStreaming : MicAction
}


@OptIn(ExperimentalCoroutinesApi::class)
class MicStateMachine @Inject constructor(): FlowReduxStateMachine<MicUiState, MicAction>(
    initialState = MicUiState.Initial()
) {
    init {
        spec {
            inState<MicUiState.Error> {

            }

            inState<MicUiState.Initial> {
                on<MicAction.StartStreaming> { action, state ->
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
}