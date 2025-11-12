package com.example.recorder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDAO {
    @Query("SELECT * FROM Recording")
    fun getAllRecordings(): Flow<List<Recording>>

    @Insert
    fun insert(recording: Recording): Long

    @Update
    fun update(recording: Recording)

    @Query("UPDATE Recording SET state = :state WHERE id = :id")
    fun updateRecordingState(id: Long, state: RecordingState)

    @Query("DELETE FROM Recording WHERE id = :id")
    fun deleteRecordingFromId(id: Long)

    @Delete
    fun delete(recording: Recording)
}