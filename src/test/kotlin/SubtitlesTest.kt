package cz.marvincz.transcript.tts

import cz.marvincz.transcript.tts.model.SpeakerType
import cz.marvincz.transcript.tts.timing.Timing
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
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

    @Test
    fun testSubtitlesOffset() {
        val subtitles = getSubtitles(timings1, 59.minutes + 58.seconds)

        Assertions.assertEquals(subtitlesOffset, "$subtitlesHeader$subtitles")
    }
}

private val timings1 = listOf(
    Timing("CORPORAL HEROUX", SpeakerType.Witness, "Thank you,", 50.milliseconds, 625.milliseconds),
    Timing("CORPORAL HEROUX", SpeakerType.Witness, "My Lord.", 637.5.milliseconds, 1.35.seconds),
    Timing("CORPORAL HEROUX", SpeakerType.Witness, "I’m just going to start --", 1.7.seconds, 3.225.seconds),
    Timing("CORPORAL HEROUX", SpeakerType.Witness, "open up my laptop here.", 3.2375.seconds, 4.525.seconds),
)
private val timings2 = listOf(
    Timing("THE COURT", SpeakerType.Court, "Yes.", 4.875.seconds, 5.5.seconds),
    Timing("THE COURT", SpeakerType.Court, "You go ahead.", 5.8375.seconds, 6.4625.seconds),
    Timing("THE COURT", SpeakerType.Court, "While he’s doing that,", 6.8.seconds, 7.825.seconds),
    Timing("THE COURT", SpeakerType.Court, "I’ll just say that we’re going to sit until about 11:15.", 8.2375.seconds, 11.4875.seconds),
    Timing("THE COURT", SpeakerType.Court, "Around 11:15,", 11.825.seconds, 13.25.seconds),
    Timing("THE COURT", SpeakerType.Court, "we’ll take a 15-minute break.", 13.6125.seconds, 15.2625.seconds),
    Timing("THE COURT", SpeakerType.Court, "So just so you know how long you’re going to have to be sitting there.", 15.6.seconds, 18.999.seconds),
)

