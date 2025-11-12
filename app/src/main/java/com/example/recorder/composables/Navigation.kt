package com.example.recorder.composables

import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.recorder.state.RecorderVM
import kotlinx.serialization.Serializable

@Serializable
object RecordingExplorer

@Serializable
object NewRecording

@Composable
fun RecorderNav(
    navController: NavHostController,
    vm: RecorderVM
) {
    NavHost(
        navController = navController,
        startDestination = RecordingExplorer
    ) {
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            if (destination.route != null) {
                vm.onNavigationChange(destination.route!!)
            }
        }
        composable<RecordingExplorer> {
            vm.recordingExplorerState.setOnNewRecordingReq {
                navController.navigate(NewRecording)
            }
            RecordingExplorer(vm.recordingExplorerState)
        }
        composable<NewRecording> {
            val state = vm.newRecordingState
            state.setOnExitNewRecordingBtnPress { navController.popBackStack() }
            NewRecording(state)
        }
    }
}