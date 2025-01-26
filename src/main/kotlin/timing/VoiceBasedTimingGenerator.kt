package cz.marvincz.transcript.tts.timing

import cz.marvincz.transcript.tts.client.fixForXml
import cz.marvincz.transcript.tts.client.recoverFromXml
import cz.marvincz.transcript.tts.model.Boundary
import cz.marvincz.transcript.tts.model.SpeakerType
import cz.marvincz.transcript.tts.model.SpeechPart
import kotlin.time.Duration.Companion.milliseconds

class VoiceBasedTimingGenerator : TimingGenerator {
    companion object {
        private val BASE_PAUSE = 12.5.milliseconds
        private const val INITIAL_PAUSE_FACTOR = 21
        private const val MAX_JOIN_LENGTH = 60
    }

    override fun getTimings(speeches: List<SpeechPart>, ssml: String, boundaries: List<Boundary>): List<Timing> {
        var index = 0
        return speeches.flatMap { speech ->
            val xmlText = speech.text.fixForXml()
            index = ssml.indexOf(xmlText, index)
            val endIndex = index + xmlText.length

            joinShortTimings(
                buildList {
                    index = splitSpeech(
                        speechText = speech.text,
                        speaker = speech.speakerName,
                        speakerType = speech.speaker.speakerType,
                        ssml = ssml,
                        boundaries = boundaries.filter { it.textOffset in index..endIndex }.addPunctuationToPause(),
                        startIndex = index,
                        pauseFactor = INITIAL_PAUSE_FACTOR,
                    )
                }
            )
        }
    }

    private fun MutableList<Timing>.splitSpeech(
        speechText: String,
        speaker: String,
        speakerType: SpeakerType,
        ssml: String,
        boundaries: List<Boundary>,
        startIndex: Int,
        pauseFactor: Int,
    ): Int {
        val splits = boundaries.findSplits(pauseFactor)

        var index = startIndex
        var speechIndex = 0
        var start = boundaries.first()
        splits.forEach { split ->
            val xmlSentence = ssml.substring(index, split.textOffset + split.text.length).recoverFromXml()

            val regex = xmlSentence.split(Regex("\\s"))
                .joinToString(separator = "\\s+(?:(?:\\(|\\[)[\\w\\s]+(?:\\)|\\])\\s*)?") { Regex.escape(it) }.toRegex()
            val match = regex.find(speechText, speechIndex)!!

            val text = match.value.trim()
            if (text.length <= 85 || pauseFactor == 2) {
                if (text.length > 85) TextBasedTimingGenerator().splitSpeech(
                    builtList = this,
                    speechText = speechText,
                    speaker = speaker,
                    speakerType = speakerType,
                    boundaries = boundaries.filter { it.textOffset in start.textOffset..split.textOffset },
                    ssml = ssml,
                    startIndex = index - 1
                )
                else add(
                    Timing(
                        speaker = speaker,
                        speakerType = speakerType,
                        text = text,
                        start = start.offset,
                        end = split.endOffset,
                        pause = split.pause,
                    )
                )
            } else {
                splitSpeech(
                    speechText = text,
                    speaker = speaker,
                    speakerType = speakerType,
                    ssml = ssml,
                    boundaries = boundaries.filter { it.textOffset in start.textOffset..split.textOffset },
                    startIndex = index,
                    pauseFactor = pauseFactor - 1
                )
            }

            start = boundaries.find { it.textOffset > split.textOffset } ?: split
            index = start.textOffset
            speechIndex = match.range.last + 1
        }

        // TODO some fallback if still speechIndex < speech.text.length
        return index
    }

    /**
     * We find boundaries with pause longer than a specified minimum pause.
     * If a word is followed by punctuation, we take the last punctuation as the "split" point,
     * but because of the way [addPunctuationToPause] works, the total pause is on the word, so we use that.
     */
    private fun List<Boundary>.findSplits(pauseFactor: Int): List<Boundary> {
        val minPause = BASE_PAUSE * pauseFactor

        return filterIndexed { index, it -> it.pause >= minPause || index == lastIndex }
            .map {
                var index = indexOf(it)
                while (index < lastIndex && this[index + 1].isPunctuation()) {
                    index++
                }
                this[index].copy(pause = it.pause)
            }.distinctBy { it.textOffset }
    }

    /**
     * The total actual "pause" after a word that is followed by punctuation can be split into multiple values:
     * * pause at the end of the last word
     * * duration of the punctuation
     * * pause at the end of the punctuation
     *
     * If there are more than one punctuation Boundaries after the word, we sum the duration and pause for all of them.
     */
    private fun List<Boundary>.addPunctuationToPause() = mapIndexed { index, boundary ->
        var pause = boundary.pause
        if (boundary.isPunctuation()) pause += boundary.duration

        var i = index
        while (i < lastIndex && this[i + 1].isPunctuation()) {
            i++
            pause += this[i].duration + this[i].pause
        }
        boundary.copy(pause = pause)
    }

    private fun joinShortTimings(timings: List<Timing>): List<Timing> {
        if (timings.size == 1) return timings

        var joined: Boolean
        var list = timings

        do {
            joined = false

            val fitness = list.mapIndexedNotNull { index, timing ->
                if (index < list.lastIndex) {
                    val other = list[index + 1]
                    if (timing.joinedLength(other) < MAX_JOIN_LENGTH) timing.joinFitness(other) else Double.MAX_VALUE
                } else null
            }

            val minFitness = fitness.minOrNull() ?: break
            val index = fitness.indexOf(minFitness)

            if (minFitness < 15000 && list[index].joinedLength(list[index + 1]) < MAX_JOIN_LENGTH) {
                list = buildList {
                    if (index > 0) addAll(list.subList(0, index))
                    add(list[index].join(list[index + 1]))
                    if (index < list.lastIndex - 1) addAll(list.subList(index + 2, list.size))
                }
                joined = true
            }
        } while (joined && list.size > 1)

        return list
    }
}