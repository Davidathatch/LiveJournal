package com.example.recorder

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.InternalPlatformDsl.toArray
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import java.io.FileOutputStream

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @MockK
    private lateinit var mockOS: FileOutputStream

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun mockOS() {
        val argSlot = slot<ByteArray>()
        every { mockOS.write(capture(argSlot)) } answers {
            Log.d("mockOS", argSlot.captured.toArray().contentToString())
        }
        mockOS.write(byteArrayOf(0x0, 0x1, 0x2))
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.recorder", appContext.packageName)
    }
}