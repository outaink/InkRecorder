package com.outaink.inkrecorder.ui.mic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.outaink.inkrecorder.data.audio.AudioRecorder
import com.outaink.inkrecorder.data.audio.AudioStreamer
import com.outaink.inkrecorder.data.discovery.MicServiceAdvertiser
import com.outaink.inkrecorder.ui.PermissionAction
import com.outaink.inkrecorder.ui.PermissionState
import com.outaink.inkrecorder.ui.PermissionStateMachine
import com.outaink.inkrecorder.ui.WaveformData
import com.outaink.inkrecorder.ui.WaveformProcessor.smoothWaveform
import com.outaink.inkrecorder.ui.WaveformProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import javax.inject.Inject

data class MicUiState(
    val broadcastPort: Int = 12346,
    val deviceName: String = Build.MODEL,
    val isRecording: Boolean = false,
    val waveformData: WaveformData = WaveformData(
        amplitudes = emptyList(),
        progress = 0f
    ),
    val elapsedTimeMs: Long = 0L,
    val error: String? = null
)


sealed interface MicAction {
    data object ClickMicButton : MicAction

}

sealed interface UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent

    data class RequestPermission(val permission: String) : UiEvent
}

@HiltViewModel
class MicViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRecorder: AudioRecorder,
    private val micServiceAdvertiser: MicServiceAdvertiser,
    private val audioStreamer: AudioStreamer
) : ViewModel() {

    companion object {
        const val TAG = "MicViewModel"
    }

    private var _uiState = MutableStateFlow(MicUiState())
    val uiState = _uiState.asStateFlow()

    private var _waveformDataFlow: MutableSharedFlow<WaveformData> = MutableSharedFlow()

    private val _uiEventChannel = Channel<UiEvent>()
    val uiEvents = _uiEventChannel.receiveAsFlow()
    
    private var frameCounter = 0
    private var timerJob: Job? = null
    private var recordingStartTime = 0L

    fun dispatch(action: MicAction) {
        viewModelScope.launch {
            when (action) {
                is MicAction.ClickMicButton -> {
                    if (checkIfPermissionGranted(Manifest.permission.RECORD_AUDIO)) {
                        if (!uiState.value.isRecording) {
                            startMicrophoneRecording()
                        } else {
                            stopMicrophoneRecording()
                        }
                    } else {
                        send(UiEvent.RequestPermission(Manifest.permission.RECORD_AUDIO))
                    }
                }

            }
        }
    }

    private fun startMicrophoneRecording() {
        recordingStartTime = System.currentTimeMillis()
        frameCounter = 0
        
        _uiState.update { it.copy(isRecording = true, elapsedTimeMs = 0L, error = null) }
        
        // Start timer to update elapsed time
        startTimer()
        
        Log.d(TAG, "Starting microphone recording")
        
        audioRecorder.startRecording(
            onDataReceived = { audioData: ByteArray, bytesRead ->
                viewModelScope.launch {
                    val waveformData = processAudioData(audioData, bytesRead)
                    logAudioData(audioData, bytesRead, waveformData)
                    
                    _uiState.update { it.copy(waveformData = waveformData) }
                    frameCounter++
                }
            },
            onError = { exception ->
                Log.e(TAG, "Audio recording error", exception)
                stopTimer()
                _uiState.update { 
                    it.copy(
                        isRecording = false,
                        error = "Recording error: ${exception.message}"
                    )
                }
            }
        )
    }
    
    private fun stopMicrophoneRecording() {
        Log.d(TAG, "Stopping microphone recording")
        audioRecorder.stopRecording()
        stopTimer()
        
        _uiState.update { 
            it.copy(
                isRecording = false,
                waveformData = WaveformData(
                    amplitudes = List(100) { 0f }, // Show flat line when stopped
                    frequencies = emptyList(),
                    progress = 0f,
                    isFrequencyMode = false
                ),
                error = null
            )
        }
    }
    
    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (uiState.value.isRecording) {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - recordingStartTime
                
                _uiState.update { it.copy(elapsedTimeMs = elapsedTime) }
                
                delay(100) // Update every 100ms for smooth time display
            }
        }
    }
    
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
    
    private fun processAudioData(audioData: ByteArray, bytesRead: Int): WaveformData {
        // Convert byte array to short array (16-bit PCM)
        val shortArray = ShortArray(bytesRead / 2)
        for (i in shortArray.indices) {
            val byteIndex = i * 2
            if (byteIndex + 1 < bytesRead) {
                // Little-endian conversion: combine two bytes into one short
                shortArray[i] = ((audioData[byteIndex + 1].toInt() shl 8) or 
                                (audioData[byteIndex].toInt() and 0xFF)).toShort()
            }
        }
        
        // Convert to normalized float values and downsample for visualization
        val targetSize = 100
        val amplitudes = if (shortArray.isNotEmpty()) {
            WaveformProcessor.processAudioSamples(shortArray, targetSize)
        } else {
            List(targetSize) { 0f }
        }
        
        // Generate frequency spectrum data
        val frequencies = if (shortArray.isNotEmpty()) {
            WaveformProcessor.processAudioToFrequency(shortArray, 48000, targetSize)
        } else {
            List(targetSize) { 0f }
        }
        
        return WaveformData(
            amplitudes = smoothWaveform(amplitudes),
            frequencies = frequencies,
            progress = 0f,
            isFrequencyMode = true // Enable frequency mode for real-time spectrum
        )
    }
    
    private fun logAudioData(audioData: ByteArray, bytesRead: Int, waveformData: WaveformData) {
        // Log every 20th frame to avoid spam (roughly every second at typical callback rates)
        if (frameCounter % 20 == 0) {
            val avgAmplitude = waveformData.amplitudes.map { kotlin.math.abs(it) }.average()
            val maxAmplitude = waveformData.amplitudes.maxByOrNull { kotlin.math.abs(it) } ?: 0f
            val rmsLevel = kotlin.math.sqrt(waveformData.amplitudes.map { it * it }.average())
            
            // Frequency analysis
            val avgFreqIntensity = if (waveformData.frequencies.isNotEmpty()) {
                waveformData.frequencies.average()
            } else 0.0
            val maxFreqIntensity = waveformData.frequencies.maxOrNull() ?: 0f
            val dominantFreqBand = if (waveformData.frequencies.isNotEmpty()) {
                waveformData.frequencies.withIndex().maxByOrNull { it.value }?.index ?: 0
            } else 0
            
            // Audio level analysis
            val audioLevel = when {
                maxAmplitude < 0.1f -> "Silent"
                maxAmplitude < 0.3f -> "Quiet"
                maxAmplitude < 0.6f -> "Moderate"
                maxAmplitude < 0.8f -> "Loud" 
                else -> "Very Loud"
            }
            
            // Frequency band analysis (high to low)
            val freqBandName = when (dominantFreqBand) {
                in 0..19 -> "High-Freq" // 10kHz+ (vocals, cymbals)
                in 20..39 -> "Mid-High" // 2-10kHz (consonants)
                in 40..59 -> "Mid-Range" // 500Hz-2kHz (vowels)
                in 60..79 -> "Low-Mid" // 100-500Hz (male vocals)
                else -> "Low-Freq" // 20-100Hz (bass, drums)
            }
            
            Log.d(TAG, "Frame $frameCounter - Bytes: $bytesRead, RMS: ${String.format("%.3f", rmsLevel)}, Max: ${String.format("%.3f", maxAmplitude)}, Level: $audioLevel")
            Log.d(TAG, "Frequency - Avg: ${String.format("%.3f", avgFreqIntensity)}, Max: ${String.format("%.3f", maxFreqIntensity)}, Dominant: $freqBandName (band $dominantFreqBand)")
            
            // Log detailed waveform data every 100th frame
            if (frameCounter % 100 == 0) {
                val waveformString = waveformData.amplitudes.take(10).joinToString(", ") { String.format("%.2f", it) }
                Log.d(TAG, "Waveform data (first 10): [$waveformString...]")
                
                val frequencyString = waveformData.frequencies.take(10).joinToString(", ") { String.format("%.2f", it) }
                Log.d(TAG, "Frequency data (first 10 high-freq bands): [$frequencyString...]")
                
                // Log raw audio data sample
                val rawSampleString = audioData.take(20).joinToString(", ") { it.toString() }
                Log.d(TAG, "Raw audio bytes (first 20): [$rawSampleString...]")
            }
        }
    }

    private fun send(event: UiEvent) {
        viewModelScope.launch {
            _uiEventChannel.send(event)
        }
    }

    private fun registerService(port: Int) {
        micServiceAdvertiser.registerService(
            port = port,
            deviceName = "InkMic"
        )
    }

    private fun checkIfPermissionGranted(permission: String) =
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
}