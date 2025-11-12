package com.example.recorder.data

import android.util.Log
import androidx.compose.material3.carousel.rememberCarouselState
import com.example.recorder.recorder.RecordingFile
import com.example.recorder.recorder.RecordingWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File

/**
 * For working with persistent recording data
 *
 * @param scope: coroutine scope to run asynchronous operations in
 * @param filesDir: app-specific files directory
 */
class RecordingRepository(
    private val scope: CoroutineScope,
    private val filesDir: File,
    db: RecordingDatabase
) {
    private val recordingDAO = db.recordingDAO()
    private val recordingsDir = filesDir.resolve("audio")

    /**
     * Flow containing all recordings in the database
     */
    val allRecordings = recordingDAO.getAllRecordings()

    /**
     * Adds a recording to the database
     *
     * @param recording: the recording to add
     * @return row ID of recording in database
     */
    fun addRecording(recording: Recording): Deferred<Long> {
        return scope.async(Dispatchers.IO) {
            recordingDAO.insert(recording)
        }
    }

    /**
     * Updates a recording in the database
     *
     * @param recording: the updated recording with [Recording.id] set to the ID of the row
     * to be updated
     */
    fun updateRecording(recording: Recording) {
        scope.launch(Dispatchers.IO) {
            recordingDAO.update(recording)
        }
    }

    /**
     * Updates the state value of a recording
     *
     * @param id: ID of the recording whose state is being updated
     * @param state: new state value
     */
    fun updateRecordingState(id: Long, state: RecordingState) {
        scope.launch(Dispatchers.IO) {
            recordingDAO.updateRecordingState(id, state)
        }
    }

    fun deleteRecordingFromId(recording: Recording, id: Long) {
        scope.launch(Dispatchers.IO) {
            try {
                File(recordingsDir, recording.fileName).delete()
            } catch (e: Exception) {
                Log.e(
                    "Delete Recording",
                    "Failed to delete recording file ${recording.fileName}",
                    e
                )
            }
            recordingDAO.deleteRecordingFromId(id)
        }
    }

    /**
     * Deletes a recording from the database and all its associated data
     *
     * @param recording: the recording to delete
     */
    fun deleteRecording(recording: Recording) {
        scope.launch(Dispatchers.IO) {
            try {
                File(recordingsDir, recording.fileName).delete()
            } catch (e: Exception) {
                Log.e(
                    "Delete Recording",
                    "Failed to delete recording file ${recording.fileName}",
                    e
                )
            }
            recordingDAO.delete(recording)
        }
    }

    /**
     * Creates a recording writer configured to write data associated with [recording]
     *
     * @param recording: recording being prepared
     * @return [RecordingWriter] recording writer to write data for [recording]
     */
    fun prepareForRecording(recording: Recording = Recording()): RecordingWriter {
        filesDir.resolve("audio").mkdir()
        val toSave = File(filesDir.resolve("audio"), recording.fileName)
        toSave.createNewFile()
        val recordingFile = RecordingFile(toSave)
        return RecordingWriter(recordingFile)
    }

    fun getRecordingFile(recording: Recording): File {
        return File(filesDir.resolve("audio"), recording.fileName)
    }
}