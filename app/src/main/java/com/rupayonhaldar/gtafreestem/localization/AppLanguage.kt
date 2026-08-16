package com.rupayonhaldar.gtafreestem.localization

import java.util.Locale

enum class TextDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
}

/** Languages shared with the Apple app's app_strings.json catalog. */
enum class AppLanguage(
    val catalogCode: String,
    val localeTag: String,
    val direction: TextDirection,
) {
    ENGLISH("en", "en", TextDirection.LEFT_TO_RIGHT),
    FRENCH("fr", "fr", TextDirection.LEFT_TO_RIGHT),
    MANDARIN("zh", "zh-Hans", TextDirection.LEFT_TO_RIGHT),
    CANTONESE("yue", "yue-Hant", TextDirection.LEFT_TO_RIGHT),
    PUNJABI("pa", "pa", TextDirection.LEFT_TO_RIGHT),
    URDU("ur", "ur", TextDirection.RIGHT_TO_LEFT),
    TAMIL("ta", "ta", TextDirection.LEFT_TO_RIGHT),
    FILIPINO("tl", "fil", TextDirection.LEFT_TO_RIGHT),
    SPANISH("es", "es", TextDirection.LEFT_TO_RIGHT),
    ARABIC("ar", "ar", TextDirection.RIGHT_TO_LEFT),
    FARSI("fa", "fa", TextDirection.RIGHT_TO_LEFT),
    HINDI("hi", "hi", TextDirection.LEFT_TO_RIGHT),
    PORTUGUESE("pt", "pt", TextDirection.LEFT_TO_RIGHT),
    GUJARATI("gu", "gu", TextDirection.LEFT_TO_RIGHT),
    BENGALI("bn", "bn", TextDirection.LEFT_TO_RIGHT),
    JAPANESE("ja", "ja", TextDirection.LEFT_TO_RIGHT),
    KOREAN("ko", "ko", TextDirection.LEFT_TO_RIGHT),
    HUNGARIAN("hu", "hu", TextDirection.LEFT_TO_RIGHT),
    ;

    val isRightToLeft: Boolean
        get() = direction == TextDirection.RIGHT_TO_LEFT

    companion object {
        /** Returns null instead of silently treating an unsupported tag as English. */
        fun matching(rawTag: String?): AppLanguage? {
            val normalized = rawTag.normalizedLanguageTag() ?: return null

            entries.firstOrNull { language -> language.matchesExact(normalized) }?.let { return it }

            when (normalized) {
                "mandarin" -> return MANDARIN
                "cantonese", "cantonese/yue", "zh-yue" -> return CANTONESE
                "tagalog", "tagalog/filipino", "filipino" -> return FILIPINO
                "farsi", "persian", "farsi/persian", "prs" -> return FARSI
                "english" -> return ENGLISH
                "french" -> return FRENCH
                "punjabi" -> return PUNJABI
                "urdu" -> return URDU
                "tamil" -> return TAMIL
                "spanish" -> return SPANISH
                "arabic" -> return ARABIC
                "hindi" -> return HINDI
                "portuguese" -> return PORTUGUESE
                "gujarati" -> return GUJARATI
                "bengali" -> return BENGALI
                "japanese" -> return JAPANESE
                "korean" -> return KOREAN
                "hungarian" -> return HUNGARIAN
            }

            val primary = normalized.substringBefore('-')
            return entries.firstOrNull { language -> language.matchesExact(primary) }
        }

        fun fromTagOrEnglish(rawTag: String?): AppLanguage = matching(rawTag) ?: ENGLISH

        fun bestMatchOrEnglish(preferredTags: Iterable<String>): AppLanguage =
            preferredTags.firstNotNullOfOrNull(::matching) ?: ENGLISH

        private fun AppLanguage.matchesExact(normalizedTag: String): Boolean =
            catalogCode.lowercase(Locale.ROOT) == normalizedTag ||
                localeTag.lowercase(Locale.ROOT) == normalizedTag

        private fun String?.normalizedLanguageTag(): String? = this
            ?.trim()
            ?.replace('_', '-')
            ?.lowercase(Locale.ROOT)
            ?.takeIf(String::isNotEmpty)
    }
}
