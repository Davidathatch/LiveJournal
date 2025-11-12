package com.example.recorder.data

import android.media.AudioFormat
import android.media.MediaRecorder
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.recorder.recorder.Recorder
import java.time.LocalDateTime
import java.util.UUID

@Entity
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "Recording",
    val fileName: String = "${UUID.randomUUID()}.wav",
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val state: RecordingState = RecordingState.INCOMPLETE,
    val channelCount: Int = Recorder.RECORDER_CHANNEL_COUNT,
    val encoding: Int = Recorder.RECORDER_AUDIO_ENCODING,
    val bitsPerSample: Int = Recorder.RECORDER_BITS_PER_SAMPLE,
    val audioSource: Int = Recorder.RECORDER_AUDIO_SOURCE,
    val sampleRate: Int = Recorder.RECORDER_SAMPLE_RATE
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