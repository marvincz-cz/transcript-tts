package cz.marvincz.transcript.tts

import cz.marvincz.transcript.tts.timing.Timing
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SubtitlesTest {
    @Test
    fun testSubtitles() {
        val subtitles1 = getSubtitles(timings1, 0.seconds)
        val subtitles2 = getSubtitles(timings2, 0.seconds)

        Assertions.assertEquals(subtitles, "$subtitlesHeader$subtitles1$subtitles2")
    }
}

private val timings1 = listOf(
    Timing("CORPORAL HEROUX", "Thank you,", 50.milliseconds, 625.milliseconds),
    Timing("CORPORAL HEROUX", "My Lord.", 637.5.milliseconds, 1.35.seconds),
    Timing("CORPORAL HEROUX", "I’m just going to start --", 1.7.seconds, 3.225.seconds),
    Timing("CORPORAL HEROUX", "open up my laptop here.", 3.2375.seconds, 4.525.seconds),
)
private val timings2 = listOf(
    Timing("THE COURT", "Yes.", 4.875.seconds, 5.5.seconds),
    Timing("THE COURT", "You go ahead.", 5.8375.seconds, 6.4625.seconds),
    Timing("THE COURT", "While he’s doing that,", 6.8.seconds, 7.825.seconds),
    Timing("THE COURT", "I’ll just say that we’re going to sit until about 11:15.", 8.2375.seconds, 11.4875.seconds),
    Timing("THE COURT", "Around 11:15,", 11.825.seconds, 13.25.seconds),
    Timing("THE COURT", "we’ll take a 15-minute break.", 13.6125.seconds, 15.2625.seconds),
    Timing("THE COURT", "So just so you know how long you’re going to have to be sitting there.", 15.6.seconds, 18.2875.seconds),
)

private val subtitles = """
WEBVTT

00:00.050 --> 00:00.625
<v CORPORAL HEROUX>Thank you,

00:00.637 --> 00:01.350
<v CORPORAL HEROUX>My Lord.

00:01.700 --> 00:03.225
<v CORPORAL HEROUX>I’m just going to start --

00:03.237 --> 00:04.525
<v CORPORAL HEROUX>open up my laptop here.

00:04.875 --> 00:05.500
<v THE COURT>Yes.

00:05.837 --> 00:06.462
<v THE COURT>You go ahead.

00:06.800 --> 00:07.825
<v THE COURT>While he’s doing that,

00:08.237 --> 00:11.487
<v THE COURT>I’ll just say that we’re going to sit until about 11:15.

00:11.825 --> 00:13.250
<v THE COURT>Around 11:15,

00:13.612 --> 00:15.262
<v THE COURT>we’ll take a 15-minute break.

00:15.600 --> 00:18.287
<v THE COURT>So just so you know how long you’re going to have to be sitting there.
""".trimIndent()