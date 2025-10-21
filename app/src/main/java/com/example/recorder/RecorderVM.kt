package com.example.recorder

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recorder.recorder.Recorder
import com.example.recorder.recorder.RecordingWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * Viewmodel for [MainActivity]
 *
 * @param filesDir: the directory in which data should be saved
 * @param scope: the coroutine scope in which the recorder should run
 */
class RecorderVM(
    private val filesDir: File,
    private val scope: CoroutineScope
) : ViewModel() {
    /**
     * Used to record audio from the device mic
     */
    private val recorder = Recorder()

    /**
     * Used to write data collected from the currently active recording
     */
    private lateinit var writer: RecordingWriter

    val recordingMutable = MutableStateFlow(false)

    /**
     * True if the device is actively recording, false otherwise
     */
    val recording = recordingMutable

    /**
     * Called when the user toggles between starting/stopping a recording. If a recording is
     * ongoing, it is finalized and should be presented to the user. If there is not an ongoing
     * recording, a new one is started and set to save its data to a new .wav file in [filesDir].
     */
    fun toggleRecording() {
        // Ongoing recording; finalize it
        if (recordingMutable.value) {
            recorder.stop()
            recordingMutable.value = false
        }
        // No ongoing recording; start a new one
        else {
            val fileName = "${UUID.randomUUID()}.wav"
            try {
                filesDir.resolve("audio").mkdir()
                val toSave = File(filesDir.resolve("audio"), fileName)
                if (toSave.createNewFile()) {
                    writer = RecordingWriter(toSave)

                    recordingMutable.value = true
                    scope.launch {
                        writer.beginSave(recorder.start {
                            scope.launch {
                                writer.finishSave()
                            }
                        })
                    }
                }
            } catch (e: Exception) {
                Log.e("Recorder Vm", "Error starting recorder", e)
                recordingMutable.value = false
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RecorderApplication
                RecorderVM(
                    app.filesDir,
                    app.scope
                )
            }
        }
    }
}