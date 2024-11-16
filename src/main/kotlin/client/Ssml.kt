package cz.marvincz.transcript.tts.client

import cz.marvincz.transcript.tts.model.SpeechPart

fun toSSML(speeches: List<SpeechPart>) = buildString {
    append("<speak version=\"1.0\" xmlns=\"https://www.w3.org/2001/10/synthesis\" xmlns:mstts=\"https://www.w3.org/2001/mstts\" xml:lang=\"en-CA\">")

    speeches.forEach { speech ->
        val text = speech.text.fixTexts()
        append("<voice name=\"${speech.speaker.voiceId}\">")
        speech.speaker.expression?.let { expression ->
            append("<mstts:express-as style=\"${expression.style}\"")
            expression.role?.let { append(" role=\"$it\"") }
            expression.styleDegree?.let { append(" styledegree=\"$it\"") }
            append(">")
            append(text)
            append("</mstts:express-as>")
        } ?: run {
            append(text)
        }
        append("</voice>")
    }
    append("</speak>")
}

fun String.fixTexts() = escapeXml()
    .replace("[sic]", "")
    .replace("(NO AUDIBLE RESPONSE)", "<mstts:silence type=\"Leading-exact\" value=\"2s\" />")

private fun String.escapeXml() =
    replace("\"", "&quot;")
        .replace("&", "&amp;")
        .replace("'", "&apos;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")