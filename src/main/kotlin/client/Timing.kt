package cz.marvincz.transcript.tts.client

import com.microsoft.cognitiveservices.speech.SpeechSynthesisBoundaryType
import cz.marvincz.transcript.tts.model.Boundary
import cz.marvincz.transcript.tts.model.SpeechPart
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun getTimings(speeches: List<SpeechPart>, ssml: String, boundaries: List<Boundary>): List<Timing> {
    var index = 0

    return speeches.flatMap { speech ->
        val speaker = speech.speakerName
        val sentences = getSentences(speech)

        val timings = mutableListOf<Timing>()
        var wipTiming: Timing? = null
        sentences.forEach { sentence ->
            val xmlSentence = sentence.fixForXml()
            index = ssml.indexOf(xmlSentence, index + 1)

            val start = boundaries.firstOrNull { it.textOffset == index }?.offset ?: wipTiming?.start
            val end = boundaries.firstOrNull { it.textEndOffset == index + xmlSentence.length }?.endOffset

            val timing = wipTiming.join(Timing(speaker, sentence, start ?: 0.milliseconds, end ?: 0.milliseconds))
            wipTiming = null

            if (start != null && end != null) {
                timings.add(timing)
            } else if (start == null && end == null) {
                throw Exception("Could not find timing for \"$sentence\"")
            } else if (start == null) {
                timings.add(timings.removeLast().join(timing))
            } else {
                wipTiming = timing
            }
        }
        // If we didn't find the end for the last sentence in this <voice> tag, use the last sound in the tag for the end time
        wipTiming?.let { wip ->
            val indexOfClosingTag = ssml.indexOf("</voice>", index)
            val endOffset = boundaries.last { it.textOffset < indexOfClosingTag }.endOffset
            timings.add(wip.copy(end = endOffset))
        }

        return@flatMap timings
    }
}

private val BASE_PAUSE = 12.5.milliseconds
private const val INITIAL_PAUSE_FACTOR = 21

