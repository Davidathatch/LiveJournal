package com.example.recorder.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.recorder.R
import com.example.recorder.recorder.RecorderState
import com.example.recorder.state.NewRecordingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewRecording(
    state: NewRecordingState
) {
    Scaffold(
      topBar = {
          TopAppBar(
              title = { Text("New Recording") }
          )
      }
    ){ innerPadding ->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val recorderState by state.recorderState.collectAsState()
            FilledIconButton(
                onClick = {
                    state.toggleRecord()
                }
            ) {
                Icon(
                    when(recorderState) {
                        RecorderState.RECORDING -> painterResource(R.drawable.pause)
                        RecorderState.READY -> painterResource(R.drawable.record)
                        else -> painterResource(R.drawable.record)
                    },
                    when(recorderState) {
                        RecorderState.RECORDING -> "Pause icon"
                        RecorderState.READY -> "Record icon"
                        else -> "Record icon"
                    },
                )
            }
            FilledIconButton(
                onClick = {
                    state.exitNewRecording()
                }
            ) {
                Icon(
                    painterResource(R.drawable.stop),
                    "Stop icon"
                )
            }
        }
    }
}