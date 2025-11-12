package com.example.recorder.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.recorder.state.RecordingPlaybackState

@Composable
fun RecordingPlayback(state: RecordingPlaybackState) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val transcription by state.transcription.collectAsState()

        Text(transcription)
        Button(
            onClick = { state.transcribe() }
        ) {
            Text("Transcribe")
        }
    }
}