package cz.marvincz.transcript.tts.client

import com.microsoft.cognitiveservices.speech.SpeechSynthesisBoundaryType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class SentenceSplitTest {
    @Test
    fun testGetTimings() {
        val speeches = listOf(SpeechPart(AzureSpeaker("en-US-DustinMultilingualNeural"), "", speech))

        val timings = getTimings(speeches, toSSML(speeches), boundaries)
            .map { it.start to it.end }

        assertEquals(expectedTimings, timings)
    }
}

// T105-29
private const val speech = "If I may, I’d like to refer to my notes from time to time just to keep things in -- " +
        "in line and perspective? I’ve also prepared a PowerPoint presentation, along " +
        "with these booklets, that are the same photographs as in the PowerPoint. And " +
        "I’d like to use these to illustrate the scene and what I documented on that day."

private val boundaries = listOf(
    boundary(SpeechSynthesisBoundaryType.Word, "If", 50, 100, 172),
    boundary(SpeechSynthesisBoundaryType.Word, "I", 150, 50, 175),
    boundary(SpeechSynthesisBoundaryType.Word, "may", 200, 150, 177),
    boundary(SpeechSynthesisBoundaryType.Punctuation, ",", 350, 50, 180),
    boundary(SpeechSynthesisBoundaryType.Word, "I’d", 400, 150, 182),
    boundary(SpeechSynthesisBoundaryType.Word, "like", 550, 200, 186),
    boundary(SpeechSynthesisBoundaryType.Word, "to", 750, 100, 191),
    boundary(SpeechSynthesisBoundaryType.Word, "refer", 850, 250, 194),
    boundary(SpeechSynthesisBoundaryType.Word, "to", 1100, 100, 200),
    boundary(SpeechSynthesisBoundaryType.Word, "my", 1200, 100, 203),
    boundary(SpeechSynthesisBoundaryType.Word, "notes", 1300, 250, 206),
    boundary(SpeechSynthesisBoundaryType.Word, "from", 1550, 200, 212),
    boundary(SpeechSynthesisBoundaryType.Word, "time", 1750, 200, 217),
    boundary(SpeechSynthesisBoundaryType.Word, "to", 1950, 100, 222),
    boundary(SpeechSynthesisBoundaryType.Word, "time", 2050, 200, 225),
    boundary(SpeechSynthesisBoundaryType.Word, "just", 2250, 200, 230),
    boundary(SpeechSynthesisBoundaryType.Word, "to", 2450, 100, 235),
    boundary(SpeechSynthesisBoundaryType.Word, "keep", 2550, 200, 238),
    boundary(SpeechSynthesisBoundaryType.Word, "things", 2750, 300, 243),
    boundary(SpeechSynthesisBoundaryType.Word, "in", 3050, 100, 245),
    boundary(SpeechSynthesisBoundaryType.Punctuation, "-", 3150, 50, 253),
    boundary(SpeechSynthesisBoundaryType.Punctuation, "-", 3200, 50, 254),
    boundary(SpeechSynthesisBoundaryType.Word, "in", 3250, 100, 256),
    boundary(SpeechSynthesisBoundaryType.Word, "line", 3350, 200, 259),
    boundary(SpeechSynthesisBoundaryType.Word, "and", 3550, 150, 264),
    boundary(SpeechSynthesisBoundaryType.Word, "perspective", 3700, 550, 268),
    boundary(SpeechSynthesisBoundaryType.Punctuation, "?", 4250, 50, 279),
    boundary(SpeechSynthesisBoundaryType.Word, "I’ve", 4300, 200, 281),
    boundary(SpeechSynthesisBoundaryType.Word, "also", 4500, 200, 286),
    boundary(SpeechSynthesisBoundaryType.Word, "prepared", 4700, 400, 291),
    boundary(SpeechSynthesisBoundaryType.Word, "a", 5100, 50, 295),
    boundary(SpeechSynthesisBoundaryType.Word, "PowerPoint", 5150, 500, 302),
    boundary(SpeechSynthesisBoundaryType.Word, "presentation", 5650, 600, 313),
    boundary(SpeechSynthesisBoundaryType.Punctuation, ",", 6250, 50, 325),
    boundary(SpeechSynthesisBoundaryType.Word, "along", 6300, 250, 327),
    boundary(SpeechSynthesisBoundaryType.Word, "with", 6550, 200, 333),
    boundary(SpeechSynthesisBoundaryType.Word, "these", 6750, 250, 338),
    boundary(SpeechSynthesisBoundaryType.Word, "booklets", 7000, 400, 344),
    boundary(SpeechSynthesisBoundaryType.Punctuation, ",", 7400, 50, 352),
    boundary(SpeechSynthesisBoundaryType.Word, "that", 7450, 200, 354),
    boundary(SpeechSynthesisBoundaryType.Word, "are", 7650, 150, 359),
    boundary(SpeechSynthesisBoundaryType.Word, "the", 7800, 150, 363),
    boundary(SpeechSynthesisBoundaryType.Word, "same", 7950, 200, 367),
    boundary(SpeechSynthesisBoundaryType.Word, "photographs", 8150, 550, 372),
    boundary(SpeechSynthesisBoundaryType.Word, "as", 8700, 100, 384),
    boundary(SpeechSynthesisBoundaryType.Word, "in", 8800, 100, 387),
    boundary(SpeechSynthesisBoundaryType.Word, "the", 8900, 150, 390),
    boundary(SpeechSynthesisBoundaryType.Word, "PowerPoint", 9050, 500, 394),
    boundary(SpeechSynthesisBoundaryType.Punctuation, ".", 9550, 50, 404),
    boundary(SpeechSynthesisBoundaryType.Word, "And", 9600, 150, 406),
    boundary(SpeechSynthesisBoundaryType.Word, "I’d", 9750, 150, 410),
    boundary(SpeechSynthesisBoundaryType.Word, "like", 9900, 200, 414),
    boundary(SpeechSynthesisBoundaryType.Word, "to", 10100, 100, 419),
    boundary(SpeechSynthesisBoundaryType.Word, "use", 10200, 150, 422),
    boundary(SpeechSynthesisBoundaryType.Word, "these", 10350, 250, 426),
    boundary(SpeechSynthesisBoundaryType.Word, "to", 10600, 100, 432),
    boundary(SpeechSynthesisBoundaryType.Word, "illustrate", 10700, 500, 435),
    boundary(SpeechSynthesisBoundaryType.Word, "the", 11200, 150, 446),
    boundary(SpeechSynthesisBoundaryType.Word, "scene", 11350, 250, 450),
    boundary(SpeechSynthesisBoundaryType.Word, "and", 11600, 150, 456),
    boundary(SpeechSynthesisBoundaryType.Word, "what", 11750, 200, 460),
    boundary(SpeechSynthesisBoundaryType.Word, "I", 11950, 50, 465),
    boundary(SpeechSynthesisBoundaryType.Word, "documented", 12000, 500, 467),
    boundary(SpeechSynthesisBoundaryType.Word, "on", 12500, 100, 478),
    boundary(SpeechSynthesisBoundaryType.Word, "that", 12600, 200, 481),
    boundary(SpeechSynthesisBoundaryType.Word, "day", 12800, 150, 486),
    boundary(SpeechSynthesisBoundaryType.Punctuation, ".", 12950, 50, 489),
)

private val expectedTimings = listOf(
    50.milliseconds to 350.milliseconds,
    400.milliseconds to 3200.milliseconds,
    3250.milliseconds to 4250.milliseconds,
    4300.milliseconds to 6250.milliseconds,
    6300.milliseconds to 7400.milliseconds,
    7450.milliseconds to 9550.milliseconds,
    9600.milliseconds to 12950.milliseconds,
)

private fun boundary(
    type: SpeechSynthesisBoundaryType,
    text: String,
    offset: Int,
    duration: Int,
    textOffset: Int,
) = Boundary(type, text, offset.milliseconds, duration.milliseconds, textOffset)
