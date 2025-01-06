package cz.marvincz.transcript.tts.model

import kotlinx.serialization.Serializable

@Serializable
data class Transcript(
    val speakers: List<String>,
    val lines: List<Line>,
)

@Serializable
data class Line(
    val type: LineType,
    val speaker: String? = null,
    val text: String? = null,
    val page: String,
    val line: Int,
) {
    fun sameSpeaker(other: Line): Boolean = speaker == other.speaker && type == other.type && type == LineType.SPEECH

    val speakerOrNarrator = speaker ?: "NARRATOR"
}

enum class LineType {
    SPEECH,
    HEADER,
    ANNOTATION,
    INFO,
    RULER,
    PARAGRAPH
}

fun List<Line>.joinTexts(): List<Line> = runningReduce { acc, line ->
    if (acc.sameSpeaker(line)) line.text?.let { acc.copy(text = "${acc.text} ${line.text}") } ?: acc
    else line
}.filterIndexed { index, line -> index == lastIndex || !line.sameSpeaker(get(index + 1)) }