package cz.marvincz.transcript.tts.client

import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import com.microsoft.cognitiveservices.speech.VoiceInfo
import cz.marvincz.transcript.tts.model.AzureSpeaker
import cz.marvincz.transcript.tts.model.Boundary
import cz.marvincz.transcript.tts.model.Expression
import cz.marvincz.transcript.tts.model.SpeechPart
import cz.marvincz.transcript.tts.model.ticksToDuration
import cz.marvincz.transcript.tts.timing.Timing
import cz.marvincz.transcript.tts.timing.VoiceBasedTimingGenerator
import cz.marvincz.transcript.tts.utils.muteSection
import cz.marvincz.transcript.tts.utils.replaceAllIndexed
import java.io.ByteArrayInputStream
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class Client(subscriptionKey: String, region: String) {

    private val speechConfig = SpeechConfig.fromSubscription(subscriptionKey, region).apply {
        // PCM_SIGNED 48000.0 Hz, 16 bit, mono, 2 bytes/frame, little-endian
        setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Riff48Khz16BitMonoPcm)
    }

    private val timingGenerator = VoiceBasedTimingGenerator()

    /**
     * Synthesize speech from the [speeches]. Callback [onProgress] receives updates with values between 0 and 1 for 0%-100% progress.
     */
    fun synthesize(speeches: List<SpeechPart>, mute: Boolean, onProgress: (Float) -> Unit): TtsResult {
        val ssml = toSSML(speeches)
        val textRange = ssml.indexOf("<lang").let { ssml.indexOf(">", it)} until ssml.lastIndexOf("</lang")

        val speechSynthesizer = SpeechSynthesizer(speechConfig, null)

        val boundaries = mutableListOf<Boundary>()
        speechSynthesizer.WordBoundary.addEventListener { _, e ->
            boundaries.add(Boundary(e))
            onProgress((e.textOffset - textRange.first) / (textRange.last - textRange.first).toFloat())
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
        boundaries.replaceAllIndexed { index, boundary ->
            if (index != boundaries.lastIndex) boundary.withPauseTo(boundaries[index + 1])
            else boundary
        }

        val mutedSections = mutableListOf<AudioSection>()
        val mutedAudio = result.audioData.muteIndiscernible(boundaries, format, mutedSections, mute)

        return TtsResult(
            audioData = mutedAudio,
            timings = timingGenerator.getTimings(speeches, ssml, boundaries),
            duration = correctDuration,
            mutedSections = mutedSections,
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
        val mutedSections: List<AudioSection>
    )

    data class AudioSection(
        val start: Duration,
        val end: Duration,
        val duration: Duration = end - start,
    ) {
        constructor(boundary: Boundary) : this(boundary.offset, boundary.endOffset)
    }
}