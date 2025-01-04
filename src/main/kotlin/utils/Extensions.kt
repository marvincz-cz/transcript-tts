package cz.marvincz.transcript.tts.utils

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.animation.progress.animateOnThread
import com.github.ajalt.mordant.animation.progress.execute
import com.github.ajalt.mordant.terminal.prompt
import com.github.ajalt.mordant.widgets.progress.percentage
import com.github.ajalt.mordant.widgets.progress.progressBar
import com.github.ajalt.mordant.widgets.progress.progressBarLayout
import com.github.ajalt.mordant.widgets.progress.text

fun <T> MutableList<T>.replaceAllIndexed(operator: (Int, T) -> T) {
    val li = listIterator()
    while (li.hasNext()) {
        li.set(operator(li.nextIndex(), li.next()))
    }
}

fun CliktCommand.prompt(prompt: String, choices: List<String>): String? {
    val initials = choices.map { it.take(1).uppercase() } + choices.map { it.take(1).lowercase() }
    require(initials.distinct() == initials) { "All choices must have distinct initials" }

    return terminal.prompt(
        prompt = buildString {
            append(prompt)
            append(" ")
            choices.forEachIndexed { index, choice ->
                append(terminal.theme.info("[${choice.take(1).uppercase()}]"))
                append(terminal.theme.info(choice.drop(1)))
                if (index < choices.lastIndex) append(" / ")
            }
        },
        choices = choices + initials,
        showChoices = false,
    )?.let {
        choices.firstOrNull { choice -> choice.lowercase().startsWith(it.lowercase()) }
    }
}

fun CliktCommand.getProgressBar(label: String) = progressBarLayout {
    text(label)
    percentage()
    progressBar()
}.animateOnThread(terminal).also { it.execute() }