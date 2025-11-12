package com.example.recorder

import com.example.recorder.recorder.RecordingFile
import com.example.recorder.recorder.RecordingWriter
import io.mockk.CapturingSlot
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.milliseconds

class RecorderTests {
    @MockK
    private lateinit var mockRecordingFile: RecordingFile
    private lateinit var writtenValues: ByteBuffer
    private lateinit var recordingFileDataSlot: CapturingSlot<ByteArray>
    private lateinit var startIndexSlot: CapturingSlot<Long>
    private val tempFolder = TemporaryFolder()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        tempFolder.create()

        // Mock recording file default initialization
        writtenValues = ByteBuffer.allocate(2_005)
        writtenValues.order(ByteOrder.LITTLE_ENDIAN)
        recordingFileDataSlot = slot<ByteArray>()
        startIndexSlot = slot<Long>()
        coEvery { mockRecordingFile.append(capture(recordingFileDataSlot)) } coAnswers {
            writtenValues.put(recordingFileDataSlot.captured)
        }
        coEvery {
            mockRecordingFile.overwrite(
                capture(startIndexSlot),
                capture(recordingFileDataSlot)
            )
        } coAnswers { }
    }

    @Test
    fun recordingFileAppend() {
        val tempFile = tempFolder.newFile("data.wav")
        val recordingFile = RecordingFile(tempFile)

        val arrSize = 10_000_000
        val arrOne = ByteArray(arrSize) { 1 }
        val arrTwo = ByteArray(arrSize) { 2 }
        val arrThree = ByteArray(arrSize) { 3 }

        runBlocking {
            recordingFile.initialize()
            coroutineScope {
                launch {
                    recordingFile.append(arrOne)
                    recordingFile.append(arrTwo)
                    recordingFile.append(arrThree)
                }
            }
        }

        tempFile.inputStream().use {
            val content = it.readBytes()

            // Verify that file consists of 44-byte header placeholder and all 3,000 written bytes
            assert(content.size == arrSize * 3 + 44)

            // Check that all data was written completely and in-order
            for (i in 44 until (arrSize + 44))
                assert(content[i] == 1.toByte())
            for (i in (arrSize + 44) until (arrSize * 2 + 44))
                assert(content[i] == 2.toByte())
            for (i in (arrSize * 2 + 44) until (arrSize * 3 + 44))
                assert(content[i] == 3.toByte())
        }
    }

    @Test
    fun recordingFileInitialization() {
        val tempFile = tempFolder.newFile("data.wav")
        val recordingFile = RecordingFile(tempFile)

        runBlocking {
            recordingFile.initialize()
        }

        // Verify initialized file only contains 44-byte WAV header placeholder
        tempFile.inputStream().use {
            val headerBytes = it.readBytes()
            assert(headerBytes.size == 44)
            for (i in 0 until 44)
                assert(headerBytes[i] == 0.toByte())
        }
    }

    @Test
    fun recordingWriterClose() {
        runBlocking {
            var fileClosed = false
            coEvery { mockRecordingFile.initialize() } coAnswers { }
            coEvery { mockRecordingFile.close() } coAnswers { fileClosed = true }

            val writer = RecordingWriter(mockRecordingFile)
            val testFlow = flow {
                for (i in 1..100) {
                    // After emitting 50 values, close the writer
                    if (i == 51)
                        writer.close()
                    emit(shortArrayOf(1.toShort()))
                    delay(10.milliseconds)
                }
            }

            runBlocking {
                writer.beginSave(testFlow)
            }

            // Verify that the first 50 shorts are equal to 5, with the rest being equal to 0
            for (i in 0 until writtenValues.capacity() - 2 step 2) {
                assert(writtenValues.getShort(i) == if (i < 100) 1.toShort() else 0.toShort())
            }
            assert(fileClosed)
        }
    }

    @Test
    fun recordingWriterDataLossAndOrdering() {
        runBlocking {
            // Mock recording file treats the first five bytes of writtenValues as the header. When the recording
            // file is initialized, these bytes are each set to 1. When the header is updated after initializing
            // and before closing, they are set to 2. When the file is closed, they are set to 3.
            coEvery { mockRecordingFile.close() } coAnswers {
                writtenValues.put(
                    0,
                    byteArrayOf(3, 3, 3, 3, 3)
                )
            }
            coEvery { mockRecordingFile.initialize() } coAnswers {
                writtenValues.put(
                    byteArrayOf(
                        1,
                        1,
                        1,
                        1,
                        1
                    )
                )
            }
            coEvery { mockRecordingFile.updateHeader() } coAnswers {
                writtenValues.put(
                    0,
                    byteArrayOf(2, 2, 2, 2, 2)
                )
            }

            // Emits 100 10-element byte arrays, one every 100 ms. Each array is filled with
            // a repeated value, with the first array containing 5 and the 100th containing 105.
            val testFlow = flow {
                var count = 5
                while (count < 105) {
                    val toSend = mutableListOf<Short>()
                    for (i in 1..10) {
                        toSend.add(count.toShort())
                    }
                    emit(toSend.toShortArray())
                    count++
                    delay(100.milliseconds)
                }
            }
            val writer = RecordingWriter(mockRecordingFile)

            // Check that all data was written
            runBlocking {
                writer.beginSave(testFlow)
            }

            for (i in 0 until 4) {
                assert(writtenValues.get(i) == 1.toByte())
            }
            var current = 5.toShort()
            var index = 5
            (1..100).forEach { i ->
                (1..10).forEach { i ->
                    assert(writtenValues.getShort(index) == current)
                    index += 2
                }
                current++
            }

            // Check that updating the header leaves other data untouched
            runBlocking {
                writer.updateHeader()
            }
            for (i in 0 until 4) {
                assert(writtenValues.get(i) == 2.toByte())
            }
            current = 5.toShort()
            index = 5
            (1..100).forEach { i ->
                (1..10).forEach { i ->
                    assert(writtenValues.getShort(index) == current)
                    index += 2
                }
                current++
            }

            // Check that finishing the save updates the header a final time and leaves other data untouched
            runBlocking {
                writer.close()
            }
            for (i in 0 until 4) {
                assert(writtenValues.get(i) == 3.toByte())
            }
            current = 5.toShort()
            index = 5
            (1..100).forEach { i ->
                (1..10).forEach { i ->
                    assert(writtenValues.getShort(index) == current)
                    index += 2
                }
                current++
            }
        }
    }
}