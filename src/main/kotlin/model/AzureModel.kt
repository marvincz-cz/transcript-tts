package cz.marvincz.transcript.tts.model

import com.microsoft.cognitiveservices.speech.SpeechSynthesisBoundaryType
import com.microsoft.cognitiveservices.speech.SpeechSynthesisWordBoundaryEventArgs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds


data class AzureSpeaker(
    val voiceId: String,
    val expression: Expression? = null,
)

data class Expression(
    val style: String = "neutral",
    val role: String? = null,
    val styleDegree: Float? = null,
)

data class SpeechPart(
    val speaker: AzureSpeaker,
    val speakerName: String,
    val text: String
)

data class Boundary(
    val type: SpeechSynthesisBoundaryType,
    val text: String,
    val offset: Duration,
    val duration: Duration,
    val textOffset: Int,
) {
    constructor(e: SpeechSynthesisWordBoundaryEventArgs) : this(
        e.boundaryType,
        e.text,
        e.audioOffset.ticksToDuration(),
        e.duration.ticksToDuration(),
        e.textOffset.toInt(),
    )

    val endOffset
        get() = offset + duration
}

fun Long.ticksToDuration() = (this * 100).nanoseconds