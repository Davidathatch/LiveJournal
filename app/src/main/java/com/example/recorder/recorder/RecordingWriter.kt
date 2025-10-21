package com.example.recorder.recorder

import com.example.recorder.utils.WavHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Used to write a stream of audio to a .wav file.
 *
 * @param file: the file to save data to
 */
class RecordingWriter(private val file: File) {
    /**
     * Number of bytes that have been saved
     */
    private var contentSize = 0

    /**
     * Output stream to write data to
     */
    private val os: FileOutputStream = file.outputStream()

    /**
     * Restricts file access to a single coroutine
     */
    private val writingSem = Semaphore(1, 0)

    /**
     * Data is only written to the file when this value is false. Otherwise, any emitted
     * data is ignored.
     */
    private val doneWriting = AtomicBoolean(false)

    /**
     * Begins to save data to [file]. Starts by placing 44 bytes of zeros at the beginning of the
     * file as a placeholder for the WAV header, which will is inserted by [finishSave]. Every
     * short array emitted by [dataFlow] is then appended to the file until [finishSave] is called.
     *
     * @param dataFlow: emits short arrays to be written to [file]
     */
    suspend fun beginSave(dataFlow: Flow<ShortArray>) {
        if (doneWriting.get()) {
            return
        }
        contentSize = 0
        withContext(Dispatchers.IO) {
            writingSem.acquire()

            // Create placeholder for WAV header
            val placeholderBuffer = ByteBuffer.allocate(44)
            for (i in 1..44) {
                placeholderBuffer.put(0x0)
            }
            os.write(placeholderBuffer.array())
            writingSem.release()

            // Collect data from flow
            dataFlow
                .buffer()
                .collect { value ->
                    if (!doneWriting.get()) {
                        writingSem.acquire()
                        val byteBuffer = ByteBuffer.allocate(value.size * 2)
                        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                        for (i in 0 until value.size) {
                            byteBuffer.putShort(value[i])
                        }
                        contentSize += value.size * 2
                        if (byteBuffer.hasArray()) {
                            os.write(byteBuffer.array())
                        } else {
                            throw RuntimeException("Error converting short array to byte array")
                        }
                        writingSem.release()
                    }
                }
        }
    }

    /**
     * Called after [Recorder.stop] is called. Closes file resources and writes the WAV header.
     */
    suspend fun finishSave() {
        withContext(Dispatchers.IO) {
            writingSem.acquire()
            os.close()
            doneWriting.set(true)

            val wavHeader = WavHelper.BuildHeader(
                contentSize,
                Recorder.RECORDER_CHANNEL_COUNT.toShort(),
                Recorder.RECORDER_SAMPLE_RATE,
                Recorder.RECORDER_BITS_PER_SAMPLE.toShort()
            )
            RandomAccessFile(file, "rw").use {
                it.seek(0)
                it.write(wavHeader)
            }
            writingSem.release()
        }
    }
}