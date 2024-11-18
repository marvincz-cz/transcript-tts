package cz.marvincz.transcript.tts.client

import com.microsoft.cognitiveservices.speech.SpeechSynthesisBoundaryType
import cz.marvincz.transcript.tts.joinTexts
import cz.marvincz.transcript.tts.model.*
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

        assertTiming("Thank you, My Lord. I’m just going to start --", 50.milliseconds, 3.225.seconds, timings[0]) // 625.milliseconds - 637.5.milliseconds, 1.45.seconds - 1.7.seconds
        assertTiming("open up my laptop here.", 3.2375.seconds, 4.625.seconds, timings[1])
        assertTiming("Yes. You go ahead.", 4.875.seconds, 6.55.seconds, timings[2]) // 5.5875.seconds - 5.8375.seconds
        assertTiming("While he’s doing that,", 6.8.seconds, 8.2375.seconds, timings[3])
        assertTiming("I’ll just say that we’re going to sit until about 11:15.", 8.2375.seconds, 11.575.seconds, timings[4])
        assertTiming("Around 11:15, we’ll take a 15-minute break.", 11.825.seconds, 15.35.seconds, timings[5]) // 13.6125.seconds - 13.6125.seconds
        assertTiming("So just so you know how long you’re going to have to be sitting there.", 15.6.seconds, 18.375.seconds, timings[6])
    }

    private fun assertTiming(expectedText: String, expectedStart: Duration, expectedEnd: Duration, timing: Timing) {
        assertEquals(expectedText, timing.text)
        assertEquals(expectedStart, timing.start)
        assertEquals(expectedEnd, timing.end)
    }

    @Test
    fun textOpening() {
        val lines = opening.joinTexts().filter { it.text != null }

        val sentences = lines.flatMap { getSentences(SpeechPart(AzureSpeaker("en-US-AdamMultilingualNeural"), it.speaker!!, it.text!!)) }

        println(sentences.joinToString("\n"))
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

private val opening = listOf(
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "Thank you.",page = "T100",line = 12),
    Line(type = LineType.PARAGRAPH,page = "T100",line = 13),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "Ladies and gentlemen, as His Lordship told you yesterday, my name is Bill Burge.",page = "T100",line = 14),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "This is Chris Browne. We have been assigned to prosecute this matter. My friends,",page = "T100",line = 15),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "Scott Spencer and Dustin Gillander [sic], are representing Mr. Stanley.",page = "T100",line = 16),
    Line(type = LineType.PARAGRAPH,page = "T100",line = 17),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "And, ladies and gentlemen, we do thank you for being here today. You have an",page = "T100",line = 18),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "important duty to undertake, and -- and, of course, today presented some trying",page = "T100",line = 19),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "conditions, I’m sure, for many of you, and -- and we thank you very much and --",page = "T100",line = 20),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "and we all appreciate that you’re here to do what you -- what is a very important",page = "T100",line = 21),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "civic duty.",page = "T100",line = 22),
    Line(type = LineType.PARAGRAPH,page = "T100",line = 23),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "Now, ladies and gentlemen, this is my opening statement, and the purpose of this",page = "T100",line = 24),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "statement is so that I can outline to you the evidence that the Crown will be",page = "T100",line = 25),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "presenting for you so that you can make your proper determination at the end of",page = "T100",line = 26),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "the case. We will be calling a variety of kinds of evidence. First of all, we’re going",page = "T100",line = 27),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "to start with three police officers who will describe attending to the scene, what",page = "T100",line = 28),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "they saw. They will describe the physical setup at the Stanley residence where this",page = "T100",line = 29),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "incident occurred. They -- you will hear about an examination, a very fine, detailed",page = "T100",line = 30),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "examination, of -- of that property. Colten Boushie was in a vehicle when he --",page = "T100",line = 31),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "when he was shot. You will hear that he died of a gunshot wound. There was a",page = "T100",line = 32),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "careful examination of the vehicle in -- at the residence or at the location, plus at",page = "T100",line = 33),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "the police service bay where they went through this vehicle in very great detail.",page = "T100",line = 34),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "You will also be hearing from a bloodstain pattern analyst who will give you her",page = "T100",line = 35),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "opinion of what she can determine from the scene, and she’ll tell you what she",page = "T100",line = 36),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "can’t determine from the scene.",page = "T100",line = 37),
    Line(type = LineType.PARAGRAPH,page = "T100",line = 38),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "We will then be calling -- those are the witnesses that I expect to be calling today.",page = "T100",line = 39),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "Hopefully we’ll get through them all today. Tomorrow we will be calling some",page = "T100",line = 40),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "civilian evidence. And just so you know, we have sent subpoenas -- we have",page = "T100",line = 41),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "served subpoenas on everyone who can be compelled to come to court with a",page = "T101",line = 1),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "subpoena who was there and who has something to say about what happened.",page = "T101",line = 2),
    Line(type = LineType.PARAGRAPH,page = "T101",line = 3),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "And just -- just so you know, there may well be some serious contradictions",page = "T101",line = 4),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "between what people saw. You will be in a very good position to determine what",page = "T101",line = 5),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "you feel the facts are, what has been proved to you. His Lordship gave you some",page = "T101",line = 6),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "preliminary instructions yesterday, and at the end of the trial, he will, I am sure,",page = "T101",line = 7),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "give you further instructions as to how to deal with -- with evidence and how you",page = "T101",line = 8),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "may come to a unanimous conclusion.",page = "T101",line = 9),
    Line(type = LineType.PARAGRAPH,page = "T101",line = 10),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "Ladies and gentlemen, you will hear that Gerald Stanley resided in the Biggar",page = "T101",line = 11),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "area. He had a farm there where he raised cattle. He also did, as I understand,",page = "T101",line = 12),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "mechanical work for people in the area. He would fix vehicles. They would bring",page = "T101",line = 13),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "them to him, leave them in his yard, and he would work on those vehicles.",page = "T101",line = 14),
    Line(type = LineType.PARAGRAPH,page = "T101",line = 15),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "The first witness from the civilian side that you will hear from is Sheldon Stanley,",page = "T101",line = 16),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "and he is Gerald Stanley’s son. He will tell you that he was at his parents’ home --",page = "T101",line = 17),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "he doesn’t live there anymore, but he was at his parents’ home on the 9th of",page = "T101",line = 18),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "August of 2016, that he and his father were working on a fence. They were",page = "T101",line = 19),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "building a gate for a fence when they heard a rather loud vehicle come into their",page = "T101",line = 20),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "yard, and that he saw this vehicle stop by a truck that had been left to be repaired.",page = "T101",line = 21),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "And someone got out of the vehicle and went into the truck, which he just assumed",page = "T101",line = 22),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "was maybe the owner dropping off a part because that’s -- that seemed to be a",page = "T101",line = 23),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "typical thing that happened when -- when Mr. Stanley repaired people’s vehicles.",page = "T101",line = 24),
    Line(type = LineType.PARAGRAPH,page = "T101",line = 25),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "But then this vehicle went further into the -- into the yard, towards the shop, and",page = "T101",line = 26),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "someone got out and started -- tried to start a quad that was in the Stanleys’ yard. I",page = "T101",line = 27),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "believe it was the Stanley -- belonged to the Stanleys. This caused a reaction.",page = "T101",line = 28),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "Sheldon Stanley will tell you that he ran towards where the quad was. His father",page = "T101",line = 29),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "came with him. There were several people in this car that had entered the yard.",page = "T101",line = 30),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "They were -- he was yelling at them, that the -- the people who were out of the --",page = "T101",line = 31),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "out of this vehicle got back in. The vehicle backed up. Sheldon Stanley will tell",page = "T101",line = 32),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "you that he -- as he was building a fence, he had a tool belt on, and he had his",page = "T101",line = 33),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "hammer with him. He struck the windshield of this car with a hammer.",page = "T101",line = 34),
    Line(type = LineType.PARAGRAPH,page = "T101",line = 35),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "The car then headed towards the driveway that would lead to the grid road. As the",page = "T101",line = 36),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "car was going in that direction, it collided with another vehicle in their yard.",page = "T101",line = 37),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "Sheldon Stanley says he was running to his house to get his car keys, thinking he",page = "T101",line = 38),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "would be following this vehicle. As he was running into the house, he heard two",page = "T101",line = 39),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "gunshots.",page = "T101",line = 40),
    Line(type = LineType.PARAGRAPH,page = "T101",line = 41),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "When he came out of the house with his -- with his car keys, he heard another",page = "T102",line = 1),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "gunshot. He looked. He saw his father standing by the driver’s door of this vehicle",page = "T102",line = 2),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "with a gun and a clip in his hand. That when Sheldon Stanley approached the",page = "T102",line = 3),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "vehicle, he saw Colten Boushie in the driver’s seat, slumped towards the right with",page = "T102",line = 4),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "the steering wheel of the vehicle. There were two females in the back seat. Two",page = "T102",line = 5),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "other males in the vehicle had got out and ran away after the vehicle collided with",page = "T102",line = 6),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "the other vehicle.",page = "T102",line = 7),
    Line(type = LineType.PARAGRAPH,page = "T102",line = 8),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "Ladies and gentlemen, this case, it might be a shorter case than a lot of murder",page = "T102",line = 9),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "trials. It’s partly because there’s -- it’s based on eyewitnesses. In addition to that,",page = "T102",line = 10),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "ladies and gentlemen, my friend has been very -- my friend, Mr. Spencer, has been",page = "T102",line = 11),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "very professional in his handling of this matter. He has agreed to admit many facts",page = "T102",line = 12),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "that would be easy for the Crown to prove, but it would take -- it takes time. It",page = "T102",line = 13),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "becomes cumbersome. And so we have been able to just get down to what is",page = "T102",line = 14),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "important in this case.",page = "T102",line = 15),
    Line(type = LineType.PARAGRAPH,page = "T102",line = 16),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "And by agreeing to certain facts, we have been able to eliminate a lot of witnesses.",page = "T102",line = 17),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "That includes -- we will be filing reports of experts. There is a report from Mr. --",page = "T102",line = 18),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "Dr. Claude Dalpe. He’s a trace evidence expert, and his report will indicate that --",page = "T102",line = 19),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "Gerald Stanley was arrested after this incident, ladies and gentlemen. He was",page = "T102",line = 20),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "examined by the police for what -- they use what’s called a gunshot residue kit",page = "T102",line = 21),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "where they check his hands and his face to see if he was in proximity to a firearm.",page = "T102",line = 22),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "And -- and there were positive results, which means that either Mr. Stanley fired a",page = "T102",line = 23),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "firearm, he was in close proximity to a firearm when it was fired, or he handled a",page = "T102",line = 24),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "firearm that had been fired. And that will be in a report from Dr. Dalpe that will be",page = "T102",line = 25),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "filed as evidence by consent.",page = "T102",line = 26),
    Line(type = LineType.PARAGRAPH,page = "T102",line = 27),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "We have reports from Gillian Sayers. She’s a specialist at the -- with the RCMP",page = "T102",line = 28),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "forensic lab. So was Dr. Dalpe with the forensic lab of the RCMP. Gillian Sayers",page = "T102",line = 29),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "reports -- she -- she’s a DNA specialist, and she has examined certain items,",page = "T102",line = 30),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "including swabs of blood from the driver’s seat of this vehicle and from the floor",page = "T102",line = 31),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "of the driver’s compartment of the vehicle, and she’s determined to her satisfaction",page = "T102",line = 32),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "that this is the blood of Colten Boushie. You’ll see her -- her reports will be in",page = "T102",line = 33),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "evidence, and -- and the number she uses is that -- she says it’s his blood, and the",page = "T102",line = 34),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "chance of this being someone else’s blood is one in a quintillion, which is --",page = "T102",line = 35),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "wasn’t a number that I’m familiar with, but it’s a one followed by 18 zeros. So",page = "T102",line = 36),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "she’s fairly confident as to whose blood this was.",page = "T102",line = 37),
    Line(type = LineType.PARAGRAPH,page = "T102",line = 38),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "The police, when they did their examination of the scene, found a number of",page = "T102",line = 39),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "expended cartridges from a firearm. There were three in particular that looked to",page = "T102",line = 40),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "be very fresh. Two of them were in the middle of the yard. You may conclude it",page = "T102",line = 41),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "was from where Gerald Stanley was when the first two shots were fired. The third",page = "T103",line = 1),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "was found inside of the vehicle where Colten Boushie was, and it was found on the",page = "T103",line = 2),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "dash in the -- in front of the passenger’s seat of the vehicle. And in the Gerald",page = "T103",line = 3),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "Stanley residence, the police located a handgun that had -- that was -- it was a",page = "T103",line = 4),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "pistol that -- that had a magazine, which is what contains the cartridges for the",page = "T103",line = 5),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "ammunition for the gun. That was found in the house. The -- a firearms expert has",page = "T103",line = 6),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "determined that the expended cartridges that were found were fired in that",page = "T103",line = 7),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "handgun from the house. The gun is called a Tokarev. It’s a Russian-made gun.",page = "T103",line = 8),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "When the DNA experts looked at this gun, they found Colten Boushie’s DNA on",page = "T103",line = 9),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "the trigger guard and trigger and hammer area of the gun. And again, they -- they",page = "T103",line = 10),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "were satisfied to the same, about one in a quintillion, that this was Colten",page = "T103",line = 11),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "Boushie’s DNA on -- on this gun.",page = "T103",line = 12),
    Line(type = LineType.PARAGRAPH,page = "T103",line = 13),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "Colten Boushie, his body was -- there was an autopsy performed on the body. And",page = "T103",line = 14),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "my friends have agreed that instead of calling the pathologist, who’s a highly-",page = "T103",line = 15),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "trained doctor who specializes in -- in examining bodies to determine cause of",page = "T103",line = 16),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "death, instead of calling that doctor, we -- they have agreed that we will simply",page = "T103",line = 17),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "tender his report in evidence. And that report will indicate that Colten Boushie, the",page = "T103",line = 18),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "only significant injury on him -- there was only one significant injury on his body,",page = "T103",line = 19),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "and that was a gunshot wound to the head that entered just behind the left ear and",page = "T103",line = 20),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "exited on -- through the right -- his right neck, and that that was the cause of death",page = "T103",line = 21),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "of Colten Boushie.",page = "T103",line = 22),
    Line(type = LineType.PARAGRAPH,page = "T103",line = 23),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "Ladies and gentlemen, what I expect will likely be the last witness that we call is a",page = "T103",line = 24),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "firearms expert. He is -- his name is Greg Williams, and he examined the -- the",page = "T103",line = 25),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "Tokarev handgun. He examined the cartridges and concluded that they were fired",page = "T103",line = 26),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "-- that these cartridges were fired in the Tokarev, and that was part of our agreed",page = "T103",line = 27),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "statement of fact.",page = "T103",line = 28),
    Line(type = LineType.PARAGRAPH,page = "T103",line = 29),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "In addition, he will testify that -- that he did find -- the expended cartridge that was",page = "T103",line = 30),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "found inside the vehicle, there was -- it was -- it had an unusual bulge, and he can’t",page = "T103",line = 31),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "tell you exactly why it had a bulge. He’ll offer some possibilities, and you might",page = "T103",line = 32),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "find his evidence to be very helpful to you. You might find that it is too",page = "T103",line = 33),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "speculative to be of much use as to what caused the bulge. But it is very important",page = "T103",line = 34),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "evidence, ladies and gentlemen, and when you -- when he is called, you -- I urge",page = "T103",line = 35),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "you to pay very close attention to what he says.",page = "T103",line = 36),
    Line(type = LineType.PARAGRAPH,page = "T103",line = 37),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "And, ladies and gentlemen, that is a summary of the evidence that we will be",page = "T103",line = 38),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "presenting in an effort to have you hold Mr. Stanley accountable for this death.",page = "T103",line = 39),
    Line(type = LineType.PARAGRAPH,page = "T103",line = 40),
    Line(type = LineType.SPEECH,speaker = "MR. BURGE",text = "So, My Lord, those are my opening statements.",page = "T103",line = 41)
)