package com.example.recorder.state

import android.util.Log
import com.example.recorder.data.Recording
import com.example.recorder.data.RecordingRepository
import com.example.recorder.data.RecordingState
import com.example.recorder.recorder.Recorder
import com.example.recorder.recorder.RecorderState
import com.example.recorder.recorder.RecordingWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    private val recorderStateMutable = MutableStateFlow(recorder.state())
    val recorderState = recorderStateMutable

    fun setOnExitNewRecordingBtnPress(callback: () -> Unit) {
        onExitNewRecordingBtnPress = callback
    }

    fun exitNewRecording() {
        stateScope.launch {
            if (recorder.state() != RecorderState.READY) {
                recorder.stop()
                writer.finishSave()
                if (recordingRowId != null) {
                    repo.updateRecordingState(recordingRowId!!, RecordingState.COMPLETE)
                }
            }
            onExitNewRecordingBtnPress()
        }
    }

    fun toggleRecord() {
        stateScope.launch {
            when (recorder.state()) {
                RecorderState.READY -> {
                    val newRecording = Recording(
                        name = "Recording",
                    )
                    try {
                        writer = repo.prepareForRecording(newRecording)

                        recorderScope.launch {
                            writer.beginSave(recorder.start())
                        }
                        recordingRowId = repo.addRecording(newRecording).await()
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
            recorderStateMutable.update { recorder.state() }
        }
    }
}