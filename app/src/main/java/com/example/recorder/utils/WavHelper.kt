package com.example.recorder.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavHelper {
    companion object {

        /**
         * Creates a WAV file header to prefix PCM data
         *
         * @param contentSize: number of bytes of data to be stored
         * @param channelCount: number of channels
         * @param sampleRate: sample rate in hertz
         * @param bitsPerSample: number of bits in each sample
         * @return byte array containing little endian data
         */
        fun BuildHeader(
            contentSize: Int,
            channelCount: Short,
            sampleRate: Int,
            bitsPerSample: Short
        ): ByteArray{
            val header = ByteBuffer.allocate(44)
            header.order(ByteOrder.LITTLE_ENDIAN)

            // ------------------------------------
            // Master RIFF chunk ------------------
            // ------------------------------------

            // File type block ID
            header.put(0x52) // R
            header.put(0x49) // I
            header.put(0x46) // F
            header.put(0x46) // F

            // File size, minus 8 bytes
            header.putInt(contentSize + 36)

            // File format
            header.put(0x57) // W
            header.put(0x41) // A
            header.put(0x56) // V
            header.put(0x45) // E

            // ------------------------------------
            // Data format info -------------------
            // ------------------------------------

            // Format block ID
            header.put(0x66) // f
            header.put(0x6D) // m
            header.put(0x74) // t
            header.put(0x20) // [SPACE]

            // Block size
            header.putInt(0x10)

            // Audio format (1 = PCM integer)
            header.putShort(0x1)

            // Channel count
            header.putShort(channelCount)

            // Sample rate
            header.putInt(sampleRate)

            // Bytes to read per second
            header.putInt(channelCount.toInt() * bitsPerSample.toInt() / 8)

            // Bytes per block
            header.putShort((channelCount * bitsPerSample / 8).toShort())

            // Bits per sample
            header.putShort(bitsPerSample)

            // ------------------------------------
            // Sampled data info ------------------
            // ------------------------------------

            // Data block ID
            header.put(0x64) // d
            header.put(0x61) // a
            header.put(0x74) // t
            header.put(0x61) // a

            header.putInt(contentSize)

            return header.array()
        }
    }
}