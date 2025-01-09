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

    @Test
    fun testJoinNumbers() {
        val lines = numberLines.joinTexts()
        Assertions.assertEquals(numbersJoined, lines.single().text)
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

private val numberLines = listOf(
    Line(type = LineType.SPEECH, speaker = "THE COURT CLERK", text = "Number 45. Number 45? Number 53. 5-",page = "T19", line = 18),
    Line(type = LineType.SPEECH, speaker = "THE COURT CLERK", text = "3? Number 58. Number 58? Number 61. 61? Number 77. Number 77? Number 80.",page = "T19", line = 19),
    Line(type = LineType.SPEECH, speaker = "THE COURT CLERK", text = "Number 80? Number 99. Number 99? Number 117. 1-1-7? Number 143. 1-4-3?",page = "T19", line = 20),
    Line(type = LineType.SPEECH, speaker = "THE COURT CLERK", text = "Number 149. 1-4-9? Number 2-0-8. 2-0-8? Number 245. 2-4-5? Number 287. 2-8-",page = "T19", line = 21),
    Line(type = LineType.SPEECH, speaker = "THE COURT CLERK", text = "7? Number 3-0-6. 3-0-6? 318. Number 318? Number 425. 4-2-5? Number 474. 4-",page = "T19", line = 22),
    Line(type = LineType.SPEECH, speaker = "THE COURT CLERK", text = "7-4? Number 475. 4-7-5? Number 499. 4-9-9? Number 550. 5-5-0? 572. 5-7-2?",page = "T19", line = 23),
    Line(type = LineType.SPEECH, speaker = "THE COURT CLERK", text = "Number 618. 6-1-8? Number 621. 6-2-1?",page = "T19", line = 24),
)

private val numbersJoined =
    "Number 45. Number 45? Number 53. 5-3? Number 58. Number 58? Number 61. 61? Number 77. Number 77? Number 80. Number 80? Number 99. Number 99? Number 117. 1-1-7? Number 143. 1-4-3? Number 149. 1-4-9? Number 2-0-8. 2-0-8? Number 245. 2-4-5? Number 287. 2-8-7? Number 3-0-6. 3-0-6? 318. Number 318? Number 425. 4-2-5? Number 474. 4-7-4? Number 475. 4-7-5? Number 499. 4-9-9? Number 550. 5-5-0? 572. 5-7-2? Number 618. 6-1-8? Number 621. 6-2-1?"