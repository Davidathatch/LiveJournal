package com.example.recorder.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.recorder.R
import com.example.recorder.state.RecordingExplorerState
import java.text.Format
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecordingExplorer(
    state: RecordingExplorerState
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recorder") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { state.onNewRecordingBtnPress() }
            ) {
                Icon(painterResource(R.drawable.add), contentDescription = "Add icon")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val recordings by state.recordings.collectAsState()
            LazyColumn {
                items(recordings, key = { it.id }) { recording ->
                    Box(
                        modifier = Modifier.animateItem()
                    ) {
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(25.dp, 5.dp)
                                .combinedClickable(
                                    onClick = { state.onRecordingCardPress(recording) },
                                    onLongClick = { dropdownExpanded = true },
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(15.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(recording.name)
                                Spacer(Modifier.width(15.dp))
                                Text(recording.timestamp.format(DateTimeFormatter.ofPattern("dd-MM-yyy HH:mm:ss")))
                            }
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        state.onRecordingDelete(recording)
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }


                    }
                }
            }
        }
    }
}