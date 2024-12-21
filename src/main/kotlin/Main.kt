package cz.marvincz.transcript.tts

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.installMordantMarkdown
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.animation.progress.animateOnThread
import com.github.ajalt.mordant.animation.progress.execute
import com.github.ajalt.mordant.animation.progress.update
import com.github.ajalt.mordant.widgets.progress.percentage
import com.github.ajalt.mordant.widgets.progress.progressBar
import com.github.ajalt.mordant.widgets.progress.progressBarLayout
import com.github.ajalt.mordant.widgets.progress.text
import cz.marvincz.transcript.tts.client.Client
import cz.marvincz.transcript.tts.model.AzureSpeaker
import cz.marvincz.transcript.tts.model.ExtraVoices
import cz.marvincz.transcript.tts.model.Line
import cz.marvincz.transcript.tts.model.SpeechPart
import cz.marvincz.transcript.tts.model.Transcript
import cz.marvincz.transcript.tts.model.VoiceLibrary
import cz.marvincz.transcript.tts.utils.combineAudioFiles
import cz.marvincz.transcript.tts.utils.json
import java.io.File
import java.util.Properties
import kotlin.time.Duration.Companion.milliseconds

fun main(args: Array<String>) = Application().main(args)

private class Application : CliktCommand() {
    init {
        installMordantMarkdown()
    }

    private val transcript: Transcript by option().file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .convert {
            json.decodeFromString<Transcript>(it.readText())
        }.required().help { "The transcript JSON file" }

    private val voices: VoiceLibrary by option().file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .convert {
            json.decodeFromString<VoiceLibrary>(it.readText())
        }.required().help { "The voice library" } // TODO describe format

    private val output: File by option().file(canBeDir = false).required()
        .help { "The output file where the generated audio will be written" }

    private val azureProperties: Properties by option().file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .convert {
            Properties().apply { load(it.inputStream()) }
        }.required().help { "The Azure API properties file. Must contain keys `subscription_key` and `region`" }

    private val sections: List<Section>? by option().convert { parseSections(it, name) }.help {
        """
                *(optional)* The section of the transcript to use. When not specified, the whole transcript is used.
                Format is a comma-separated list of pages, optionally with a colon and lines either as a single number or a range with two numbers separated by a hyphen.
                
                Example: `T100:3-41,T101,T102,T103`
            """.trimIndent()
    }

    override fun run() {
        val lines = transcript.lines
            .joinTexts()
            .filter { line -> sections?.any { it.matches(line) } != false }
            .filter { it.text != null }

        if (lines.isEmpty()) {
            println("No text found")
            return
        }

        val voiceMap = voices.voices + voices.mapExtrasToSpeakers(transcript)

        val missingVoices = lines.mapTo(mutableSetOf(), Line::speakerOrNarrator).filter { speaker ->
            speaker !in voiceMap && voices.extras.none {
                it.assignment == ExtraVoices.AssignmentType.LineRoundRobin && it.speakerRegex.matches(speaker)
            }
        }
        require(missingVoices.isEmpty()) { "Missing TTS voices for speakers: $missingVoices" }

        val extraVoicesByLine = voices.extras
            .filter { it.assignment == ExtraVoices.AssignmentType.LineRoundRobin }
            .map(::ExtraVoicesByLineTracker)

        val client = Client(
            subscriptionKey = azureProperties.getProperty("subscription_key"),
            region = azureProperties.getProperty("region"),
        )

        val subtitleFile = File(output.path.replaceAfterLast('.', "vtt"))
        subtitleFile.writeText(subtitlesHeader)

        val chunks = lines.map { line ->
            val speakerName = line.speakerOrNarrator
            val speaker = voiceMap[speakerName] ?: extraVoicesByLine.first { it.matches(speakerName) }[speakerName]
            SpeechPart(speaker, speakerName, requireNotNull(line.text))
        }.splitToChunks()

        var chunkOffset = 0.milliseconds
        try {
            val outputs = chunks.mapIndexed { index, chunk ->
                val progress = getProgressBar(index, chunks.size)

                progress.update { total = 1_000 }
                val result = client.synthesize(chunk) { progress.update(1_000 * it) }
                progress.update(1_000)

                val chunkOutput = output.chunkTempFile(index)
                chunkOutput.writeBytes(result.audioData)
                subtitleFile.appendText(getSubtitles(result.timings, chunkOffset))

                chunkOffset += result.duration
                chunkOutput
            }

            combineAudioFiles(outputs, output)
        } finally {
            chunks.indices.forEach { runCatching { output.chunkTempFile(it).delete() } }
        }
    }

    private fun getProgressBar(index: Int, count: Int) = progressBarLayout {
        text("Chunk ${index + 1}/$count")
        percentage()
        progressBar()
    }.animateOnThread(terminal).also { it.execute() }

    private data class Section(val page: String, val lines: IntRange) {
        fun matches(line: Line): Boolean = line.page == page && line.line in lines
    }

    private fun parseSections(value: String, optionName: String) = value.split(',').map { section ->
        val matchGroups =
            requireNotNull(sectionRegex.matchEntire(section)) { "$optionName does not match the format" }.groups

        val page = matchGroups["page"]!!.value
        val lineFrom = matchGroups["lineFrom"]?.value?.toInt()
        val lineTo = matchGroups["lineTo"]?.value?.toInt()

        Section(
            page = page,
            lines = lineFrom?.let { IntRange(it, lineTo ?: lineFrom) } ?: IntRange(Int.MIN_VALUE, Int.MAX_VALUE)
        )
    }

    private fun VoiceLibrary.mapExtrasToSpeakers(transcript: Transcript): List<Pair<String, AzureSpeaker>> =
        extras.filter { it.assignment == ExtraVoices.AssignmentType.SpeakerRoundRobin }
            .flatMap { extra ->
                val speakers = transcript.speakers.filter { extra.speakerRegex.matches(it) }
                speakers.mapIndexed { index, speaker -> speaker to extra.voices[index % extra.voices.size] }
            }

    private data class ExtraVoicesByLineTracker(
        val extra: ExtraVoices,
        var index: Int = 0,
        var lastSpeaker: String? = null,
    ) {
        fun matches(speaker: String) = extra.speakerRegex.matches(speaker)

        operator fun get(speaker: String): AzureSpeaker {
            require(matches(speaker))

            if (lastSpeaker != speaker && lastSpeaker != null) {
                index = (index + 1) % extra.voices.size
            }
            lastSpeaker = speaker

            return extra.voices[index]
        }
    }
}

private fun File.chunkTempFile(index: Int) =
    File(buildString {
        append(path.substringBeforeLast('.'))
        append(".temp")
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