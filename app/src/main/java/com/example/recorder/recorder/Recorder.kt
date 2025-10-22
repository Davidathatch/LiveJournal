package com.example.recorder.recorder

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.atomic.AtomicReference

/**
 * Used to record audio from the device mic
 */
class Recorder {
    /**
     * Current state of the recorder
     */
    private val state = AtomicReference(RecorderState.READY)

    fun state(): RecorderState {
        return state.get()
    }

    /**
     * @return flow that emits byte arrays read from an audio recorder
     */
    @SuppressLint("MissingPermission")
    fun start(): Flow<ShortArray> = flow {
        state.set(RecorderState.RECORDING)
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

                while (state.get() != RecorderState.STOPPED && state.get() != RecorderState.ERROR) {
                    if (state.get() == RecorderState.RECORDING) {
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
                    } else if (state.get() == RecorderState.PAUSED) {
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
                state.set(RecorderState.ERROR)
            }

        } catch (e: Exception) {
            Log.e("Recorder", "Error initializing recorder", e)
            state.set(RecorderState.ERROR)
        }
    }

    /**
     * Stops an ongoing recording
     */
    fun stop() {
        state.set(RecorderState.STOPPED)
    }

    /**
     * Pauses an ongoing recording
     */
    fun pause() {
        if (state.get() == RecorderState.RECORDING)
            state.set(RecorderState.PAUSED)
    }

    /**
     * Resumes a paused recording
     */
    fun resume() {
        if (state.get() == RecorderState.PAUSED)
            state.set(RecorderState.RECORDING)
    }

    companion object {
        const val RECORDER_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val RECORDER_CHANNEL_COUNT = 1
        const val RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val RECORDER_BITS_PER_SAMPLE = 16
        const val RECORDER_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        const val RECORDER_SAMPLE_RATE = 44000
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
