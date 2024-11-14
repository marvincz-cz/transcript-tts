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