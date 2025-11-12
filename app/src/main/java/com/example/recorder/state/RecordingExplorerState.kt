package com.example.recorder.state

import com.example.recorder.data.Recording
import kotlinx.coroutines.flow.StateFlow

class RecordingExplorerState(
    val recordings: StateFlow<List<Recording>>,
    val onRecordingDelete: (Recording) -> Unit = {},
    var onNewRecordingBtnPress: () -> Unit = {},
    var onRecordingCardPress: (Recording) -> Unit = {}
) {
    fun setOnNewRecordingReq(callback: () -> Unit) {
        onNewRecordingBtnPress = callback
    }

    fun setOnRecordingSelected(callback: (Recording) -> Unit) {
        onRecordingCardPress = callback
    }
}