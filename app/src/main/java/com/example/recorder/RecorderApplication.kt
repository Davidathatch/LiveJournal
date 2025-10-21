package com.example.recorder

import android.app.Application
import androidx.room.Room
import com.example.recorder.data.RecordingDatabase
import com.example.recorder.data.RecordingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class RecorderApplication: Application() {
    val scope = CoroutineScope(SupervisorJob())

    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            RecordingDatabase::class.java,
            "recordings_db"
        ).build()
    }

    val repo by lazy {
        RecordingRepository(scope, filesDir, db)
    }
}