package com.example.recorder.recorder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Used to write a stream of audio to a .wav file.
 *
 * @param file: the file to save data to
 */
class RecordingWriter(private val recordingFile: RecordingFile) {
    /**
     * Data is only written to the file when this value is false. Otherwise, any emitted
     * data is ignored.
     */
    private val doneWriting = AtomicBoolean(false)

    /**
     * Begins to save data to [recordingFile]. Starts by placing 44 bytes of zeros at the beginning of the
     * file as a placeholder for the WAV header, which will is inserted by [close]. Every
     * short array emitted by [dataFlow] is then appended to the file until [close] is called.
     *
     * @param dataFlow: emits short arrays to be written to [recordingFile]
     */
    suspend fun beginSave(dataFlow: Flow<ShortArray>) {
        if (doneWriting.get()) {
            return
        }
        withContext(Dispatchers.IO) {
            recordingFile.initialize()

            // Collect data from flow
            dataFlow
                .buffer()
                .collect { value ->
                    if (!doneWriting.get()) {
                        val byteBuffer = ByteBuffer.allocate(value.size * 2)
                        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                        for (i in 0 until value.size) {
                            byteBuffer.putShort(value[i])
                        }
                        if (byteBuffer.hasArray()) {
                            recordingFile.append(byteBuffer.array())
                        } else {
                            throw RuntimeException("Error converting short array to byte array")
                        }
                    }
                }
        }
    }

    suspend fun updateHeader() {
        withContext(Dispatchers.IO) {
            recordingFile.updateHeader()
        }
    }

    /**
     * Called after [Recorder.stop] is called. Closes file resources and writes the WAV header.
     */
    suspend fun close() {
        withContext(Dispatchers.IO) {
            doneWriting.set(true)
            recordingFile.close()
        }
    }
}