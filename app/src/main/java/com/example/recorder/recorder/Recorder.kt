package com.example.recorder.recorder

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.measureTime

class Recorder {
    private val recording = AtomicBoolean(false)

    @SuppressLint("MissingPermission")
    fun start(onStop: () -> Unit = {}): Flow<ShortArray> = flow {
        Log.d("Recorder", "Starting recorder...")
        recording.set(true)
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

                while (recording.get()) {
                    val buffer = ShortArray(bufferSize * 2)
                    val readAmount = audioRecord.read(buffer, 0, buffer.size)
                    if (readAmount > 0) {
                        emit(buffer)
                    } else {
                        throw RuntimeException("Audio recorder returned error code $readAmount")
                    }
                }
            } catch (e: Exception) {
                Log.e("Recorder", "Error while reading recorded data", e)
                recording.set(false)
                onStop()
            }

        } catch (e: Exception) {
            Log.e("Recorder", "Error initializing recorder", e)
            recording.set(false)
            onStop()
        }

        onStop()
    }

    fun stop() {
        recording.set(false)
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