package com.example.recorder.state

import android.util.Log
import com.example.recorder.data.Recording
import com.example.recorder.data.RecordingRepository
import com.example.recorder.data.RecordingState
import com.example.recorder.recorder.Recorder
import com.example.recorder.recorder.RecorderState
import com.example.recorder.recorder.RecordingWriter
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class NewRecordingState(
    private val stateScope: CoroutineScope,
    private val recorderScope: CoroutineScope,
    private val repo: RecordingRepository,
    private var onExitNewRecordingBtnPress: () -> Unit = {}
) {
    /**
     * Used to record audio from device mic
     */
    private val recorder = Recorder()

    /**
     * Used to write recorder data to a file
     */
    private lateinit var writer: RecordingWriter

    /**
     * Row ID of this new recording in the database
     */
    private var recordingRowId: Long? = null

    /**
     * Recording database entry currently being modified
     */
    private val newRecording = Recording(name = "Recording")

    /**
     * Current state of the recorder
     */
    val recorderState = recorder.state

    private val initializedMutable = MutableStateFlow(InitializationState.NOT_INITIALIZED)

    /**
     * Tracks the status of this state object's initialization. Some initializations are run
     * when the state object is created my [RecorderVM], so [initialized] enables these resources
     * to be tracked and cleaned up when necessary.
     */
    val initialized = initializedMutable

    /**
     * Updates the callback that is invoked when the stop recording button is pressed.
     *
     * @param callback: function to be called by stop button
     */
    fun setOnExitNewRecordingBtnPress(callback: () -> Unit) {
        onExitNewRecordingBtnPress = callback
    }

    /**
     * Eagerly initializes a recording file and adds an entry to the database. This
     * data is deleted if the user leaves the new recording screen without recording
     * any audio.
     */
    fun initialize() {
        if (initializedMutable.compareAndSet(
                InitializationState.NOT_INITIALIZED,
                InitializationState.INITIALIZING
            )
        ) {
            stateScope.launch(Dispatchers.IO) {
                writer = repo.prepareForRecording(newRecording)
                recordingRowId = repo.addRecording(newRecording).await()
                initializedMutable.update { InitializationState.INITIALIZED }
            }
        }
    }

    /**
     * Releases any resources used by this state object.
     */
    fun close() {
        stateScope.launch(Dispatchers.IO) {
            if (recorderState.value != RecorderState.READY) {
                recorder.stop()
                writer.close()
                if (recordingRowId != null) {
                    repo.updateRecordingState(recordingRowId!!, RecordingState.COMPLETE)
                }
            } else {
                if (!initializedMutable.compareAndSet(
                        InitializationState.NOT_INITIALIZED,
                        InitializationState.CLOSING
                    )
                ) {
                    while (!initializedMutable.compareAndSet(
                            InitializationState.INITIALIZED,
                            InitializationState.CLOSING
                        )
                    ) {
                    }
                    writer.close()
                    if (recordingRowId != null) {
                        repo.deleteRecordingFromId(newRecording, recordingRowId!!)
                    }
                }
            }
        }
    }


    /**
     * Incoked when the stop button is pressed
     */
    fun onStopBtnPressed() {
        stateScope.launch {
            close()
            onExitNewRecordingBtnPress()
        }
    }

    /**
     * Toggles the recording between recording and paused states.
     */
    fun toggleRecord() {
        stateScope.launch(Dispatchers.IO) {
            when (recorderState.value) {
                RecorderState.READY -> {
                    try {
                        stateScope.launch {
                            writer.beginSave(recorder.start())
                        }
                    } catch (e: Exception) {
                        Log.e("Recorder Vm", "Error starting recorder", e)
                    }
                }

                RecorderState.RECORDING -> {
                    recorder.pause()
                    writer.updateHeader()
                    checkNotNull(recordingRowId)
                    repo.updateRecordingState(recordingRowId!!, RecordingState.COMPLETE)
                }

                RecorderState.PAUSED -> {
                    checkNotNull(recordingRowId)
                    repo.updateRecordingState(recordingRowId!!, RecordingState.INCOMPLETE)
                    recorder.resume()
                }

                else -> {}
            }
        }
    }
}

/**
 * States of initialization for a [NewRecordingState] object
 */
enum class InitializationState {
    /**
     * Resources have not been initialized, nor has the initialization process started
     */
    NOT_INITIALIZED,

    /**
     * Resources are in the process of being initialized
     */
    INITIALIZING,

    /**
     * Resources have been initialized
     */
    INITIALIZED,

    /**
     * Resources are in the process of being released
     */
    CLOSING
}
