package cz.marvincz.transcript.tts.client

import cz.marvincz.transcript.tts.getSubtitles
import cz.marvincz.transcript.tts.model.Boundary
import cz.marvincz.transcript.tts.model.SpeechPart
import cz.marvincz.transcript.tts.subtitlesHeader
import cz.marvincz.transcript.tts.timing.TextBasedTimingGenerator
import cz.marvincz.transcript.tts.timing.Timing
import cz.marvincz.transcript.tts.timing.TimingGenerator
import cz.marvincz.transcript.tts.timing.VoiceBasedTimingGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

@OptIn(ExperimentalSerializationApi::class)
class TimingTest {
    @Test
    fun testTimings() {
        // T105:29-32
        testTiming(
            timingGenerator = TextBasedTimingGenerator(),
            speechesResource = "client/testSpeeches.json",
            boundariesResource = "client/testBoundaries.json",
            expectedResource = "client/testExpected.vtt",
        )
    }

    @Test
    fun testTiming2() {
        // T106:4-10
        testTiming(
            timingGenerator = TextBasedTimingGenerator(),
            speechesResource = "client/test2Speeches.json",
            boundariesResource = "client/test2Boundaries.json",
            expectedResource = "client/test2Expected.vtt",
        )
    }

    @Test
    fun textOpening() {
        // T100:3-41,T101,T102:1-37
        testTiming(
            timingGenerator = VoiceBasedTimingGenerator(),
            speechesResource = "client/openingSpeeches.json",
            boundariesResource = "client/openingBoundaries.json",
            expectedResource = "client/openingExpected.vtt",
        )
    }

    @Test
    fun testIndiscernible() {
        // T106:12-29
        testTiming(
            timingGenerator = VoiceBasedTimingGenerator(),
            speechesResource = "client/indiscernibleSpeeches.json",
            boundariesResource = "client/indiscernibleBoundaries.json",
            expectedResource = "client/indiscernibleExpected.vtt",
        )
    }

    /**
     * Tests both a "(NO AUDIBLE RESPONSE)" line, and two consecutive identical one-word lines. ("Content.")
     */
    @Test
    fun testNoAudibleResponse() {
        // T69:7-22
        testTiming(
            timingGenerator = VoiceBasedTimingGenerator(),
            speechesResource = "client/noAudibleSpeeches.json",
            boundariesResource = "client/noAudibleBoundaries.json",
            expectedResource = "client/noAudibleExpected.vtt"
        )
    }

    private fun testTiming(
        timingGenerator: TimingGenerator,
        speechesResource: String,
        boundariesResource: String,
        expectedResource: String,
    ) {
        val speeches = loadResourceAndDeserialize<List<SpeechPart>>(speechesResource)
        val ssml = toSSML(speeches)

        val boundaries = loadResourceAndDeserialize<List<Boundary>>(boundariesResource)

        val timings = timingGenerator.getTimings(speeches, ssml, boundaries)

        val expected = loadResourceAsString(expectedResource)
        assertEquals(expected, getSubtitles(timings))
    }

    private fun getSubtitles(timings: List<Timing>) = "$subtitlesHeader${getSubtitles(timings, Duration.ZERO)}"

    private inline fun <reified T> loadResourceAndDeserialize(name: String) =
        Json.decodeFromStream<T>(javaClass.classLoader.getResourceAsStream(name)!!)

    private fun loadResourceAsString(name: String) =
        javaClass.classLoader.getResourceAsStream(name)!!.reader().use { it.readText() }
}