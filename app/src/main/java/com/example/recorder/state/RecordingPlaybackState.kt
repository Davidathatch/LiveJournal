package com.example.recorder.state

import android.content.res.AssetManager
import com.example.recorder.data.Recording
import com.example.recorder.data.RecordingRepository
import com.whispercpp.whisper.WhisperContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RecordingPlaybackState(
    private val recording: Recording,
    private val assetManager: AssetManager,
    private val repo: RecordingRepository,
    private val scope: CoroutineScope
) {
    private lateinit var whisperContext: WhisperContext
    private val transcriptionState = MutableStateFlow(false)

    private val transcriptionMutable = MutableStateFlow("No transcription")
    val transcription = transcriptionMutable

    fun transcribe() {
        scope.launch {
            transcriptionMutable.update { "Working..." }
            loadBaseModel()
            transcribeFile()
        }
    }

    private suspend fun loadBaseModel() = withContext(Dispatchers.IO) {
        whisperContext =
            WhisperContext.createContextFromAsset(assetManager, "models/ggml-tiny.en.bin")
    }

    private suspend fun transcribeFile() = withContext(Dispatchers.IO) {
        if (transcriptionState.compareAndSet(expect = false, update = true)) {
            val toTranscribe = repo.getRecordingFile(recording)
            val data = readAudioSamples(toTranscribe)
            transcriptionMutable.update { whisperContext.transcribeData(data) }
            transcriptionState.update { false }
        }
    }

    private suspend fun readAudioSamples(file: File): FloatArray = withContext(Dispatchers.IO) {
        val baos = ByteArrayOutputStream()
        file.inputStream().use { it.copyTo(baos) }
        val buffer = ByteBuffer.wrap(baos.toByteArray())
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val channel = buffer.getShort(22).toInt()
        buffer.position(44)
        val shortBuffer = buffer.asShortBuffer()
        val shortArray = ShortArray(shortBuffer.limit())
        shortBuffer.get(shortArray)
        val sampleRateMultiplier = recording.sampleRate / 16_000
        return@withContext FloatArray(shortArray.size / channel / sampleRateMultiplier) { index ->
            when (channel) {
                1 -> (shortArray[index * sampleRateMultiplier] / 32767.0f).coerceIn(-1f..1f)
                else -> ((shortArray[2 * index * sampleRateMultiplier] + shortArray[2 * index + 1 * sampleRateMultiplier]) / 32767.0f / 2.0f).coerceIn(
                    -1f..1f
                )
            }
        }
    }
}