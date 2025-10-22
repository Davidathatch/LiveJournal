package com.example.recorder.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recorder.RecorderApplication
import com.example.recorder.data.Recording
import com.example.recorder.data.RecordingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.io.File

/**
 * Viewmodel for [com.example.recorder.MainActivity]
 *
 * @param filesDir: the directory in which data should be saved
 * @param recorderScope: the coroutine scope in which the recorder should run
 * @param repo: repository for accessing recorder data
 */
class RecorderVM(
    filesDir: File,
    private val recorderScope: CoroutineScope,
    private val repo: RecordingRepository
) : ViewModel() {
    /**
     * State for recording explorer composable
     */
    val recordingExplorerState = RecordingExplorerState(
        repo.allRecordings.stateIn(
            recorderScope,
            SharingStarted.Companion.WhileSubscribed(),
            initialValue = listOf()
        ),
        ::onRecordingDelete
    )

    /**
     * State for the new recording composable
     */
    val newRecordingState get() = NewRecordingState(
        viewModelScope,
        recorderScope,
        repo
    )

    /**
     * Called when user request that a recording be deleted
     *
     * @param toDelete: the recording to delete
     */
    fun onRecordingDelete(toDelete: Recording) {
        repo.deleteRecording(toDelete)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY] as RecorderApplication
                RecorderVM(
                    app.filesDir,
                    app.scope,
                    app.repo
                )
            }
        }
    }
}