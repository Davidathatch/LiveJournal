package com.example.recorder

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class RecorderApplication: Application() {
    val scope = CoroutineScope(SupervisorJob())
}