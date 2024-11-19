package cz.marvincz.transcript.tts.client

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
            val xmlSentence = sentence.fixTexts()
            index = ssml.indexOf(xmlSentence, index + 1)

            val start = boundaries.firstOrNull { it.textOffset == index }?.offset ?: wipTiming?.start
            val end = boundaries.firstOrNull {
                it.textOffset == index + xmlSentence.length - 1
                        || it.textOffset + it.text.length == index + xmlSentence.length
            }?.endOffset

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
) {
    fun join(other: Timing): Timing {
        require(speaker == other.speaker) { "Only lines from the same speaker can be joined for timing" }
        return copy(
            text = "$text ${other.text}",
            end = other.end,
        )
    }
}

private fun Timing?.join(other: Timing): Timing = this?.join(other) ?: other

private fun String.joinFitness(other: String) = joinedLength(other) * separatorFitnessWeight()

private fun String.joinedLength(other: String) = length + other.length + 1

private fun String.separatorFitnessWeight() = when {
    endsWith(',') -> 0.7
    last() in listOf('&', '\'', '%', '*') -> 0.2
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