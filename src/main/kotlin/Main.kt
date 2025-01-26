package cz.marvincz.transcript.tts

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.installMordantMarkdown
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.animation.progress.update
import cz.marvincz.transcript.tts.client.Client
import cz.marvincz.transcript.tts.model.ExtraVoices
import cz.marvincz.transcript.tts.model.Line
import cz.marvincz.transcript.tts.model.SpeakerInfo
import cz.marvincz.transcript.tts.model.SpeechPart
import cz.marvincz.transcript.tts.model.Transcript
import cz.marvincz.transcript.tts.model.VoiceMapping
import cz.marvincz.transcript.tts.model.joinTexts
import cz.marvincz.transcript.tts.utils.audioDuration
import cz.marvincz.transcript.tts.utils.combineAudioFiles
import cz.marvincz.transcript.tts.utils.getProgressBar
import cz.marvincz.transcript.tts.utils.json
import cz.marvincz.transcript.tts.utils.prompt
import java.io.File
import java.util.Properties
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun main(args: Array<String>) = Application().subcommands(Transcript(), VoiceLibrary()).main(args)

private class Application : CliktCommand() {
    init {
        installMordantMarkdown()
    }

    override fun helpEpilog(context: Context) =
        terminal.theme.warning("Command help:") + " application <command> --help"

    override fun run() = Unit
}

private abstract class AzureCommand : CliktCommand() {
    val azureConfig: AzureConfig by option("--azure-properties").file(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true,
    )
        .convert {
            val properties = Properties().apply { load(it.inputStream()) }

            AzureConfig(
                properties.getProperty(PROPERTY_SUBSCRIPTION_KEY),
                properties.getProperty(PROPERTY_REGION),
            )
        }.required()
        .help { "The Azure API properties file. Must contain keys `$PROPERTY_SUBSCRIPTION_KEY` and `$PROPERTY_REGION`" }

    companion object {
        private const val PROPERTY_SUBSCRIPTION_KEY = "subscription_key"
        private const val PROPERTY_REGION = "region"
    }

    data class AzureConfig(val subscriptionKey: String, val region: String)
}

private class Transcript : AzureCommand() {
    override fun help(context: Context) = "Generate speech from a transcript"

    private val transcript: Transcript by option().file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .convert {
            json.decodeFromString<Transcript>(it.readText())
        }.required().help { "The transcript JSON file" }

    private val voices: VoiceMapping by option().file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .convert {
            json.decodeFromString<VoiceMapping>(it.readText())
        }.required().help { "The voice mapping" } // TODO describe format

    private val output: File by option().file(canBeDir = false).required()
        .help { "The output file where the generated audio will be written" }

    private val sections: List<Section>? by option().convert { parseSections(it, name) }.help {
        """
            *(optional)* The sections of the transcript to use. When not specified, the whole transcript is used.
            Format is a comma-separated list of sections. A section is a page or a range of pages as first and last page separated by two dots.
            A single page can optionally specify lines, using a colon and a single line number or a range as two numbers separated by a hyphen.
            
            Example: `T100:3-41,T100..T103,T104:1`
        """.trimIndent()
    }

    private val muteMode: MuteMode by option().enum<MuteMode>().default(MuteMode.MUTE).help {
        """
            *(optional)* The method to deal with sections that need to be muted (i.e. `(INDISCERNIBLE)`).
             
            * MUTE *(default)* - mute the section. May be inaccurate in some cases.
            * EXPORT - export information about the sections, so that they can be muted later manually. 
        """.trimIndent()
    }

    private var continueFromIndex: Int = -1
    private var continueFromDuration: Duration? = null

    override fun run() {
        if (output.exists()) {
            val choice = prompt("$output exists", listOf("Overwrite", "Exit"))

            if (choice != "Overwrite") throw PrintMessage("Exiting.")
        }

        val lines = transcript.lines
            .joinTexts()
            .filter { line -> sections?.any { it.matches(line) } != false }
            .filter { it.text != null }

        if (lines.isEmpty()) throw PrintMessage("No text found", printError = true)

        val voiceMap = voices.voices + voices.mapExtrasToSpeakers(transcript)

        val missingVoices = lines.mapTo(mutableSetOf(), Line::speakerOrNarrator).filter { speaker ->
            speaker !in voiceMap && voices.extras.none {
                it.assignment == ExtraVoices.AssignmentType.LineRoundRobin && it.matches(speaker)
            }
        }
        require(missingVoices.isEmpty()) { "Missing TTS voices for speakers: $missingVoices" }

        val extraVoicesByLine = voices.extras
            .filter { it.assignment == ExtraVoices.AssignmentType.LineRoundRobin }
            .map(::ExtraVoicesByLineTracker)

        if (chunkTempFile(0).exists()) {
            val choice = prompt("Previous unfinished generation found", listOf("Resume", "Overwrite", "Exit"))

            val maxTempIndex = generateSequence(0) { it + 1 }.first { !chunkTempFile(it).exists() } - 1

            when (choice) {
                "Resume" -> {
                    continueFromIndex = maxTempIndex
                    continueFromDuration = audioDuration(tempFiles(maxTempIndex))
                    echo("Resuming after ${maxTempIndex + 1} previously generated chunks")
                }

                "Overwrite" -> tempFiles(maxTempIndex).forEach { it.delete() }

                else -> throw PrintMessage("Exiting.")
            }
        }

        val client = Client(azureConfig.subscriptionKey, azureConfig.region)

        if (continueFromDuration == null) {
            subtitleFile.writeText(subtitlesHeader)
            if (muteMode == MuteMode.EXPORT) mutedSectionsHeader()
        }

        val chunks = lines.map { line ->
            val speakerName = line.speakerOrNarrator
            val speaker = voiceMap[speakerName] ?: extraVoicesByLine.first { it.matches(speakerName) }[speakerName]
            SpeechPart(speaker, speakerName, requireNotNull(line.text))
        }.splitToChunks()

        var chunkOffset = continueFromDuration ?: 0.milliseconds
        chunks.forEachIndexed { index, chunk ->
            if (index <= continueFromIndex) {
                echo("Skipping chunk ${index + 1}")
                return@forEachIndexed
            }

            val progress = getProgressBar("Chunk ${index + 1}/${chunks.size}")

            progress.update { total = 1_000 }
            val result = client.synthesize(chunk, muteMode == MuteMode.MUTE) {
                progress.update(1_000 * it)
            }
            progress.update(1_000)

            chunkTempFile(index).writeBytes(result.audioData)
            subtitleFile.appendText(getSubtitles(result.timings, chunkOffset))
            if (muteMode == MuteMode.EXPORT) {
                exportMutedSections(result, chunkOffset)
            }

            chunkOffset += result.duration
        }

        val tempFiles = chunks.tempFiles()
        combineAudioFiles(tempFiles, output)
        tempFiles.forEach { it.deleteOnExit() }

        echo("Saved to ${output.absolutePath}")
    }

