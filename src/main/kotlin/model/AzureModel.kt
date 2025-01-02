package cz.marvincz.transcript.tts.model

import com.microsoft.cognitiveservices.speech.SpeechSynthesisBoundaryType
import com.microsoft.cognitiveservices.speech.SpeechSynthesisWordBoundaryEventArgs
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.serialization.Contextual


@Serializable
data class AzureSpeaker(
    val voiceId: String,
    val expression: Expression? = null,
)

@Serializable
data class Expression(
    val style: String = "neutral",
    val role: String? = null,
    val styleDegree: Float? = null,
)

@Serializable
data class VoiceMapping(
    val voices: Map<String, AzureSpeaker>,
    val extras: List<ExtraVoices> = emptyList(),
)

@Serializable
data class ExtraVoices(
    @Contextual
    val speakerRegex: Regex,
    val voices: List<AzureSpeaker>,
    val assignment: AssignmentType,
) {
    enum class AssignmentType {
        SpeakerRoundRobin, LineRoundRobin
    }
}

@Serializable
data class SpeechPart(
    val speaker: AzureSpeaker,
    val speakerName: String,
    val text: String
)

@Serializable
data class Boundary(
    val type: SpeechSynthesisBoundaryType,
    val text: String,
    val offset: Duration,
    val duration: Duration,
    val textOffset: Int,
    val pause: Duration = Duration.ZERO,
) {
    constructor(e: SpeechSynthesisWordBoundaryEventArgs) : this(
        e.boundaryType,
        e.text,
        e.audioOffset.ticksToDuration(),
        e.duration.ticksToDuration(),
        e.textOffset.toInt(),
    )

    val endOffset = offset + duration

    val textEndOffset = textOffset + text.length

    fun withPauseTo(next: Boundary) = copy(pause = next.offset - endOffset)

    fun isPunctuation() = type == SpeechSynthesisBoundaryType.Punctuation && text != "("
}

fun Long.ticksToDuration() = (this * 100).nanoseconds