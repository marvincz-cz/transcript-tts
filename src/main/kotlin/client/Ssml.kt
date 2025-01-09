package cz.marvincz.transcript.tts.client

import cz.marvincz.transcript.tts.model.SpeechPart

fun toSSML(speeches: List<SpeechPart>) = buildString {
    append("<speak version=\"1.0\" xmlns=\"https://www.w3.org/2001/10/synthesis\" xmlns:mstts=\"https://www.w3.org/2001/mstts\" xml:lang=\"en-CA\">")

    speeches.forEach { speech ->
        val text = speech.text.fixForXml()
        append("<voice name=\"${speech.speaker.voiceId}\">")
        append("<lang xml:lang=\"en-CA\">")
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
        append("</lang>")
        append("</voice>")
    }
    append("</speak>")
}

fun String.fixForXml() = escapeXml()
    .replace("[sic]", "")
    .replace("(sic)", "")
    .replace("(As read)", "")
    .replace("(All)", "")
    .replace("(phonetic)", "")
    .replace("(NO AUDIBLE RESPONSE)", "(NO-AUDIBLE-RESPONSE)")
    .replace("(UNREPORTABLE SOUND)", "(UNREPORTABLE-SOUND)")

private fun String.escapeXml() =
    replace("\"", "&quot;")
        .replace("&", "&amp;")
        .replace("'", "&apos;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

fun String.recoverFromXml() = unescapeXml()
    .replace("(NO-AUDIBLE-RESPONSE)", "(NO AUDIBLE RESPONSE)")
    .replace("(UNREPORTABLE-SOUND)", "(UNREPORTABLE SOUND)")

private fun String.unescapeXml() =
    replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")