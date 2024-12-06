package cz.marvincz.transcript.tts.client

import cz.marvincz.transcript.tts.getSubtitles
import cz.marvincz.transcript.tts.model.Boundary
import cz.marvincz.transcript.tts.model.SpeechPart
import cz.marvincz.transcript.tts.subtitlesHeader
import cz.marvincz.transcript.tts.timing.TextBasedTimingGenerator
import cz.marvincz.transcript.tts.timing.Timing
import cz.marvincz.transcript.tts.timing.VoiceBasedTimingGenerator
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration

@OptIn(ExperimentalSerializationApi::class)
class TimingTest {
    @Test
    fun testGetTimings() {
        // T105:29-32
        val speeches = loadResourceAndDeserialize<List<SpeechPart>>("client/testSpeeches.json")

        val boundaries = loadResourceAndDeserialize<List<Boundary>>("client/testBoundaries.json")

        val timings = TextBasedTimingGenerator().getTimings(speeches, toSSML(speeches), boundaries)

        val expected = loadResourceAsString("client/testExpected.vtt")
        assertEquals(expected, getSubtitles(timings))
    }

    @Test
    fun testTiming2() {
        // T106:4-10
        val speeches = loadResourceAndDeserialize<List<SpeechPart>>("client/test2Speeches.json")
        val ssml = toSSML(speeches)

        val boundaries = loadResourceAndDeserialize<List<Boundary>>("client/test2Boundaries.json")

        val timings = TextBasedTimingGenerator().getTimings(speeches, ssml, boundaries)

        val expected = loadResourceAsString("client/test2Expected.vtt")
        assertEquals(expected, getSubtitles(timings))
    }

    @Test
    fun textOpening() {
        // T100:3-41,T101,T102:1-37
        val speeches = loadResourceAndDeserialize<List<SpeechPart>>("client/openingSpeeches.json")
        val ssml = toSSML(speeches)

        val boundaries = loadResourceAndDeserialize<List<Boundary>>("client/openingBoundaries.json")

        val timings = VoiceBasedTimingGenerator().getTimings(speeches, ssml, boundaries)

        val expected = loadResourceAsString("client/openingExpected.vtt")
        assertEquals(expected, getSubtitles(timings))
    }

    @Test
    fun testIndiscernible() {
        // T106:12-29
        val speeches = loadResourceAndDeserialize<List<SpeechPart>>("client/indiscernibleSpeeches.json")
        val ssml = toSSML(speeches)

        val boundaries = loadResourceAndDeserialize<List<Boundary>>("client/indiscernibleBoundaries.json")

        val timings = VoiceBasedTimingGenerator().getTimings(speeches, ssml, boundaries)

        val expected = loadResourceAsString("client/indiscernibleExpected.vtt")
        assertEquals(expected, getSubtitles(timings))
    }

    /**
     * Tests both a "(NO AUDIBLE RESPONSE)" line, and two consecutive identical one-word lines. ("Content.")
     */
    @Test
    fun testNoAudibleResponse() {
        // T69:7-22
        val speeches = loadResourceAndDeserialize<List<SpeechPart>>("client/noAudibleSpeeches.json")
        val ssml = toSSML(speeches)

        val boundaries =
            Json.decodeFromStream<List<Boundary>>(javaClass.classLoader.getResourceAsStream("client/noAudibleBoundaries.json")!!)

        val timings = VoiceBasedTimingGenerator().getTimings(speeches, ssml, boundaries)

        val expected = loadResourceAsString("client/noAudibleExpected.vtt")
        assertEquals(expected, getSubtitles(timings))
    }

    private fun getSubtitles(timings: List<Timing>) = "$subtitlesHeader${getSubtitles(timings, Duration.ZERO)}"

    private inline fun <reified T> loadResourceAndDeserialize(name: String) =
        Json.decodeFromStream<T>(javaClass.classLoader.getResourceAsStream(name)!!)

    private fun loadResourceAsString(name: String) =
        javaClass.classLoader.getResourceAsStream(name)!!.reader().use { it.readText() }
}