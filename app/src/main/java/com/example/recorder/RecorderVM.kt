package com.example.recorder

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recorder.data.Recording
import com.example.recorder.data.RecordingRepository
import com.example.recorder.data.RecordingState
import com.example.recorder.recorder.Recorder
import com.example.recorder.recorder.RecordingWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/**
 * Viewmodel for [MainActivity]
 *
 * @param filesDir: the directory in which data should be saved
 * @param recorderScope: the coroutine scope in which the recorder should run
 */
class RecorderVM(
    private val filesDir: File,
    private val recorderScope: CoroutineScope,
    private val repo: RecordingRepository
) : ViewModel() {
    /**
     * Used to record audio from the device mic
     */
    private val recorder = Recorder()

    private var recordingRowId: Long? = null

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
     * List of recordings to present to user
     */
    val recordings = repo.allRecordings.stateIn(
        recorderScope,
        SharingStarted.WhileSubscribed(),
        initialValue = listOf()
    )

    /**
     * Called when user request that a recording be deleted
     *
     * @param toDelete: the recording to delete
     */
    fun onRecordingDelete(toDelete: Recording) {
        repo.deleteRecording(toDelete)
    }

    /**
     * Called when the user toggles between starting/stopping a recording. If a recording is
     * ongoing, it is finalized and should be presented to the user. If there is not an ongoing
     * recording, a new one is started and set to save its data to a new .wav file in [filesDir].
     */
    fun toggleRecording() {
        viewModelScope.launch {
            // Ongoing recording; finalize it
            if (recordingMutable.value) {
                recorder.stop()
                checkNotNull(recordingRowId)
                repo.updateRecordingState(recordingRowId!!, RecordingState.COMPLETE)
                recordingMutable.value = false
            }
            // No ongoing recording; start a new one
            else {
                val newRecording = Recording(
                    name = "Recording",
                )
                try {
                    writer = repo.prepareForRecording(newRecording)

                    recordingMutable.value = true
                    recorderScope.launch {
                        writer.beginSave(recorder.start {
                            recorderScope.launch {
                                writer.finishSave()
                            }
                        })
                    }
                    recordingRowId = repo.addRecording(newRecording).await()
                } catch (e: Exception) {
                    Log.e("Recorder Vm", "Error starting recorder", e)
                    recordingMutable.value = false
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RecorderApplication
                RecorderVM(
                    app.filesDir,
                    app.scope,
                    app.repo
                )
            }
        }
    }
}