package cz.marvincz.transcript.tts.client

import cz.marvincz.transcript.tts.model.Boundary
import cz.marvincz.transcript.tts.model.SpeechPart
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun getTimings(speeches: List<SpeechPart>, ssml: String, boundaries: List<Boundary>): List<Timing> {
    var index = 0

    return speeches.flatMap { speech ->
        val speaker = speech.speakerName
        val sentences = sentenceRegex.findAll(speech.text).map { it.groups[1]!!.value }

        val timings = mutableListOf<Timing>()
        var wipTiming: Timing? = null
        sentences.forEach { sentence ->
            val xmlSentence = sentence.fixTexts()
            index = ssml.indexOf(xmlSentence, index + 1)

            val start = boundaries.firstOrNull { it.textOffset == index }?.offset ?: wipTiming?.start
            val end = boundaries.firstOrNull { it.textOffset == index + xmlSentence.length - 1 }?.endOffset

            val timing = wipTiming.join(Timing(speaker, sentence, start ?: 0.milliseconds, end ?: 0.milliseconds))
            wipTiming = null

            if (start != null && end != null) {
                timings.add(timing)
            } else if (start == null && end == null) {
                throw Exception("Could not find timing for \"$sentence\"")
            } else if (start == null) {
                timings.add(timings.removeLast().join(timing))
            } else {
                wipTiming = timing
            }
        }
        // If we didn't find the end for the last sentence in this <voice> tag, use the last sound in the tag for the end time
        wipTiming?.let {
            val voiceEnd = ssml.indexOf("</voice>", index)
            timings.add(it.copy(end = boundaries.last { it.textOffset < voiceEnd}.endOffset))
        }

        return@flatMap timings
    }
}

data class Timing(
    val speaker: String,
    val text: String,
    val start: Duration,
    val end: Duration,
) {
    fun join(other: Timing): Timing {
        require(speaker == other.speaker) { "Only lines from the same speaker can be joined for timing" }
        return copy(
            text = "$text ${other.text}",
            end = other.end,
        )
    }
}

private fun Timing?.join(other: Timing): Timing = this?.join(other) ?: other

private val notSeparators = listOf("Mr", "Mrs", "Ms", "Dr").joinToString("") { "(?<!$it)" }
private val sentenceRegex = Regex("\\s*(.+?$notSeparators(?<separator>\\p{Po}|--|$))(?> |$)")