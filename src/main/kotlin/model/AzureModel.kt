package cz.marvincz.transcript.tts.model

import com.microsoft.cognitiveservices.speech.SpeechSynthesisBoundaryType
import com.microsoft.cognitiveservices.speech.SpeechSynthesisWordBoundaryEventArgs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable


@Serializable
data class AzureSpeaker(
    val voiceId: String,
    val expression: Expression? = null,
)

@Serializable
data class SpeakerInfo(
    val speaker: AzureSpeaker,
    val speakerType: SpeakerType,
)

@Serializable
data class Expression(
    val style: String = "neutral",
    val role: String? = null,
    val styleDegree: Float? = null,
)

@Serializable
data class VoiceMapping(
    val voices: Map<String, SpeakerInfo>,
    val extras: List<ExtraVoices> = emptyList(),
)

@Serializable
data class ExtraVoices(
    @Contextual
    val speakerRegex: Regex?,
    val speakers: List<String>?,
    val voices: List<AzureSpeaker>,
    val speakerType: SpeakerType,
    val assignment: AssignmentType,
) {
    init {
        requireNotNull(speakerRegex ?: speakers) {
            "Either speakerRegex or speakers must be provided."
        }
    }

    fun matches(speaker: String): Boolean = (speakerRegex?.matches(speaker) ?: speakers?.contains(speaker)) == true

    enum class AssignmentType {
        SpeakerRoundRobin, LineRoundRobin
    }
}

@Suppress("unused")
enum class SpeakerType {
    Narrator,
    Court,
    Crown,
    Defense,
    Witness,
    Other
}

@Serializable
data class SpeechPart(
    val speaker: SpeakerInfo,
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
}

fun Long.ticksToDuration() = (this * 100).nanoseconds