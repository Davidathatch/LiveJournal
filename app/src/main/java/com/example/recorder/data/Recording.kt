package com.example.recorder.data

import android.media.AudioFormat
import android.media.MediaRecorder
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.util.UUID

@Entity
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "Recording",
    val fileName: String = "${UUID.randomUUID()}.wav",
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val state: RecordingState = RecordingState.INCOMPLETE,
    val channelCount: Int = 1,
    val encoding: Int = AudioFormat.ENCODING_PCM_16BIT,
    val bitsPerSample: Int = 16,
    val audioSource: Int = MediaRecorder.AudioSource.MIC,
    val sampleRate: Int = 44000
)

class Converters {
    @TypeConverter
    fun stringToRecordingState(value: String?): RecordingState? {
        return when (value) {
            RecordingState.COMPLETE.value -> RecordingState.COMPLETE
            RecordingState.INCOMPLETE.value -> RecordingState.INCOMPLETE
            else -> null
        }
    }

    @TypeConverter
    fun recordingStateToString(recordingState: RecordingState?): String? {
        return when (recordingState) {
            RecordingState.COMPLETE -> RecordingState.COMPLETE.value
            RecordingState.INCOMPLETE -> RecordingState.INCOMPLETE.value
            else -> null
        }
    }

    @TypeConverter
    fun stringToLocalDateTime(value: String?): LocalDateTime {
        return LocalDateTime.parse(value)
    }

    @TypeConverter
    fun localDateTimeToString(localDateTime: LocalDateTime?): String? {
        return localDateTime.toString()
    }
}

enum class RecordingState(val value: String) {
    /**
     * Recording completed normally-- [Recording.fileName] has been fully processed
     */
    COMPLETE("Complete"),

    /**
     * Recording was started but hasn't been fully processed (WAV header may be missing)
     */
    INCOMPLETE("Incomplete")
}