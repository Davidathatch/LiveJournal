package com.example.recorder.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.recorder.R
import com.example.recorder.recorder.RecorderState
import com.example.recorder.state.InitializationState
import com.example.recorder.state.NewRecordingState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val recorderState by state.recorderState.collectAsState()
            val initializationState by state.initialized.collectAsState()

            Card(Modifier.weight(0.85f)) {
            }

            ButtonGroup(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp)
                    .weight(0.15f),
                overflowIndicator = { menuState ->
                    ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                }
            ) {
                toggleableItem(
                    enabled = initializationState == InitializationState.INITIALIZED,
                    checked = recorderState == RecorderState.RECORDING,
                    onCheckedChange = { state.toggleRecord() },
                    label = if (recorderState != RecorderState.RECORDING) "Record" else "Pause",
                    weight = 0.6f,
                    icon = {
                        if (recorderState != RecorderState.RECORDING)
                            Icon(painterResource(R.drawable.record), "Record icon")
                        else
                            Icon(painterResource(R.drawable.pause), "Pause icon")
                    })
                toggleableItem(
                    enabled = initializationState == InitializationState.INITIALIZED,
                    checked = recorderState == RecorderState.STOPPED,
                    onCheckedChange = { state.onStopBtnPressed() },
                    label = "Stop",
                    weight = 0.4f,
                    icon = { Icon(painterResource(R.drawable.stop), "Stop icon") })
            }
        }
    }
}