    private data class Section(val pages: IntRange, val lines: IntRange) {
        constructor(pages: ClosedRange<String>, lines: IntRange) : this(
            pages.start.substringAfter("T").toInt()..pages.endInclusive.substringAfter("T").toInt(),
            lines
        )

        fun matches(line: Line) = line.page.substringAfter("T").toInt() in pages && line.line in lines
    }

    private fun parseSections(value: String, optionName: String) = value.split(',').map { section ->
        val matchGroups =
            requireNotNull(sectionRegex.matchEntire(section)) { "$optionName does not match the format" }.groups

        val page = matchGroups["page"]!!.value
        val endPage = matchGroups["endPage"]?.value
        val lineFrom = matchGroups["lineFrom"]?.value?.toInt()
        val lineTo = matchGroups["lineTo"]?.value?.toInt()

        Section(
            pages = page..(endPage ?: page),
            lines = lineFrom?.let { it..(lineTo ?: it) } ?: Int.MIN_VALUE..Int.MAX_VALUE
        )
    }

    private fun VoiceMapping.mapExtrasToSpeakers(transcript: Transcript): List<Pair<String, SpeakerInfo>> =
        extras.filter { it.assignment == ExtraVoices.AssignmentType.SpeakerRoundRobin }
            .flatMap { extra ->
                val speakers = transcript.speakers.filter { extra.matches(it) }
                speakers.mapIndexed { index, speaker ->
                    speaker to SpeakerInfo(
                        extra.voices[index % extra.voices.size],
                        extra.speakerType,
                    )
                }
            }

    private data class ExtraVoicesByLineTracker(
        val extra: ExtraVoices,
        var index: Int = 0,
        var lastSpeaker: String? = null,
    ) {
        fun matches(speaker: String) = extra.matches(speaker)

        operator fun get(speaker: String): SpeakerInfo {
            require(matches(speaker))

            if (lastSpeaker != speaker && lastSpeaker != null) {
                index = (index + 1) % extra.voices.size
            }
            lastSpeaker = speaker

            return SpeakerInfo(extra.voices[index], extra.speakerType)
        }
    }

    private val subtitleFile by lazy { File(output.path.replaceAfterLast('.', "ass")) }

    private val mutedSectionsFile by lazy { File(output.path.substringBeforeLast('.') + "-mute.csv") }

    private fun mutedSectionsHeader() {
        mutedSectionsFile.writeText("START;LENGTH")
    }

    private fun exportMutedSections(result: Client.TtsResult, chunkOffset: Duration) {
        mutedSectionsFile.appendText(
            result.mutedSections
                .map { it.copy(start = it.start + chunkOffset, end = it.end + chunkOffset) }
                .joinToString(separator = "") { "\n${it.start.rounded()};${it.duration.rounded()}" }
        )
    }

    private fun Duration.rounded() = inWholeMilliseconds.milliseconds

    private fun chunkTempFile(index: Int) =
        File(buildString {
            append(output.path.substringBeforeLast('.'))
            append(".temp")
            append(index + 1)
            append('.')
            append(output.path.substringAfterLast('.'))
        })

    private fun tempFiles(lastIndex: Int) = (0..lastIndex).map { chunkTempFile(it) }

    private fun <T> List<T>.tempFiles() = tempFiles(lastIndex)

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

    private val sectionRegex =
        Regex("(?<page>T\\d+)(?:\\.\\.(?<endPage>T\\d+)|:(?<lineFrom>\\d+)(?:-(?<lineTo>\\d+))?)?")

    private enum class MuteMode {
        MUTE, EXPORT
    }

    companion object {
        private const val CHUNK_TEXT_LENGTH_LIMIT = 8_000
        private const val CHUNK_SPEECHES_LIMIT = 45
    }
}

private class VoiceLibrary : AzureCommand() {
    override fun help(context: Context) = "Generate sample audio for all available voices and exit"

    override fun run() {
        val client = Client(azureConfig.subscriptionKey, azureConfig.region)
        val dir = File("voices").apply { mkdir() }

        val voices = client.getAllVoices()

        val progress = getProgressBar("Generating")
        progress.update { total = voices.size.toLong() }
        voices
            .flatMap { voice -> listOf(voice to null) + voice.styleList.map { voice to it } }
            .forEachIndexed { index, (voice, style) ->
                client.generateSpeechSample(voice, style)
                progress.update(index + 1)
            }

        echo("Generated sample audio for all available voices at ${dir.absolutePath}")
    }
}