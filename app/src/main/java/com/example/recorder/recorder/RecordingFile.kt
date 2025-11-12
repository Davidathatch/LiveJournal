package com.example.recorder.recorder

import com.example.recorder.utils.WavHelper
import kotlinx.coroutines.sync.Semaphore
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer

class RecordingFile(
    private val file: File,
    val channelCount: Short = Recorder.RECORDER_CHANNEL_COUNT.toShort(),
    val sampleRate: Int = Recorder.RECORDER_SAMPLE_RATE,
    val bitsPerSample: Short = Recorder.RECORDER_BITS_PER_SAMPLE.toShort()
) {
    private val writingSem = Semaphore(1, 0)
    private var os: FileOutputStream? = file.outputStream()
    var contentSize = 0L

    /**
     * Initializes header with placeholder for WAV header
     */
    suspend fun initialize() {
        writingSem.acquire()
        if (os != null) {
            // Create placeholder for WAV header
            val placeholderBuffer = ByteBuffer.allocate(44)
            for (i in 1..44) {
                placeholderBuffer.put(0x0)
            }
            os?.write(placeholderBuffer.array())
        }
        writingSem.release()
    }

    /**
     * Writes data to the end of a file.
     *
     * @param data: data to write
     */
    suspend fun append(data: ByteArray) {
        writingSem.acquire()
        if (os != null) {
            os?.write(data)
            contentSize += data.size
        }
        writingSem.release()
    }

    /**
     * Overwrites data starting at [startIndex].
     *
     * @param startIndex: index of byte in the file where writing should begin
     * @param data: the data to write. Please ensure this data doesn't exceed [contentSize] + [startIndex]
     */
    suspend fun overwrite(startIndex: Long, data: ByteArray) {
        writingSem.acquire()
        RandomAccessFile(file, "rw").use {
            it.seek(startIndex)
            it.write(data)
        }
        writingSem.release()
    }

    suspend fun updateHeader() {
        val wavHeader = WavHelper.BuildHeader(
            contentSize.toInt(),
            channelCount,
            sampleRate,
            bitsPerSample
        )
        overwrite(0, wavHeader)
    }

    suspend fun close(
    ) {
        updateHeader()
        if (os != null) {
            writingSem.acquire()
            os?.close()
            os = null
            writingSem.release()
        }
    }
}