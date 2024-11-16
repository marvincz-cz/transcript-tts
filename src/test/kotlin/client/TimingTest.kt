package cz.marvincz.transcript.tts.client

import com.microsoft.cognitiveservices.speech.SpeechSynthesisBoundaryType
import cz.marvincz.transcript.tts.model.AzureSpeaker
import cz.marvincz.transcript.tts.model.Boundary
import cz.marvincz.transcript.tts.model.SpeechPart
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TimingTest {
    @Test
    fun testGetTimings() {
        // T105:29-32
        val speeches = listOf(
            SpeechPart(
                AzureSpeaker("en-US-NovaTurboMultilingualNeural"),
                "SERGEANT BARNES",
                "If I may, I’d like to refer to my notes from time to time just to keep things in -- " +
                        "in line and perspective? I’ve also prepared a PowerPoint presentation, along " +
                        "with these booklets, that are the same photographs as in the PowerPoint. And " +
                        "I’d like to use these to illustrate the scene and what I documented on that day."
            )
        )

        val timings = getTimings(speeches, toSSML(speeches), boundaries)

        assertTiming("If I may,", 50.milliseconds, 400.milliseconds, timings[0])
        assertTiming("I’d like to refer to my notes from time to time just to keep things in --", 400.milliseconds, 3250.milliseconds, timings[1])
        assertTiming("in line and perspective?", 3250.milliseconds, 4300.milliseconds, timings[2])
        assertTiming("I’ve also prepared a PowerPoint presentation,", 4300.milliseconds, 6300.milliseconds, timings[3])
        assertTiming("along with these booklets,", 6300.milliseconds, 7450.milliseconds, timings[4])
        assertTiming("that are the same photographs as in the PowerPoint.", 7450.milliseconds, 9600.milliseconds, timings[5])
        assertTiming("And I’d like to use these to illustrate the scene and what I documented on that day.", 9600.milliseconds, 13000.milliseconds, timings[6])
    }
    
    @Test
    fun testTiming2() {
        // T106:4-10
        val speeches = listOf(
            SpeechPart(AzureSpeaker("en-US-DustinMultilingualNeural"), "CORPORAL HEROUX", "Thank you, My Lord. I’m just going to start -- open up my laptop here."),
            SpeechPart(AzureSpeaker("en-US-LewisMultilingualNeural"), "THE COURT", "Yes. You go ahead."),
            SpeechPart(AzureSpeaker("en-US-LewisMultilingualNeural"), "THE COURT", "While he’s doing that, I’ll just say that we’re going to sit until about 11:15. Around 11:15, we’ll take a 15-minute break. So just so you know how long you’re going to have to be sitting there."),
        )
        val ssml = toSSML(speeches)

        val timings = getTimings(speeches, ssml, boundaries2)

        assertTiming("Thank you,", 50.milliseconds, 625.milliseconds, timings[0])
        assertTiming("My Lord.", 637.5.milliseconds, 1.45.seconds, timings[1])
        assertTiming("I’m just going to start --", 1.7.seconds, 3.225.seconds, timings[2])
        assertTiming("open up my laptop here.", 3.2375.seconds, 4.625.seconds, timings[3])
        assertTiming("Yes.", 4.875.seconds, 5.5875.seconds, timings[4])
        assertTiming("You go ahead.", 5.8375.seconds, 6.55.seconds, timings[5])
        assertTiming("While he’s doing that,", 6.8.seconds, 8.2375.seconds, timings[6])
        assertTiming("I’ll just say that we’re going to sit until about 11:15.", 8.2375.seconds, 11.575.seconds, timings[7])
        assertTiming("Around 11:15,", 11.825.seconds, 13.6125.seconds, timings[8])
        assertTiming("we’ll take a 15-minute break.", 13.6125.seconds, 15.35.seconds, timings[9])
        assertTiming("So just so you know how long you’re going to have to be sitting there.", 15.6.seconds, 18.375.seconds, timings[10])
    }

    private fun assertTiming(expectedText: String, expectedStart: Duration, expectedEnd: Duration, timing: Timing) {
        assertEquals(expectedText, timing.text)
        assertEquals(expectedStart, timing.start)
        assertEquals(expectedEnd, timing.end)
    }
}

