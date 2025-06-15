package com.outaink.inkrecorder.viewmodel

import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.outaink.inkrecorder.domain.audio.AudioRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecorderUiState(
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
    private val audioRecorder: AudioRecorder
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecorderUiState())
    val uiState: StateFlow<RecorderUiState> = _uiState.asStateFlow()

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

    private fun stopAudioRecording() {
        if (!_uiState.value.isRecording) return

        audioRecorder.stopRecording()
        _uiState.update { it.copy(isRecording = false) }
    }

    override fun onCleared() {
        audioRecorder.stopRecording()
        super.onCleared()
    }

}