package com.example.recorder.state

import android.content.res.AssetManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recorder.RecorderApplication
import com.example.recorder.composables.NewRecording
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
    private val repo: RecordingRepository,
    private val assetManager: AssetManager
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
     * State for new recording screen
     */
    private var newRecordingStateMutable = NewRecordingState(
        viewModelScope,
        recorderScope,
        repo
    )

    lateinit var recordingPlaybackState: RecordingPlaybackState

    /**
     * Current navigation route
     */
    private var currentRoute = ""

    /**
     * State for the new recording composable
     */
    var newRecordingState
        get() = newRecordingStateMutable
        private set(value) {
            newRecordingStateMutable = value
        }

    fun initializeRecordingPlaybackState(recording: Recording) {
        recordingPlaybackState = RecordingPlaybackState(
            recording,
            assetManager,
            repo,
            viewModelScope
        )
    }

    /**
     * Resets the new recording state, disposing of any existing state data
     */
    fun resetNewRecordingState() {
        newRecordingState = NewRecordingState(
            viewModelScope,
            recorderScope,
            repo
        )
    }

    /**
     * Handles navigation to a difference screen by the user
     *
     * @param route: route of the current screen
     */
    fun onNavigationChange(route: String) {
        if (currentRoute == NewRecording.javaClass.canonicalName && route != NewRecording.javaClass.canonicalName) {
            newRecordingState.close()
            resetNewRecordingState()
        }
        if (currentRoute != NewRecording.javaClass.canonicalName && route == NewRecording.javaClass.canonicalName) {
            resetNewRecordingState()
            newRecordingState.initialize()
        }
        currentRoute = route
    }

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
                    app.repo,
                    app.assets
                )
            }
        }
    }
}