private val boundaries = listOf(
    Boundary(SpeechSynthesisBoundaryType.Word, "If", 50.milliseconds, 100.milliseconds, 175),
    Boundary(SpeechSynthesisBoundaryType.Word, "I", 150.milliseconds, 50.milliseconds, 178),
    Boundary(SpeechSynthesisBoundaryType.Word, "may", 200.milliseconds, 150.milliseconds, 180),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, ",", 350.milliseconds, 50.milliseconds, 183),
    Boundary(SpeechSynthesisBoundaryType.Word, "I’d", 400.milliseconds, 150.milliseconds, 185),
    Boundary(SpeechSynthesisBoundaryType.Word, "like", 550.milliseconds, 200.milliseconds, 189),
    Boundary(SpeechSynthesisBoundaryType.Word, "to", 750.milliseconds, 100.milliseconds, 194),
    Boundary(SpeechSynthesisBoundaryType.Word, "refer", 850.milliseconds, 250.milliseconds, 197),
    Boundary(SpeechSynthesisBoundaryType.Word, "to", 1100.milliseconds, 100.milliseconds, 203),
    Boundary(SpeechSynthesisBoundaryType.Word, "my", 1200.milliseconds, 100.milliseconds, 206),
    Boundary(SpeechSynthesisBoundaryType.Word, "notes", 1300.milliseconds, 250.milliseconds, 209),
    Boundary(SpeechSynthesisBoundaryType.Word, "from", 1550.milliseconds, 200.milliseconds, 215),
    Boundary(SpeechSynthesisBoundaryType.Word, "time", 1750.milliseconds, 200.milliseconds, 220),
    Boundary(SpeechSynthesisBoundaryType.Word, "to", 1950.milliseconds, 100.milliseconds, 225),
    Boundary(SpeechSynthesisBoundaryType.Word, "time", 2050.milliseconds, 200.milliseconds, 228),
    Boundary(SpeechSynthesisBoundaryType.Word, "just", 2250.milliseconds, 200.milliseconds, 233),
    Boundary(SpeechSynthesisBoundaryType.Word, "to", 2450.milliseconds, 100.milliseconds, 238),
    Boundary(SpeechSynthesisBoundaryType.Word, "keep", 2550.milliseconds, 200.milliseconds, 241),
    Boundary(SpeechSynthesisBoundaryType.Word, "things", 2750.milliseconds, 300.milliseconds, 246),
    Boundary(SpeechSynthesisBoundaryType.Word, "in", 3050.milliseconds, 100.milliseconds, 248),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, "-", 3150.milliseconds, 50.milliseconds, 256),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, "-", 3200.milliseconds, 50.milliseconds, 257),
    Boundary(SpeechSynthesisBoundaryType.Word, "in", 3250.milliseconds, 100.milliseconds, 259),
    Boundary(SpeechSynthesisBoundaryType.Word, "line", 3350.milliseconds, 200.milliseconds, 262),
    Boundary(SpeechSynthesisBoundaryType.Word, "and", 3550.milliseconds, 150.milliseconds, 267),
    Boundary(SpeechSynthesisBoundaryType.Word, "perspective", 3700.milliseconds, 550.milliseconds, 271),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, "?", 4250.milliseconds, 50.milliseconds, 282),
    Boundary(SpeechSynthesisBoundaryType.Word, "I’ve", 4300.milliseconds, 200.milliseconds, 284),
    Boundary(SpeechSynthesisBoundaryType.Word, "also", 4500.milliseconds, 200.milliseconds, 289),
    Boundary(SpeechSynthesisBoundaryType.Word, "prepared", 4700.milliseconds, 400.milliseconds, 294),
    Boundary(SpeechSynthesisBoundaryType.Word, "a", 5100.milliseconds, 50.milliseconds, 298),
    Boundary(SpeechSynthesisBoundaryType.Word, "PowerPoint", 5150.milliseconds, 500.milliseconds, 305),
    Boundary(SpeechSynthesisBoundaryType.Word, "presentation", 5650.milliseconds, 600.milliseconds, 316),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, ",", 6250.milliseconds, 50.milliseconds, 328),
    Boundary(SpeechSynthesisBoundaryType.Word, "along", 6300.milliseconds, 250.milliseconds, 330),
    Boundary(SpeechSynthesisBoundaryType.Word, "with", 6550.milliseconds, 200.milliseconds, 336),
    Boundary(SpeechSynthesisBoundaryType.Word, "these", 6750.milliseconds, 250.milliseconds, 341),
    Boundary(SpeechSynthesisBoundaryType.Word, "booklets", 7000.milliseconds, 400.milliseconds, 347),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, ",", 7400.milliseconds, 50.milliseconds, 355),
    Boundary(SpeechSynthesisBoundaryType.Word, "that", 7450.milliseconds, 200.milliseconds, 357),
    Boundary(SpeechSynthesisBoundaryType.Word, "are", 7650.milliseconds, 150.milliseconds, 362),
    Boundary(SpeechSynthesisBoundaryType.Word, "the", 7800.milliseconds, 150.milliseconds, 366),
    Boundary(SpeechSynthesisBoundaryType.Word, "same", 7950.milliseconds, 200.milliseconds, 370),
    Boundary(SpeechSynthesisBoundaryType.Word, "photographs", 8150.milliseconds, 550.milliseconds, 375),
    Boundary(SpeechSynthesisBoundaryType.Word, "as", 8700.milliseconds, 100.milliseconds, 387),
    Boundary(SpeechSynthesisBoundaryType.Word, "in", 8800.milliseconds, 100.milliseconds, 390),
    Boundary(SpeechSynthesisBoundaryType.Word, "the", 8900.milliseconds, 150.milliseconds, 393),
    Boundary(SpeechSynthesisBoundaryType.Word, "PowerPoint", 9050.milliseconds, 500.milliseconds, 397),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, ".", 9550.milliseconds, 50.milliseconds, 407),
    Boundary(SpeechSynthesisBoundaryType.Word, "And", 9600.milliseconds, 150.milliseconds, 409),
    Boundary(SpeechSynthesisBoundaryType.Word, "I’d", 9750.milliseconds, 150.milliseconds, 413),
    Boundary(SpeechSynthesisBoundaryType.Word, "like", 9900.milliseconds, 200.milliseconds, 417),
    Boundary(SpeechSynthesisBoundaryType.Word, "to", 10100.milliseconds, 100.milliseconds, 422),
    Boundary(SpeechSynthesisBoundaryType.Word, "use", 10200.milliseconds, 150.milliseconds, 425),
    Boundary(SpeechSynthesisBoundaryType.Word, "these", 10350.milliseconds, 250.milliseconds, 429),
    Boundary(SpeechSynthesisBoundaryType.Word, "to", 10600.milliseconds, 100.milliseconds, 435),
    Boundary(SpeechSynthesisBoundaryType.Word, "illustrate", 10700.milliseconds, 500.milliseconds, 438),
    Boundary(SpeechSynthesisBoundaryType.Word, "the", 11200.milliseconds, 150.milliseconds, 449),
    Boundary(SpeechSynthesisBoundaryType.Word, "scene", 11350.milliseconds, 250.milliseconds, 453),
    Boundary(SpeechSynthesisBoundaryType.Word, "and", 11600.milliseconds, 150.milliseconds, 459),
    Boundary(SpeechSynthesisBoundaryType.Word, "what", 11750.milliseconds, 200.milliseconds, 463),
    Boundary(SpeechSynthesisBoundaryType.Word, "I", 11950.milliseconds, 50.milliseconds, 468),
    Boundary(SpeechSynthesisBoundaryType.Word, "documented", 12000.milliseconds, 500.milliseconds, 470),
    Boundary(SpeechSynthesisBoundaryType.Word, "on", 12500.milliseconds, 100.milliseconds, 481),
    Boundary(SpeechSynthesisBoundaryType.Word, "that", 12600.milliseconds, 200.milliseconds, 484),
    Boundary(SpeechSynthesisBoundaryType.Word, "day", 12800.milliseconds, 150.milliseconds, 489),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, ".", 12950.milliseconds, 50.milliseconds, 492),
)

