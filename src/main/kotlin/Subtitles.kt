package cz.marvincz.transcript.tts

import cz.marvincz.transcript.tts.model.SpeakerType
import cz.marvincz.transcript.tts.timing.Timing
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration


val subtitlesHeader = """
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
""".trimIndent()

fun getSubtitles(timings: List<Timing>, offset: Duration) = timings.joinToString("") {
    val start = (it.start + offset).format()
    val end = (it.end + offset).format()

    if (it.speakerType == SpeakerType.Narrator) "\nDialogue: 0,$start,$end,Narrator,${it.speaker},0,0,0,,${it.text}"
    else "\nDialogue: 0,$start,$end,Speaker${it.speakerType},${it.speaker},0,0,0,,${it.speaker}" +
            "\nDialogue: 0,$start,$end,Text${it.speakerType},${it.speaker},0,0,0,,${it.text}"
}

private fun Duration.format() = toComponents { hours, minutes, seconds, nanoseconds ->
    val fractionSeconds = min((nanoseconds / 10_000_000.0).roundToInt(), 99)
    buildString {
        append(hours.toString().padStart(2, '0'))
        append(":")
        append(minutes.toString().padStart(2, '0'))
        append(":")
        append(seconds.toString().padStart(2, '0'))
        append(".")
        append(fractionSeconds.toString().padStart(2, '0'))
    }
}