private val subtitles = """
[Script Info]
ScriptType: v4.00+
ScaledBorderAndShadow: yes
YCbCr Matrix: None
PlayResX: 1920
PlayResY: 1080
LayoutResX: 1920
LayoutResY: 1080
WrapStyle: 0

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Narrator,DejaVu Sans,96,&H00FFFFFF,&H00FFFFFF,&H00000000,&H00808080,0,0,0,0,100,100,0,0,1,3.0,0,5,200,200,10,1
Style: TextCourt,DejaVu Sans,64,&H00FFFFFF,&H00FFFFFF,&H00000000,&H00808080,0,0,0,0,100,100,0,0,1,2.0,0,5,350,350,10,1
Style: SpeakerCourt,DejaVu Sans,40,&H40FFFFFF,&H00FFFFFF,&H00000000,&H00808080,-1,0,0,0,100,100,0,0,1,1.5,0,8,350,350,300,1
Style: TextCrown,DejaVu Sans,64,&H00FFD8B3,&H00FFFFFF,&H00000000,&H00808080,0,0,0,0,100,100,0,0,1,2.0,0,5,350,350,10,1
Style: SpeakerCrown,DejaVu Sans,40,&H40FFD8B3,&H00FFFFFF,&H00000000,&H00808080,-1,0,0,0,100,100,0,0,1,1.5,0,8,350,350,300,1
Style: TextDefense,DejaVu Sans,64,&H00D8FFB3,&H00FFFFFF,&H00000000,&H00808080,0,0,0,0,100,100,0,0,1,2.0,0,5,350,350,10,1
Style: SpeakerDefense,DejaVu Sans,40,&H40D8FFB3,&H00FFFFFF,&H00000000,&H00808080,-1,0,0,0,100,100,0,0,1,1.5,0,8,350,350,300,1
Style: TextWitness,DejaVu Sans,64,&H00AAFFFF,&H00FFFFFF,&H00000000,&H00808080,0,0,0,0,100,100,0,0,1,2.0,0,5,350,350,10,1
Style: SpeakerWitness,DejaVu Sans,40,&H40AAFFFF,&H00FFFFFF,&H00000000,&H00808080,-1,0,0,0,100,100,0,0,1,1.5,0,8,350,350,300,1
Style: TextOther,DejaVu Sans,64,&H00CBC0FF,&H00FFFFFF,&H00000000,&H00808080,0,0,0,0,100,100,0,0,1,2.0,0,5,350,350,10,1
Style: SpeakerOther,DejaVu Sans,40,&H40CBC0FF,&H00FFFFFF,&H00000000,&H00808080,-1,0,0,0,100,100,0,0,1,1.5,0,8,350,350,300,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
Dialogue: 0,00:00:00.05,00:00:00.63,SpeakerWitness,CORPORAL HEROUX,0,0,0,,CORPORAL HEROUX
Dialogue: 0,00:00:00.05,00:00:00.63,TextWitness,CORPORAL HEROUX,0,0,0,,Thank you,
Dialogue: 0,00:00:00.64,00:00:01.35,SpeakerWitness,CORPORAL HEROUX,0,0,0,,CORPORAL HEROUX
Dialogue: 0,00:00:00.64,00:00:01.35,TextWitness,CORPORAL HEROUX,0,0,0,,My Lord.
Dialogue: 0,00:00:01.70,00:00:03.23,SpeakerWitness,CORPORAL HEROUX,0,0,0,,CORPORAL HEROUX
Dialogue: 0,00:00:01.70,00:00:03.23,TextWitness,CORPORAL HEROUX,0,0,0,,I’m just going to start --
Dialogue: 0,00:00:03.24,00:00:04.53,SpeakerWitness,CORPORAL HEROUX,0,0,0,,CORPORAL HEROUX
Dialogue: 0,00:00:03.24,00:00:04.53,TextWitness,CORPORAL HEROUX,0,0,0,,open up my laptop here.
Dialogue: 0,00:00:04.88,00:00:05.50,SpeakerCourt,THE COURT,0,0,0,,THE COURT
Dialogue: 0,00:00:04.88,00:00:05.50,TextCourt,THE COURT,0,0,0,,Yes.
Dialogue: 0,00:00:05.84,00:00:06.46,SpeakerCourt,THE COURT,0,0,0,,THE COURT
Dialogue: 0,00:00:05.84,00:00:06.46,TextCourt,THE COURT,0,0,0,,You go ahead.
Dialogue: 0,00:00:06.80,00:00:07.83,SpeakerCourt,THE COURT,0,0,0,,THE COURT
Dialogue: 0,00:00:06.80,00:00:07.83,TextCourt,THE COURT,0,0,0,,While he’s doing that,
Dialogue: 0,00:00:08.24,00:00:11.49,SpeakerCourt,THE COURT,0,0,0,,THE COURT
Dialogue: 0,00:00:08.24,00:00:11.49,TextCourt,THE COURT,0,0,0,,I’ll just say that we’re going to sit until about 11:15.
Dialogue: 0,00:00:11.83,00:00:13.25,SpeakerCourt,THE COURT,0,0,0,,THE COURT
Dialogue: 0,00:00:11.83,00:00:13.25,TextCourt,THE COURT,0,0,0,,Around 11:15,
Dialogue: 0,00:00:13.61,00:00:15.26,SpeakerCourt,THE COURT,0,0,0,,THE COURT
Dialogue: 0,00:00:13.61,00:00:15.26,TextCourt,THE COURT,0,0,0,,we’ll take a 15-minute break.
Dialogue: 0,00:00:15.60,00:00:18.99,SpeakerCourt,THE COURT,0,0,0,,THE COURT
Dialogue: 0,00:00:15.60,00:00:18.99,TextCourt,THE COURT,0,0,0,,So just so you know how long you’re going to have to be sitting there.
""".trimIndent()

