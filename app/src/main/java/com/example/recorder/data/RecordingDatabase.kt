package com.example.recorder.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Recording::class], version = 1)
@TypeConverters(Converters::class)
abstract class RecordingDatabase: RoomDatabase() {
    abstract fun recordingDAO(): RecordingDAO
}