val boundaries2 = listOf(
    Boundary(SpeechSynthesisBoundaryType.Word, "Thank", 50.milliseconds, 312.5.milliseconds, 172),
    Boundary(SpeechSynthesisBoundaryType.Word, "you", 375.milliseconds, 237.5.milliseconds, 178),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, ",", 625.milliseconds, 0.seconds, 181),
    Boundary(SpeechSynthesisBoundaryType.Word, "My", 637.5.milliseconds, 150.milliseconds, 183),
    Boundary(SpeechSynthesisBoundaryType.Word, "Lord", 800.milliseconds, 537.5.milliseconds, 186),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, ".", 1.35.seconds, 100.milliseconds, 190),
    Boundary(SpeechSynthesisBoundaryType.Word, "I’m", 1.7.seconds, 262.5.milliseconds, 192),
    Boundary(SpeechSynthesisBoundaryType.Word, "just", 1.975.seconds, 125.milliseconds, 196),
    Boundary(SpeechSynthesisBoundaryType.Word, "going", 2.112500.seconds, 112.5.milliseconds, 201),
    Boundary(SpeechSynthesisBoundaryType.Word, "to", 2.237500.seconds, 37.5.milliseconds, 207),
    Boundary(SpeechSynthesisBoundaryType.Word, "start", 2.287500.seconds, 400.milliseconds, 210),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, "-", 2.787500.seconds, 437.5.milliseconds, 216),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, "-", 3.225.seconds, 0.seconds, 217),
    Boundary(SpeechSynthesisBoundaryType.Word, "open", 3.237500.seconds, 212.5.milliseconds, 219),
    Boundary(SpeechSynthesisBoundaryType.Word, "up", 3.462500.seconds, 87.5.milliseconds, 224),
    Boundary(SpeechSynthesisBoundaryType.Word, "my", 3.562500.seconds, 125.milliseconds, 227),
    Boundary(SpeechSynthesisBoundaryType.Word, "laptop", 3.7.seconds, 487.5.milliseconds, 230),
    Boundary(SpeechSynthesisBoundaryType.Word, "here", 4.2.seconds, 312.5.milliseconds, 237),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, ".", 4.525.seconds, 100.milliseconds, 241),
    Boundary(SpeechSynthesisBoundaryType.Word, "Yes", 4.875.seconds, 625.milliseconds, 294),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, ".", 5.5.seconds, 87.5.milliseconds, 297),
    Boundary(SpeechSynthesisBoundaryType.Word, "You", 5.837500.seconds, 200.milliseconds, 299),
    Boundary(SpeechSynthesisBoundaryType.Word, "go", 6.037500.seconds, 137.5.milliseconds, 303),
    Boundary(SpeechSynthesisBoundaryType.Word, "ahead", 6.175.seconds, 287.5.milliseconds, 306),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, ".", 6.462500.seconds, 87.5.milliseconds, 311),
    Boundary(SpeechSynthesisBoundaryType.Word, "While", 6.8.seconds, 262.5.milliseconds, 364),
    Boundary(SpeechSynthesisBoundaryType.Word, "he’s", 7.062500.seconds, 200.milliseconds, 370),
    Boundary(SpeechSynthesisBoundaryType.Word, "doing", 7.262500.seconds, 250.milliseconds, 375),
    Boundary(SpeechSynthesisBoundaryType.Word, "that", 7.512500.seconds, 225.milliseconds, 381),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, ",", 7.825.seconds, 412.5.milliseconds, 385),
    Boundary(SpeechSynthesisBoundaryType.Word, "I’ll", 8.237500.seconds, 112.5.milliseconds, 387),
    Boundary(SpeechSynthesisBoundaryType.Word, "just", 8.35.seconds, 237.5.milliseconds, 392),
    Boundary(SpeechSynthesisBoundaryType.Word, "say", 8.587500.seconds, 300.milliseconds, 397),
    Boundary(SpeechSynthesisBoundaryType.Word, "that", 8.887500.seconds, 100.milliseconds, 401),
    Boundary(SpeechSynthesisBoundaryType.Word, "we’re", 8.987500.seconds, 125.milliseconds, 406),
    Boundary(SpeechSynthesisBoundaryType.Word, "going", 9.112500.seconds, 187.5.milliseconds, 412),
    Boundary(SpeechSynthesisBoundaryType.Word, "to", 9.3.seconds, 50.milliseconds, 418),
    Boundary(SpeechSynthesisBoundaryType.Word, "sit", 9.35.seconds, 312.5.milliseconds, 421),
    Boundary(SpeechSynthesisBoundaryType.Word, "until", 9.987500.seconds, 300.milliseconds, 425),
    Boundary(SpeechSynthesisBoundaryType.Word, "about", 10.287500.seconds, 287.5.milliseconds, 431),
    Boundary(SpeechSynthesisBoundaryType.Word, "11:15", 10.575.seconds, 912.5.milliseconds, 437),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, ".", 11.487500.seconds, 87.5.milliseconds, 442),
    Boundary(SpeechSynthesisBoundaryType.Word, "Around", 11.825.seconds, 425.milliseconds, 444),
    Boundary(SpeechSynthesisBoundaryType.Word, "11:15", 12.25.seconds, 912.5.milliseconds, 451),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, ",", 13.25.seconds, 362.5.milliseconds, 456),
    Boundary(SpeechSynthesisBoundaryType.Word, "we’ll", 13.612500.seconds, 137.5.milliseconds, 458),
    Boundary(SpeechSynthesisBoundaryType.Word, "take", 13.75.seconds, 200.milliseconds, 464),
    Boundary(SpeechSynthesisBoundaryType.Word, "a", 13.95.seconds, 62.5.milliseconds, 469),
    Boundary(SpeechSynthesisBoundaryType.Word, "15-minute", 14.012500.seconds, 837.5.milliseconds, 471),
    Boundary(SpeechSynthesisBoundaryType.Word, "break", 14.85.seconds, 412.5.milliseconds, 481),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, ".", 15.262500.seconds, 87.5.milliseconds, 486),
    Boundary(SpeechSynthesisBoundaryType.Word, "So", 15.6.seconds, 237.5.milliseconds, 488),
    Boundary(SpeechSynthesisBoundaryType.Word, "just", 15.837500.seconds, 225.milliseconds, 491),
    Boundary(SpeechSynthesisBoundaryType.Word, "so", 16.062500.seconds, 175.milliseconds, 496),
    Boundary(SpeechSynthesisBoundaryType.Word, "you", 16.237500.seconds, 137.5.milliseconds, 499),
    Boundary(SpeechSynthesisBoundaryType.Word, "know", 16.375.seconds, 187.5.milliseconds, 503),
    Boundary(SpeechSynthesisBoundaryType.Word, "how", 16.562500.seconds, 175.milliseconds, 508),
    Boundary(SpeechSynthesisBoundaryType.Word, "long", 16.737500.seconds, 362.5.milliseconds, 512),
    Boundary(SpeechSynthesisBoundaryType.Word, "you’re", 17.1.seconds, 100.milliseconds, 517),
    Boundary(SpeechSynthesisBoundaryType.Word, "going", 17.2.seconds, 150.milliseconds, 524),
    Boundary(SpeechSynthesisBoundaryType.Word, "to", 17.35.seconds, 50.milliseconds, 530),
    Boundary(SpeechSynthesisBoundaryType.Word, "have", 17.4.seconds, 212.5.milliseconds, 533),
    Boundary(SpeechSynthesisBoundaryType.Word, "to", 17.612500.seconds, 50.milliseconds, 538),
    Boundary(SpeechSynthesisBoundaryType.Word, "be", 17.662500.seconds, 100.milliseconds, 541),
    Boundary(SpeechSynthesisBoundaryType.Word, "sitting", 17.762500.seconds, 275.milliseconds, 544),
    Boundary(SpeechSynthesisBoundaryType.Word, "there", 18.037500.seconds, 250.milliseconds, 552),
    Boundary(SpeechSynthesisBoundaryType.Punctuation, ".", 18.287500.seconds, 87.5.milliseconds, 557),
)