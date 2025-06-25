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

    private val audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION

    private val audioConfig = AudioConfig(
        sampleRate = 48000,
        channelConfig = AudioFormat.CHANNEL_IN_MONO,
        audioFormat = AudioFormat.ENCODING_PCM_16BIT
    )

    private val audioRecord: AudioRecord by lazy {
        AudioRecord(
            audioSource,
            audioConfig.sampleRate,
            audioConfig.channelConfig,
            audioConfig.audioFormat,
            audioConfig.bufferSize
        )
    }

    companion object {
        const val TAG = "AudioRecorder"
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
            return
        }

        try {
            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                val error = IllegalStateException("AudioRecord initialization failed.")
                Log.e(TAG, error.message ?: "Unknown error")
                onError?.invoke(error)
                return
            }

            isRecording = true
            audioRecord.startRecording()

            recordingJob = coroutineScope.launch {
                Log.d(TAG, "Recording started on thread: ${Thread.currentThread().name}")
                val audioBuffer = ByteArray(size = audioConfig.bufferSize)

                try {
                    while (isActive && isRecording) {
                        val readResult = audioRecord.read(
                            audioBuffer,
                            0,
                            audioBuffer.size
                        )

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
            if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop()
            }

            audioRecord.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
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