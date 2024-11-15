package cz.marvincz.transcript.tts

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.installMordantMarkdown
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import cz.marvincz.transcript.tts.client.*
import cz.marvincz.transcript.tts.model.Line
import cz.marvincz.transcript.tts.model.Transcript
import java.io.File
import java.io.SequenceInputStream
import java.util.Collections.enumeration
import java.util.Properties
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.serialization.json.Json

fun main(args: Array<String>) = Application().main(args)

private class Application : CliktCommand() {
    init {
        installMordantMarkdown()
    }

    private val transcript: File by option().file(mustExist = true, canBeDir = false, mustBeReadable = true).required()
        .help { "The transcript JSON file" }
    private val output: File by option().file(canBeDir = false).required()
        .help { "The output file where the generated audio will be written" }
    private val azureProperties: File by option().file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .required()
        .help { "The Azure API properties file. Must contain keys `subscription_key` and `region`" }
    private val sections: List<Section>? by option().convert { parseSections(it, name) }
        .help {
            """
                *(optional)* The section of the transcript to use. When not specified, the whole transcript is used.
                Format is a comma-separated list of pages, optionally with a colon and lines either as a single number or a range with two numbers separated by a hyphen.
                
                Example: `T100:3-41,T101,T102,T103`
            """.trimIndent()
        }

    override fun run() {
        val json = transcript.readText()
        val transcript = Json.decodeFromString<Transcript>(json)

        val lines = transcript.lines
            .joinTexts()
            .filter { line -> sections?.any { line.page == it.page && line.line in it.lines } != false }
            .filter { it.text != null }

        if (lines.isEmpty()) {
            println("No text found")
            return
        }

        // TODO: Voices as a parameter
        val jurorVoices = listOf(
            AzureSpeaker("en-US-AmandaMultilingualNeural"),
            AzureSpeaker("zh-CN-XiaoyuMultilingualNeural"),
            AzureSpeaker("it-IT-GiuseppeMultilingualNeural"),
            AzureSpeaker("pt-BR-MacerioMultilingualNeural"),
            AzureSpeaker("zh-CN-XiaochenMultilingualNeural"),
            AzureSpeaker("en-US-SteffanMultilingualNeural"),
        )
        val jurors = transcript.speakers.filter { it.startsWith("JUROR") }

        val voices = mapOf(
            "THE COURT" to AzureSpeaker("en-US-LewisMultilingualNeural"),
            "THE COURT CLERK" to AzureSpeaker(
                "en-US-SerenaMultilingualNeural",
                expression = Expression(style = "serious")
            ),
            "MR. BURGE" to AzureSpeaker("en-US-AdamMultilingualNeural"),
            "MR. SPENCER" to AzureSpeaker("en-US-DerekMultilingualNeural"),
            "CORPORAL HEROUX" to AzureSpeaker("en-US-DustinMultilingualNeural"),
            "SERGEANT BARNES" to AzureSpeaker("en-US-NovaTurboMultilingualNeural"),
            "MR. BROWNE" to AzureSpeaker("en-US-RyanMultilingualNeural"),
            "NARRATOR" to AzureSpeaker("en-US-AlloyTurboMultilingualNeural"),
            "THE ACCUSED" to AzureSpeaker("en-US-DavisMultilingualNeural"),
            "MR. GILLANDERS" to AzureSpeaker("en-US-SamuelMultilingualNeural"),
            "THE SHERIFF" to AzureSpeaker("en-US-DavisMultilingualNeural"),
            "MR. BRUCE" to AzureSpeaker("en-US-DavisMultilingualNeural"),
            "UNIDENTIFIED SPEAKER" to AzureSpeaker("en-US-ChristopherMultilingualNeural"),
        ) + jurors.mapIndexed { index, juror -> juror to jurorVoices[index % jurorVoices.size] }

        val missingVoices = lines.mapTo(mutableSetOf(), Line::speakerOrNarrator).filter { it !in voices }
        require(missingVoices.isEmpty()) { "Missing TTS voices for speakers: $missingVoices" }

        val properties = Properties().apply { load(azureProperties.inputStream()) }

        val client = Client(
            subscriptionKey = properties.getProperty("subscription_key"),
            region = properties.getProperty("region"),
        )

        val subtitleFile = File(output.path.replaceAfterLast('.', "vtt"))
        subtitleFile.writeText("WEBVTT")

        val chunks = lines.map { line ->
            SpeechPart(voices.getValue(line.speakerOrNarrator), line.speakerOrNarrator, requireNotNull(line.text))
        }.splitToChunks()
        var chunkOffset = 0.milliseconds

        try {
            val outputs = chunks.mapIndexed { index, chunk ->
                println("Generating chunk ${index + 1}")
                val result = client.call(chunk)

                val chunkOutput = output.chunkTempFile(index)
                chunkOutput.writeBytes(result.audioData)
                subtitleFile.appendText(getSubtitles(result.timings, chunkOffset))

                chunkOffset += result.duration
                chunkOutput
            }

            val stream = outputs.map { AudioSystem.getAudioInputStream(it) }
                .let { streams ->
                    AudioInputStream(
                        SequenceInputStream(enumeration(streams)),
                        streams.first().format,
                        streams.sumOf { it.frameLength }
                    )
                }
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, output)
        } finally {
            chunks.indices.forEach { runCatching { output.chunkTempFile(it).delete() } }
        }
    }

    private data class Section(val page: String, val lines: IntRange)

    private fun parseSections(value: String, optionName: String) = value.split(',').map { section ->
        val matchGroups =
            requireNotNull(sectionRegex.matchEntire(section)) { "$optionName does not match the format" }.groups

        val page = matchGroups["page"]!!.value
        val lineFrom = matchGroups["lineFrom"]?.value?.toInt()
        val lineTo = matchGroups["lineTo"]?.value?.toInt()

        Section(
            page = page,
            lines = lineFrom?.let { IntRange(it, lineTo ?: lineFrom) } ?: IntRange(-Int.MIN_VALUE, Int.MAX_VALUE)
        )
    }
}

