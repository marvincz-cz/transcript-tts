package cz.marvincz.transcript.tts.utils

import java.io.Closeable
import java.io.File
import java.io.SequenceInputStream
import java.util.Collections.enumeration
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


fun ByteArray.muteSection(
    format: AudioFormat,
    start: Duration,
    end: Duration,
    fadeLength: Duration = 50.milliseconds
) = apply {
    val startIndex = frameIndex(start, format)
    val endIndex = frameIndex(end, format)

    for (i in startIndex..endIndex step 2) {
        shortToLittleEndian(i, 0)
    }

    val startFadeIndex = frameIndex(start - fadeLength, format)
    val fadeOutLength = startIndex - startFadeIndex
    for (i in max(startFadeIndex, 0) until startIndex step 2) {
        val muted = littleEndianToShort(i) * (startIndex.toLong() - i) / fadeOutLength
        shortToLittleEndian(i, muted.toShort())
    }

    val endFadeIndex = frameIndex(end + fadeLength, format)
    val fadeInLength = endFadeIndex - endIndex
    for (i in endIndex..min(endFadeIndex, lastIndex - 1) step 2) {
        val muted = littleEndianToShort(i) * (i - endIndex.toLong()) / fadeInLength
        shortToLittleEndian(i, muted.toShort())
    }
}

fun combineAudioFiles(files: List<File>, output: File) {
    Streams.ofFiles(files).use { streams ->
        AudioInputStream(streams.sequence, streams.format, streams.length).use {
            AudioSystem.write(it, AudioFileFormat.Type.WAVE, output)
        }
    }
}

fun audioDuration(files: List<File>) = Streams.ofFiles(files).use { it.duration }

private fun frameIndex(duration: Duration, format: AudioFormat) =
    format.frameSize * (duration.inWholeMilliseconds * format.sampleRate / 1000).toInt()

// public for test only
fun ByteArray.littleEndianToShort(index: Int): Short {
    var value: Int = get(index).toInt() and 255
    value = value or get(index + 1).toInt().and(255).shl(8)
    return value.toShort()
}

// public for test only
fun ByteArray.shortToLittleEndian(index: Int, value: Short) {
    set(index, value.toByte())
    set(index + 1, (value.toInt() ushr 8).toByte())
}

val AudioInputStream.duration
    get() = (1_000 * frameLength / format.sampleRate).toLong().milliseconds

/**
 * Helper class for cleaner work with a sequence of streams
 */
private data class Streams(val streams: List<AudioInputStream>) : Closeable {
    override fun close() = streams.forEach { it.close() }

    val sequence: SequenceInputStream
        get() = SequenceInputStream(enumeration(streams))

    val format: AudioFormat
        get() = streams.first().format

    val length: Long
        get() = streams.sumOf { it.frameLength }

    val duration: Duration
        get() = streams.fold(Duration.ZERO) { acc, stream -> acc + stream.duration }

    companion object {
        fun ofFiles(files: List<File>) = Streams(files.map { AudioSystem.getAudioInputStream(it) })
    }
}