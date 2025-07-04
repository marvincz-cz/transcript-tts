package cz.marvincz.transcript.tts.client

import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import cz.marvincz.transcript.tts.model.AzureSpeaker
import cz.marvincz.transcript.tts.model.Boundary
import cz.marvincz.transcript.tts.model.Expression
import cz.marvincz.transcript.tts.model.SpeakerInfo
import cz.marvincz.transcript.tts.model.SpeakerType
import cz.marvincz.transcript.tts.model.SpeechPart
import cz.marvincz.transcript.tts.model.ticksToDuration
import cz.marvincz.transcript.tts.timing.Timing
import cz.marvincz.transcript.tts.timing.VoiceBasedTimingGenerator
import cz.marvincz.transcript.tts.utils.duration
import cz.marvincz.transcript.tts.utils.json
import cz.marvincz.transcript.tts.utils.muteSection
import cz.marvincz.transcript.tts.utils.replaceAllIndexed
import java.io.ByteArrayInputStream
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString

class Client(config: AzureConfig) {

    private val speechConfig = SpeechConfig.fromSubscription(config.subscriptionKey, config.region).apply {
        // PCM_SIGNED 48000.0 Hz, 16 bit, mono, 2 bytes/frame, little-endian
        setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Riff48Khz16BitMonoPcm)
    }

    private val timingGenerator = VoiceBasedTimingGenerator()

    /**
     * Synthesize speech from the [speeches]. Callback [onProgress] receives updates with values between 0 and 1 for 0%-100% progress.
     */
    fun synthesize(speeches: List<SpeechPart>, mute: Boolean, boundariesFile: File?, onProgress: (Float) -> Unit): TtsResult {
        val ssml = toSSML(speeches)
        val textRange = ssml.indexOf("<lang").let { ssml.indexOf(">", it) } until ssml.lastIndexOf("</lang")

        val speechSynthesizer = SpeechSynthesizer(speechConfig, null)

        val boundaries = mutableListOf<Boundary>()
        speechSynthesizer.WordBoundary.addEventListener { _, e ->
            boundaries.add(Boundary(e))
            onProgress((e.textOffset - textRange.first) / (textRange.last - textRange.first).toFloat())
        }

        return speechSynthesizer.SpeakSsml(ssml).use { result ->
            if (result.reason != ResultReason.SynthesizingAudioCompleted) {
                println(result.reason)
            }
            val audioInputStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(result.audioData))

            // Azure reports wrong timing information, we try to correct it
            val correctDuration = audioInputStream.duration
            val correctionRatio = correctDuration / result.audioDuration.ticksToDuration()
            boundaries.replaceAll {
                it.copy(offset = it.offset * correctionRatio, duration = it.duration * correctionRatio)
            }
            boundaries.replaceAllIndexed { index, boundary ->
                if (index != boundaries.lastIndex) boundary.withPauseTo(boundaries[index + 1])
                else boundary
            }

            boundariesFile?.writeText(json.encodeToString(boundaries))

            val mutedSections = mutableListOf<AudioSection>()
            val mutedAudio =
                result.audioData.muteIndiscernible(boundaries, audioInputStream.format, mutedSections, mute)

            TtsResult(
                audioData = mutedAudio,
                timings = timingGenerator.getTimings(speeches, ssml, boundaries),
                duration = correctDuration,
                mutedSections = mutedSections,
            )
        }
    }

    fun getAllVoices(language: String): List<Pair<SpeakerInfo, String>> {
        val speechSynthesizer = SpeechSynthesizer(speechConfig, null)

        return speechSynthesizer.voicesAsync.get().use { result ->
            result.voices.filter { "Multilingual" in it.shortName || it.locale == language }
                .flatMap {
                    it.use { voice ->
                        listOf(
                            SpeakerInfo(
                                AzureSpeaker(voice.shortName, null, language),
                                SpeakerType.Other
                            ) to "${voice.localName} (${voice.shortName}) - default"
                        ) + voice.styleList.map { style ->
                            SpeakerInfo(
                                AzureSpeaker(voice.shortName, Expression(style), language),
                                SpeakerType.Other
                            ) to "${voice.localName} (${voice.shortName}) - $style"
                        }
                    }
                }
        }
    }

    suspend fun generateSpeechSample(output: File, speaker: SpeakerInfo, name: String) {
        val timeTaken = measureTime {
            val speechParts = listOf(
                SpeechPart(speaker, name, "Yes, My Lord."),
                SpeechPart(speaker, name, "Okay."),
                SpeechPart(
                    speaker, name, "So the -- P-4 -- what is the document entitled? The case involves " +
                            "an allegation that on or about the 9th day of August, 2016, at or near Biggar, Saskatchewan, " +
                            "Gerald Stanley unlawfully caused the death of Colten Boushie and thereby committed " +
                            "second degree murder."
                ),
            )

            val speechSynthesizer = SpeechSynthesizer(speechConfig, null)

            val ssml = toSSML(speechParts)
            val data = speechSynthesizer.SpeakSsml(ssml).use { it.audioData }

            File(output, "$name.wav").writeBytes(data)
        }

        // throttling, max 20 calls / second
        delay(3.1.seconds - timeTaken)
    }

    fun speakDirect(ssml: String, onProgress: (Float) -> Unit): ByteArray {
        val textRange = ssml.indexOf("<lang").let { ssml.indexOf(">", it) } until ssml.lastIndexOf("</lang")

        val speechSynthesizer = SpeechSynthesizer(speechConfig, null)

        speechSynthesizer.WordBoundary.addEventListener { _, e ->
            onProgress((e.textOffset - textRange.first) / (textRange.last - textRange.first).toFloat())
        }

        val result = speechSynthesizer.SpeakSsml(ssml)

        return result.use { it.audioData }
    }

    private fun ByteArray.muteIndiscernible(
        boundaries: List<Boundary>,
        format: AudioFormat,
        mutedSections: MutableList<AudioSection>,
        mute: Boolean,
    ) = apply {
        for (boundary in boundaries) {
            if (boundary.text in listOf("INDISCERNIBLE", "NO-AUDIBLE-RESPONSE", "UNREPORTABLE-SOUND")) {
                if (mute) {
                    muteSection(format, boundary.offset, boundary.endOffset)
                }
                mutedSections.add(AudioSection(boundary))
            }
        }
    }

    class TtsResult(
        val audioData: ByteArray,
        val timings: List<Timing>,
        val duration: Duration,
        val mutedSections: List<AudioSection>,
    )

    data class AudioSection(
        val start: Duration,
        val end: Duration,
        val duration: Duration = end - start,
    ) {
        constructor(boundary: Boundary) : this(boundary.offset, boundary.endOffset)
    }
}

data class AzureConfig(val subscriptionKey: String, val region: String)