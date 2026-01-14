package com.bisayaspeak.ai.ui.roleplay

internal fun splitInlineTranslation(line: String): Pair<String, String?> {
    val trimmed = line.trim()
    if (trimmed.isEmpty()) return "" to null
    val bracketPairs = listOf('（' to '）', '(' to ')', '【' to '】')
    for ((open, close) in bracketPairs) {
        val openIndex = trimmed.lastIndexOf(open)
        val closeIndex = trimmed.lastIndexOf(close)
        if (openIndex != -1 && closeIndex == trimmed.length - 1 && openIndex < closeIndex) {
            val translation = trimmed.substring(openIndex + 1, closeIndex).trim()
            if (translation.isNotEmpty()) {
                val primary = trimmed.substring(0, openIndex).trimEnd()
                return primary.ifBlank { trimmed } to translation
            }
        }
    }
    return trimmed to null
}

internal fun primarySpeechText(line: String): String {
    val (primary, _) = splitInlineTranslation(line)
    return primary.ifBlank { line }.trim()
}
