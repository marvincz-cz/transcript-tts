package cz.marvincz.transcript.tts.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AudioUtilsTest {
    @Test
    fun `little endian 32_767`() {
        val value: Short = 32_767
        testLittleEndian(value, byteArrayOf(0xFF.toByte(), 0x7F.toByte()))
    }
    @Test
    fun `little endian -32_768`() {
        val value: Short = -32_768
        testLittleEndian(value, byteArrayOf(0x00.toByte(), 0x80.toByte()))
    }

    @Test
    fun `little endian 0`() {
        val value: Short = 0
        testLittleEndian(value, byteArrayOf(0x00.toByte(), 0x00.toByte()))
    }

    private fun testLittleEndian(value: Short, expectedArray: ByteArray) {
        val array = ByteArray(2)
        array.shortToLittleEndian(0, value)

        Assertions.assertArrayEquals(expectedArray, array)

        val back = array.littleEndianToShort(0)

        Assertions.assertEquals(value, back)
    }
}