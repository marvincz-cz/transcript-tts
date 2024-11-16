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
import cz.marvincz.transcript.tts.utils.muteSection
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

    private fun ByteArray.muteIndiscernible(boundaries: List<Boundary>, format: AudioFormat) = apply {
        for (boundary in boundaries) {
            if (boundary.text == "INDISCERNIBLE") muteSection(format, boundary.offset, boundary.endOffset)
        }
    }

    class TtsResult(
        val audioData: ByteArray,
        val boundaries: MutableList<Boundary>,
        val timings: List<Timing>,
        val duration: Duration,
    )
}