package cz.marvincz.transcript.tts

import cz.marvincz.transcript.tts.timing.Timing
import kotlin.time.Duration


const val subtitlesHeader = "WEBVTT"

fun getSubtitles(timings: List<Timing>, offset: Duration) = timings.joinToString("\n\n", prefix = "\n\n") {
    "${(it.start + offset).format()} --> ${(it.end + offset).format()}\n<v ${it.speaker}>${it.text}"
}

private fun Duration.format() = toComponents { hours, minutes, seconds, nanoseconds ->
    val milliseconds = nanoseconds / 1_000_000
    buildString {
        append(hours.toString().padStart(2, '0'))
        append(":")
        append(minutes.toString().padStart(2, '0'))
        append(":")
        append(seconds.toString().padStart(2, '0'))
        append(".")
        append(milliseconds.toString().padStart(3, '0'))
    }
}