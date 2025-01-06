package cz.marvincz.transcript.tts

import cz.marvincz.transcript.tts.model.Line
import cz.marvincz.transcript.tts.model.LineType
import cz.marvincz.transcript.tts.model.joinTexts
import kotlin.test.Test
import org.junit.jupiter.api.Assertions

class MainTest {
    @Test
    fun testJoinTexts() {
        val lines = transcriptLines.joinTexts()
        Assertions.assertArrayEquals(expectedJoined.toTypedArray(), lines.toTypedArray())
    }
}

private val transcriptLines = listOf(
    Line(type = LineType.SPEECH, speaker = "MR. SPENCER", text = "Yeah. So -- so you didn’t know with any degree of certainty which direction", page = "T168", line = 32),
    Line(type = LineType.SPEECH, speaker = "MR. SPENCER", text = "the bullet or the projectile even went through the vehicle?", page = "T168", line = 33),
    Line(type = LineType.SPEECH, speaker = "CORPORAL HEROUX", text = "Well, and that’s -- I did have opportunity to speak with Major Crimes, and I", page = "T168", line = 34),
    Line(type = LineType.SPEECH, speaker = "CORPORAL HEROUX", text = "know that they had witness statements putting -- putting the accused at the", page = "T168", line = 35),
    Line(type = LineType.SPEECH, speaker = "CORPORAL HEROUX", text = "driver’s side window shortly after a gunshot was heard. So the driver’s side", page = "T168", line = 36),
    Line(type = LineType.SPEECH, speaker = "CORPORAL HEROUX", text = "was certainly a possibility.",page = "T168", line = 37),
    Line(type = LineType.PARAGRAPH, page = "T168", line = 38),
    Line(type = LineType.SPEECH, speaker = "MR. SPENCER", text = "Okay. And were you aware there was a witness saying the exact opposite?", page = "T168", line = 39),
    Line(type = LineType.SPEECH, speaker = "CORPORAL HEROUX", text = "No.", page = "T168", line = 4)
)

private val expectedJoined = listOf(
    Line(type = LineType.SPEECH, speaker = "MR. SPENCER", text = "Yeah. So -- so you didn’t know with any degree of certainty which direction the bullet or the projectile even went through the vehicle?", page = "T168", line = 32),
    Line(type = LineType.SPEECH, speaker = "CORPORAL HEROUX", text = "Well, and that’s -- I did have opportunity to speak with Major Crimes, and I know that they had witness statements putting -- putting the accused at the driver’s side window shortly after a gunshot was heard. So the driver’s side was certainly a possibility.", page = "T168", line = 34),
    Line(type = LineType.PARAGRAPH, page = "T168", line = 38),
    Line(type = LineType.SPEECH, speaker = "MR. SPENCER", text = "Okay. And were you aware there was a witness saying the exact opposite?", page = "T168", line = 39),
    Line(type = LineType.SPEECH, speaker = "CORPORAL HEROUX", text = "No.", page = "T168", line = 4)
)