package com.example.recorder.recorder

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicReference

/**
 * Used to record audio from the device mic
 */
class Recorder {
    /**
     * Current state of the recorder
     */
    private val stateMutable = MutableStateFlow(RecorderState.READY)
    val state = stateMutable

    /**
     * @return flow that emits byte arrays read from an audio recorder
     */
    @SuppressLint("MissingPermission")
    fun start(): Flow<ShortArray> = flow {
        stateMutable.update { RecorderState.RECORDING }
        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                RECORDER_SAMPLE_RATE,
                RECORDER_CHANNEL_CONFIG,
                RECORDER_AUDIO_ENCODING
            )

            val audioRecord = AudioRecord(
                RECORDER_AUDIO_SOURCE,
                RECORDER_SAMPLE_RATE,
                RECORDER_CHANNEL_CONFIG,
                RECORDER_AUDIO_ENCODING,
                RECORDER_BUFFER_SIZE
            )

            try {
                audioRecord.startRecording()
                var isRecording = true

                while (stateMutable.value != RecorderState.STOPPED && stateMutable.value != RecorderState.ERROR) {
                    if (stateMutable.value == RecorderState.RECORDING) {
                        if (!isRecording) {
                            audioRecord.startRecording()
                            isRecording = true
                        }
                        val buffer = ShortArray(bufferSize * 2)
                        val readAmount = audioRecord.read(buffer, 0, buffer.size)
                        if (readAmount > 0) {
                            emit(buffer)
                        } else {
                            throw RuntimeException("Audio recorder returned error code $readAmount")
                        }
                    } else if (stateMutable.value == RecorderState.PAUSED) {
                        if (isRecording) {
                            audioRecord.stop()
                            isRecording = false
                        }
                    }
                }

                audioRecord.stop()
                audioRecord.release()
            } catch (e: Exception) {
                Log.e("Recorder", "Error while reading recorded data", e)
                stateMutable.update { RecorderState.ERROR }
            }

        } catch (e: Exception) {
            Log.e("Recorder", "Error initializing recorder", e)
            stateMutable.update { RecorderState.ERROR }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Stops an ongoing recording
     */
    fun stop() {
        stateMutable.update { RecorderState.STOPPED }
    }

    /**
     * Pauses an ongoing recording
     */
    fun pause() {
        if (stateMutable.value == RecorderState.RECORDING)
            stateMutable.update { RecorderState.PAUSED }
    }

    /**
     * Resumes a paused recording
     */
    fun resume() {
        if (stateMutable.value == RecorderState.PAUSED)
            stateMutable.update { RecorderState.RECORDING }
    }

    companion object {
        const val RECORDER_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val RECORDER_CHANNEL_COUNT = 1
        const val RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val RECORDER_BITS_PER_SAMPLE = 16
        const val RECORDER_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        const val RECORDER_SAMPLE_RATE = 16_000
        val RECORDER_BUFFER_SIZE = AudioRecord.getMinBufferSize(
            RECORDER_SAMPLE_RATE,
            RECORDER_CHANNEL_CONFIG,
            RECORDER_AUDIO_ENCODING
        )
    }

}

enum class RecorderState {
    READY,
    STOPPED,
    PAUSED,
    RECORDING,
    ERROR
}
