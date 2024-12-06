package cz.marvincz.transcript.tts.timing

import cz.marvincz.transcript.tts.model.Boundary
import cz.marvincz.transcript.tts.model.SpeechPart
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface TimingGenerator {
    fun getTimings(speeches: List<SpeechPart>, ssml: String, boundaries: List<Boundary>): List<Timing>
}

data class Timing(
    val speaker: String,
    val text: String,
    val start: Duration,
    val end: Duration,
    val pause: Duration = 0.milliseconds,
) {
    fun join(other: Timing): Timing {
        require(speaker == other.speaker) { "Only lines from the same speaker can be joined for timing" }
        return copy(
            text = "$text ${other.text}",
            end = other.end,
            pause = other.pause,
        )
    }

    fun joinFitness(other: Timing) =
        joinedLength(other) * (pause.inWholeMilliseconds + 100) * text.separatorFitnessWeight()

    fun joinedLength(other: Timing) = text.length + other.text.length + 1
}

fun Timing?.join(other: Timing): Timing = this?.join(other) ?: other

/**
 * We prefer to join lines in some places as opposed to others
 */
fun String.separatorFitnessWeight() = when {
    endsWith(',') -> 0.7
    last() in listOf('&', '\'', '%', '*') -> 0.4
    last().isLetterOrDigit() -> 0.4
    endsWith(';') -> 0.8
    endsWith("--") -> 1.2
    else -> 1.0
}