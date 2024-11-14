package cz.marvincz.transcript.tts.client

import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesisBoundaryType
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat
import com.microsoft.cognitiveservices.speech.SpeechSynthesisWordBoundaryEventArgs
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import com.microsoft.cognitiveservices.speech.VoiceInfo
import java.io.ByteArrayInputStream
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.serialization.Serializable

class Client(subscriptionKey: String, region: String) {

    private val speechConfig = SpeechConfig.fromSubscription(subscriptionKey, region).apply {
        // PCM_SIGNED 48000.0 Hz, 16 bit, mono, 2 bytes/frame, little-endian
        setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Riff48Khz16BitMonoPcm)
    }

    fun call(speeches: List<SpeechPart>): TtsResult {
        val ssml = toSSML(speeches)
        val speechSynthesizer = SpeechSynthesizer(speechConfig, null)

        val boundaries = mutableListOf<Boundary>()
        speechSynthesizer.WordBoundary.addEventListener { _, e ->
            val boundary = Boundary(e)
            boundaries.add(boundary)
            println(boundary)
        }

        val result = speechSynthesizer.SpeakSsml(ssml)

        val audioInputStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(result.audioData))
        val format = audioInputStream.format

        // Azure reports wrong timing information, we try to correct it
        val correctDuration = (1_000 * audioInputStream.frameLength / format.sampleRate).toLong().milliseconds
        val correctionRatio = correctDuration / result.audioDuration.ticksToDuration()
        boundaries.replaceAll {
            it.copy(offset = it.offset * correctionRatio, duration = it.duration * correctionRatio)
        }

        return TtsResult(
            audioData = result.audioData.muteIndiscernible(boundaries, format),
            boundaries = boundaries,
            timings = getTimings(speeches, ssml, boundaries),
            duration = correctDuration,
        )
    }

    fun getVoices() {
        val speechSynthesizer = SpeechSynthesizer(speechConfig, null)

        val voices =
            speechSynthesizer.voicesAsync.get().voices.filter { "Multilingual" in it.localName || it.locale == "en-CA" }
        voices.forEach { voice ->
            testSpeech(speechSynthesizer, voice)

            voice.styleList.forEach { style ->
                testSpeech(speechSynthesizer, voice, style)
            }
        }
    }

    private fun testSpeech(
        speechSynthesizer: SpeechSynthesizer,
        voice: VoiceInfo,
        style: String? = null
    ) {
        val speechPart = SpeechPart(
            speaker = AzureSpeaker(voice.shortName, style?.let { Expression(style = style) }),
            speakerName = "${voice.localName} (${voice.shortName})",
            text = "Yes, My Lord. Okay. So the -- P-4 -- what is the document entitled? The case involves " +
                    "an allegation that on or about the 9th day of August, 2016, at or near Biggar, Saskatchewan, " +
                    "Gerald Stanley unlawfully caused the death of Colten Boushie and thereby committed " +
                    "second degree murder."
        )
        val data = speechSynthesizer.SpeakSsml(toSSML(listOf(speechPart))).audioData!!

        File("data/voices/${voice.localName} (${voice.shortName}) - $style.mp3").writeBytes(data)
    }
}

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

class TtsResult(
    val audioData: ByteArray,
    val boundaries: MutableList<Boundary>,
    val timings: List<Timing>,
    val duration: Duration,
)

fun toSSML(speeches: List<SpeechPart>) = buildString {
    append("<speak version=\"1.0\" xmlns=\"https://www.w3.org/2001/10/synthesis\" xmlns:mstts=\"https://www.w3.org/2001/mstts\" xml:lang=\"en-CA\">")

    speeches.forEach { speech ->
        val text = speech.text.fixTexts()
        append("<voice name=\"${speech.speaker.voiceId}\">")
        speech.speaker.expression?.let { expression ->
            append("<mstts:express-as style=\"${expression.style}\"")
            expression.role?.let { append(" role=\"$it\"") }
            expression.styleDegree?.let { append(" styledegree=\"$it\"") }
            append(">")
            append(text)
            append("</mstts:express-as>")
        } ?: run {
            append(text)
        }
        append("</voice>")
    }
    append("</speak>")
}

private fun String.fixTexts() = escapeXml()
    .replace("[sic]", "")
    .replace("(NO AUDIBLE RESPONSE)", "<mstts:silence type=\"Leading-exact\" value=\"2s\" />")

private fun String.escapeXml() =
    replace("\"", "&quot;")
        .replace("&", "&amp;")
        .replace("'", "&apos;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

private fun Long.ticksToDuration() = (this * 100).nanoseconds

private val fadeLength = 50.milliseconds

private fun ByteArray.muteIndiscernible(boundaries: MutableList<Boundary>, format: AudioFormat) = apply {
    for (boundary in boundaries) {
        if (boundary.text == "INDISCERNIBLE") {
            val startIndex = frameIndex(boundary.offset, format)
            val endIndex = frameIndex(boundary.endOffset, format)

            for (i in startIndex..endIndex step 2) {
                shortToLittleEndian(i, 0)
            }

            val startFadeIndex = frameIndex(boundary.offset - fadeLength, format)
            val fadeOutLength = startIndex - startFadeIndex
            for (i in max(startFadeIndex, 0) until startIndex step 2) {
                val muted = littleEndianToShort(i) * (startIndex.toLong() - i) / fadeOutLength
                shortToLittleEndian(i, muted.toShort())
            }

            val endFadeIndex = frameIndex(boundary.endOffset + fadeLength, format)
            val fadeInLength = endFadeIndex - endIndex
            for (i in endIndex..min(endFadeIndex, lastIndex - 1) step 2) {
                val muted = littleEndianToShort(i) * (i - endIndex.toLong()) / fadeInLength
                shortToLittleEndian(i, muted.toShort())
            }
        }
    }
}

private fun frameIndex(duration: Duration, format: AudioFormat) =
    format.frameSize * (duration.inWholeMilliseconds * format.sampleRate / 1000).toInt()

fun ByteArray.littleEndianToShort(index: Int): Short {
    var value: Int = get(index).toInt() and 255
    value = value or get(index + 1).toInt().and(255).shl(8)
    return value.toShort()
}

fun ByteArray.shortToLittleEndian(index: Int, value: Short) {
    set(index, value.toByte())
    set(index + 1, (value.toInt() ushr 8).toByte())
}

fun getTimings(speeches: List<SpeechPart>, ssml: String, boundaries: List<Boundary>): List<Timing> {
    var index = 0

    return speeches.flatMap { speech ->
        val speaker = speech.speakerName
        val sentences = sentenceRegex.findAll(speech.text).map { it.groups[1]!!.value }

        sentences.map { sentence ->
            val xmlSentence = sentence.fixTexts()
            index = ssml.indexOf(xmlSentence, index + 1)

            val start = boundaries.first { it.textOffset == index }.offset
            val end = boundaries.first { it.textOffset == index + xmlSentence.length - 1 }.offset
            Timing(speaker, sentence, start, end)
        }.toList()
    }
}

data class Timing(
    val speaker: String,
    val text: String,
    val start: Duration,
    val end: Duration,
)

private val notSeparators = listOf("Mr", "Mrs", "Ms", "Dr").joinToString("") { "(?<!$it)" }
val sentenceRegex = Regex("\\s*(.+?$notSeparators(?<separator>\\p{Po}|--))(?> |\$)")