fun getTimingsFromSpeech(speeches: List<SpeechPart>, ssml: String, boundaries: List<Boundary>): List<Timing> {
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
                    ssml = ssml,
                    boundaries = boundaries.filter { it.textOffset in index .. endIndex }.addPunctuationToPause(),
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
            add(
                Timing(
                    speaker = speaker,
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
            while (index < lastIndex && this[index + 1].type == SpeechSynthesisBoundaryType.Punctuation) {
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
    if (boundary.type == SpeechSynthesisBoundaryType.Punctuation) pause += boundary.duration

    var i = index
    while (i < lastIndex && this[i + 1].type == SpeechSynthesisBoundaryType.Punctuation) {
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
                timing.joinFitness(list[index + 1])
            } else null
        }

        val minFitness = fitness.minOrNull() ?: break
        val index = fitness.indexOf(minFitness)

        if (minFitness < 15000 && list[index].joinedLength(list[index + 1]) < 40) {
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

/**
 * First pass: Split text by punctuation
 * Second pass: Join very short "sentences" on one line
 * Third pass: Split very long unbroken "sentences"
 */
fun getSentences(speech: SpeechPart): List<String> {
    val sentences = sentenceRegex.findAll(speech.text).map { it.groups[1]!!.value }.toList()

    return splitSentences(joinSentences(sentences))
}

private fun joinSentences(sentences: List<String>): List<String> {
    var joined: Boolean
    var list = sentences

    do {
        if (list.size == 1) break

        joined = false
        val fitness = list.mapIndexedNotNull { index, sentence ->
            if (index < list.lastIndex) {
                sentence.joinFitness(list[index + 1])
            } else null
        }

        val minFitness = fitness.minOrNull()!!
        val index = fitness.indexOf(minFitness)

        if (minFitness <= 50 && list[index].joinedLength(list[index + 1]) < 80) {
            list = buildList {
                if (index > 0) addAll(list.subList(0, index))
                add("${list[index]} ${list[index + 1]}")
                if (index < list.lastIndex - 1) addAll(list.subList(index + 2, list.size))
            }
            joined = true
        }
    } while (joined)

    return list
}

fun splitSentences(sentences: List<String>): List<String> {
    return buildList {
        sentences.forEach { sentence ->
            if (sentence.length <= 90) add(sentence)
            else {
                val splitIndices = splitPoints.flatMap { split -> split.findAll(sentence).map { it.range.first } }
                if (splitIndices.isEmpty()) add(sentence)
                else {
                    val best = splitIndices.maxByOrNull { splitFitness(it, sentence.length) }!!
                    val bestTwo = if (max(best, sentence.length - best) > 75 && splitIndices.size > 1) {
                        splitIndices.flatMap { first ->
                            splitIndices.mapNotNull { second -> if (second > first) first to second else null }
                        }.maxByOrNull { splitFitnessTwo(it, sentence.length) }
                    } else null

                    if (bestTwo != null) {
                        add(sentence.substring(0, bestTwo.first).trim())
                        add(sentence.substring(bestTwo.first, bestTwo.second).trim())
                        add(sentence.substring(bestTwo.second).trim())
                    } else {
                        add(sentence.substring(0, best).trim())
                        add(sentence.substring(best).trim())
                    }
                }
            }
        }
    }
}

private fun splitFitness(index: Int, length: Int) = index * (length - index)

private fun splitFitnessTwo(indices: Pair<Int, Int>, length: Int) =
    if (indices.second > indices.first) indices.first * (indices.second - indices.first) * (length - indices.second)
    else 0

data class Timing(
    val speaker: String,
    val text: String,
    val start: Duration,
    val end: Duration,
    val pause: Duration = 0.milliseconds,
) {
    fun join(other: Timing): Timing {
        require(speaker == other.speaker) { "Only lines from the same speaker can be joined for timing" }
        return copy(
            text = "$text ${other.text}",
            end = other.end,
            pause = other.pause,
        )
    }
}

private fun Timing?.join(other: Timing): Timing = this?.join(other) ?: other

private fun Timing.joinFitness(other: Timing) = joinedLength(other) * (pause.inWholeMilliseconds + 100) * text.separatorFitnessWeight()

private fun Timing.joinedLength(other: Timing) = text.length + other.text.length + 1

private fun String.joinFitness(other: String) = joinedLength(other) * separatorFitnessWeight()

private fun String.joinedLength(other: String) = length + other.length + 1

/**
 * We prefer to join lines in some places as opposed to others
 */
private fun String.separatorFitnessWeight() = when {
    endsWith(',') -> 0.7
    last() in listOf('&', '\'', '%', '*') -> 0.4
    last().isLetterOrDigit() -> 0.4
    endsWith(';') -> 0.8
    endsWith("--") -> 1.2
    else -> 1.0
}

private val notSeparators = listOf("Mr", "Mrs", "Ms", "Dr").joinToString("") { "(?<!$it)" }
private val sentenceRegex = Regex("\\s*(.+?$notSeparators(?<separator>\\p{Po}|--|$))(?> |$)")

private const val minSplitSentenceLength = 20
// mostly conjunctions
val splitWords = listOf(
    "and", "but", "or", "so", "yet", "nor", "for", "after", "although", "as", "because", "before", "if", "once",
    "since", "that", "though", "till", "unless", "while", "where", "whether", "in order that", "even though",
    "just as", "in case", "now that", "inasmuch as", "until", "while", "lest", "regardless", "apart from",
    "given that", "in spite of", "on the condition", "only if", "supposing", "in the event",
    "not to mention", "such that", "to the extent that", "although", "despite", "much as", "besides", "conversely",
    "except that", "in order to", "provided", "save that", "to the end that", "by the time", "even if",
    "on condition", "even when", "in as much as", "in the same way that", "not only", "notwithstanding",
    "presuming", "seeing that", "in accordance", "in addition", "in relation to",
    "in the light of", "not to speak of", "with regard to", "who", "what", "why", "when", "how", "which", "about",
)
private val notSplitBehind = splitWords.joinToString("|", prefix = "(?<! (", postfix = ") )")
val splitPoints = splitWords.map { Regex("$notSplitBehind(?<=.{$minSplitSentenceLength} )$it\\b(?= .{${minSplitSentenceLength - it.length}})") }

// (?<! (and|but|or|so|yet|nor|for|after|although|as|because|before|if|once|since|that|though|till|unless|while|where|whether|in order that|even though|just as|in case|now that|inasmuch as|until|while|lest|regardless|apart from|given that|in spite of|on the condition that|only if|supposing|in the event that|not to mention|such that|to the extent that|although|despite|much as|besides|conversely|except that|in order to|provided|save that|to the end that|by the time|even if|on condition that|even when|in as much as|in the same way that|not only|notwithstanding|presuming that|rather|seeing that|in accordance with|in addition to|in relation to|in the light of|not to speak of|with regard to|who|what|why|when|how|which|about) )(?<=.{20} )on the condition that\b(?= .{-1})