private fun File.chunkTempFile(index: Int) =
    File(buildString {
        append(path.substringBeforeLast('.'))
        append("temp")
        append(index + 1)
        append('.')
        append(path.substringAfterLast('.'))
    })

private fun List<SpeechPart>.splitToChunks(): List<List<SpeechPart>> {
    var chunk = mutableListOf<SpeechPart>()
    val chunks = mutableListOf(chunk)
    var textLength = 0

    forEach { speech ->
        if (textLength + speech.text.length > CHUNK_TEXT_LENGTH_LIMIT || chunk.size >= CHUNK_SPEECHES_LIMIT) {
            chunk = mutableListOf(speech)
            chunks.add(chunk)
            textLength = speech.text.length
        } else {
            chunk.add(speech)
            textLength += speech.text.length
        }
    }

    return chunks
}

private const val CHUNK_TEXT_LENGTH_LIMIT = 8_000
private const val CHUNK_SPEECHES_LIMIT = 45

private val sectionRegex = Regex("(?<page>T\\d+)(?::(?<lineFrom>\\d+)(?:-(?<lineTo>\\d+))?)?")

fun List<Line>.joinTexts(): List<Line> = runningReduce { acc, line ->
    if (acc.sameSpeaker(line)) line.text?.let { acc.copy(text = "${acc.text} ${line.text}") } ?: acc
    else line
}.filterIndexed { index, line -> index == lastIndex || !line.sameSpeaker(get(index + 1)) }

fun getSubtitles(timings: List<Timing>, offset: Duration) = timings.joinToString("\n\n", prefix = "\n\n") {
    "${(it.start + offset).format()} --> ${(it.end + offset).format()}\n<v ${it.speaker}>${it.text}"
}

private fun Duration.format() = toComponents { minutes, seconds, nanoseconds ->
    val milliseconds = nanoseconds / 1_000_000
    "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.${
        milliseconds.toString().padStart(3, '0')
    }"
}