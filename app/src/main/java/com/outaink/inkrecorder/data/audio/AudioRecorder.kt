package com.outaink.inkrecorder.data.audio

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission")
@Singleton
class AudioRecorder @Inject constructor() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var recordingJob: Job? = null

    @Volatile
    private var isRecording = false

    private val audioSource = MediaRecorder.AudioSource.DEFAULT

    private val audioConfig = AudioConfig(
        sampleRate = 44100, // Changed to 44.1kHz for macOS client compatibility
        channelConfig = AudioFormat.CHANNEL_IN_MONO,
        audioFormat = AudioFormat.ENCODING_PCM_16BIT
    )

    private var audioRecord: AudioRecord? = null

    companion object {
        const val TAG = "AudioRecorder"
    }

    private fun createAudioRecord(): AudioRecord? {
        // Try primary configuration first
        val primaryRecord = tryCreateAudioRecord(
            audioSource,
            audioConfig.sampleRate,
            audioConfig.channelConfig,
            audioConfig.audioFormat
        )
        if (primaryRecord != null) return primaryRecord
        
        // Try fallback configurations
        Log.w(TAG, "Primary config failed, trying fallback configurations...")
        
        // Try with MIC source instead of VOICE_COMMUNICATION
        val micRecord = tryCreateAudioRecord(
            MediaRecorder.AudioSource.MIC,
            audioConfig.sampleRate,
            audioConfig.channelConfig,
            audioConfig.audioFormat
        )
        if (micRecord != null) return micRecord
        
        // Try with lower sample rate
        val lowerSampleRateRecord = tryCreateAudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100, // Standard CD quality
            audioConfig.channelConfig,
            audioConfig.audioFormat
        )
        if (lowerSampleRateRecord != null) return lowerSampleRateRecord
        
        // Final fallback: Basic configuration
        return tryCreateAudioRecord(
            MediaRecorder.AudioSource.MIC,
            16000, // Phone quality
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
    }
    
    private fun tryCreateAudioRecord(
        audioSource: Int,
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int
    ): AudioRecord? {
        return try {
            // Validate minimum buffer size
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            
            if (minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.w(TAG, "Invalid parameters - sampleRate: $sampleRate, channelConfig: $channelConfig, audioFormat: $audioFormat")
                return null
            }
            
            val bufferSize = minBufferSize * 4 // Use larger buffer for stability
            Log.d(TAG, "Trying AudioRecord - source: $audioSource, sampleRate: $sampleRate, bufferSize: $bufferSize")
            
            val record = AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize)
            
            if (record.state == AudioRecord.STATE_INITIALIZED) {
                Log.d(TAG, "AudioRecord created successfully:")
                Log.d(TAG, "  Sample Rate: $sampleRate Hz")
                Log.d(TAG, "  Channel Config: $channelConfig") 
                Log.d(TAG, "  Audio Format: $audioFormat")
                Log.d(TAG, "  Buffer Size: $bufferSize bytes")
                record
            } else {
                Log.w(TAG, "AudioRecord creation failed - state: ${record.state}")
                record.release()
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception creating AudioRecord with sampleRate: $sampleRate", e)
            null
        }
    }

    /**
     * 开始录音
     * @param onDataReceived 音频数据回调函数
     * @param onError 错误回调函数
     */
    fun startRecording(
        onDataReceived: (ByteArray, Int) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ) {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return
        }

        try {
            // Clean up any existing AudioRecord instance
            cleanup()
            
            // Create new AudioRecord instance
            audioRecord = createAudioRecord()
            if (audioRecord == null) {
                val error = IllegalStateException("AudioRecord initialization failed.")
                Log.e(TAG, error.message.toString())
                onError?.invoke(error)
                return
            }

            isRecording = true
            audioRecord!!.startRecording()

            recordingJob = coroutineScope.launch {
                Log.d(TAG, "Recording started on thread: ${Thread.currentThread().name}")
                val audioBuffer = ByteArray(size = audioConfig.bufferSize)

                try {
                    while (isActive && isRecording) {
                        val readResult = audioRecord?.read(
                            audioBuffer,
                            0,
                            audioBuffer.size
                        ) ?: break

                        when {
                            readResult > 0 -> {
                                onDataReceived(audioBuffer.copyOf(newSize = readResult), readResult)
                            }

                            readResult == AudioRecord.ERROR_INVALID_OPERATION -> {
                                Log.e(TAG, "ERROR_INVALID_OPERATION")
                                onError?.invoke(IllegalStateException("Invalid operation"))
                                break
                            }

                            readResult == AudioRecord.ERROR_BAD_VALUE -> {
                                Log.e(TAG, "ERROR_BAD_VALUE")
                                onError?.invoke(IllegalArgumentException("Bad value"))
                                break
                            }

                            readResult == AudioRecord.ERROR_DEAD_OBJECT -> {
                                Log.e(TAG, "ERROR_DEAD_OBJECT")
                                onError?.invoke(IllegalStateException("Dead object"))
                                break
                            }

                            readResult < 0 -> {
                                Log.e(TAG, "Unknown error: $readResult")
                                onError?.invoke(Exception("Unknown error: $readResult"))
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during recording", e)
                    onError?.invoke(e)
                } finally {
                    Log.d(TAG, "Recording loop finished.")
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception - missing RECORD_AUDIO permission", e)
            onError?.invoke(e)
            cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            onError?.invoke(e)
            cleanup()
        }
    }

    fun stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "Not recording.")
            return
        }

        isRecording = false

        runBlocking {
            recordingJob?.cancelAndJoin()
        }

        cleanup()
        Log.d(TAG, "Recording stopped and resources released.")
    }

    private fun cleanup() {
        try {
            audioRecord?.let { record ->
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                    Log.d(TAG, "AudioRecord stopped")
                }
                record.release()
                Log.d(TAG, "AudioRecord released")
            }
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
            audioRecord = null
        }
    }

    fun getAudioConfig(): AudioConfig = audioConfig

    /**
     * 音频配置数据类
     */
    data class AudioConfig(
        val sampleRate: Int,
        val channelConfig: Int,
        val audioFormat: Int,
        val bufferSize: Int = AudioRecord.getMinBufferSize(
            sampleRate, channelConfig, audioFormat
        ) * 2
    )
}