private val subtitlesOffset = """
[Script Info]
ScriptType: v4.00+
ScaledBorderAndShadow: yes
YCbCr Matrix: None
PlayResX: 1920
PlayResY: 1080
LayoutResX: 1920
LayoutResY: 1080
WrapStyle: 0

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Narrator,DejaVu Sans,96,&H00FFFFFF,&H00FFFFFF,&H00000000,&H00808080,0,0,0,0,100,100,0,0,1,3.0,0,5,200,200,10,1
Style: TextCourt,DejaVu Sans,64,&H00FFFFFF,&H00FFFFFF,&H00000000,&H00808080,0,0,0,0,100,100,0,0,1,2.0,0,5,350,350,10,1
Style: SpeakerCourt,DejaVu Sans,40,&H40FFFFFF,&H00FFFFFF,&H00000000,&H00808080,-1,0,0,0,100,100,0,0,1,1.5,0,8,350,350,300,1
Style: TextCrown,DejaVu Sans,64,&H00FFD8B3,&H00FFFFFF,&H00000000,&H00808080,0,0,0,0,100,100,0,0,1,2.0,0,5,350,350,10,1
Style: SpeakerCrown,DejaVu Sans,40,&H40FFD8B3,&H00FFFFFF,&H00000000,&H00808080,-1,0,0,0,100,100,0,0,1,1.5,0,8,350,350,300,1
Style: TextDefense,DejaVu Sans,64,&H00D8FFB3,&H00FFFFFF,&H00000000,&H00808080,0,0,0,0,100,100,0,0,1,2.0,0,5,350,350,10,1
Style: SpeakerDefense,DejaVu Sans,40,&H40D8FFB3,&H00FFFFFF,&H00000000,&H00808080,-1,0,0,0,100,100,0,0,1,1.5,0,8,350,350,300,1
Style: TextWitness,DejaVu Sans,64,&H00AAFFFF,&H00FFFFFF,&H00000000,&H00808080,0,0,0,0,100,100,0,0,1,2.0,0,5,350,350,10,1
Style: SpeakerWitness,DejaVu Sans,40,&H40AAFFFF,&H00FFFFFF,&H00000000,&H00808080,-1,0,0,0,100,100,0,0,1,1.5,0,8,350,350,300,1
Style: TextOther,DejaVu Sans,64,&H00CBC0FF,&H00FFFFFF,&H00000000,&H00808080,0,0,0,0,100,100,0,0,1,2.0,0,5,350,350,10,1
Style: SpeakerOther,DejaVu Sans,40,&H40CBC0FF,&H00FFFFFF,&H00000000,&H00808080,-1,0,0,0,100,100,0,0,1,1.5,0,8,350,350,300,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
Dialogue: 0,00:59:58.05,00:59:58.63,SpeakerWitness,CORPORAL HEROUX,0,0,0,,CORPORAL HEROUX
Dialogue: 0,00:59:58.05,00:59:58.63,TextWitness,CORPORAL HEROUX,0,0,0,,Thank you,
Dialogue: 0,00:59:58.64,00:59:59.35,SpeakerWitness,CORPORAL HEROUX,0,0,0,,CORPORAL HEROUX
Dialogue: 0,00:59:58.64,00:59:59.35,TextWitness,CORPORAL HEROUX,0,0,0,,My Lord.
Dialogue: 0,00:59:59.70,01:00:01.23,SpeakerWitness,CORPORAL HEROUX,0,0,0,,CORPORAL HEROUX
Dialogue: 0,00:59:59.70,01:00:01.23,TextWitness,CORPORAL HEROUX,0,0,0,,I’m just going to start --
Dialogue: 0,01:00:01.24,01:00:02.53,SpeakerWitness,CORPORAL HEROUX,0,0,0,,CORPORAL HEROUX
Dialogue: 0,01:00:01.24,01:00:02.53,TextWitness,CORPORAL HEROUX,0,0,0,,open up my laptop here.
""".trimIndent()