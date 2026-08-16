package com.rupayonhaldar.gtafreestem.localization

import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

data class LanguageMetadata(
    val englishName: String,
    val nativeName: String,
    val direction: TextDirection,
)

/**
 * Immutable, platform-neutral view of the shared Apple/Android string catalog.
 * Missing or blank target-language values fall back to English, then to the key.
 */
class AppStringCatalog private constructor(
    private val metadataByLanguage: Map<AppLanguage, LanguageMetadata>,
    private val stringsByLanguage: Map<AppLanguage, Map<String, String>>,
) {
    val availableLanguages: Set<AppLanguage>
        get() = stringsByLanguage.keys

    val metadataLanguages: Set<AppLanguage>
        get() = metadataByLanguage.keys

    val sourceKeys: Set<String>
        get() = stringsByLanguage.getValue(AppLanguage.ENGLISH).keys

    fun metadata(language: AppLanguage): LanguageMetadata =
        metadataByLanguage[language] ?: LanguageMetadata(
            englishName = language.catalogCode,
            nativeName = language.catalogCode,
            direction = language.direction,
        )

    fun keys(language: AppLanguage): Set<String> = stringsByLanguage[language]?.keys.orEmpty()

    fun rawText(key: String, language: AppLanguage): String? =
        stringsByLanguage[language]?.get(key)?.trimmedOrNull()

    fun text(
        key: String,
        language: AppLanguage,
        placeholders: Map<String, String> = emptyMap(),
    ): String {
        val template = rawText(key, language)
            ?: rawText(key, AppLanguage.ENGLISH)
            ?: key
        return SafePlaceholderSubstitution.substitute(template, placeholders)
    }

    fun isComplete(language: AppLanguage): Boolean {
        val strings = stringsByLanguage[language] ?: return false
        return keys(language) == sourceKeys && sourceKeys.all { key ->
            strings[key].trimmedOrNull() != null
        }
    }

    companion object {
        private val strictJson = Json {
            isLenient = false
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        fun decode(jsonText: String): AppStringCatalog {
            val root = runCatching { strictJson.parseToJsonElement(jsonText).jsonObject }
                .getOrElse { error ->
                    throw IllegalArgumentException("Invalid app string catalog JSON", error)
                }
            val metadataObject = root["languageMeta"] as? JsonObject
            val metadata = metadataObject?.let(::decodeMetadata).orEmpty()
            val strings = AppLanguage.entries.mapNotNull { language ->
                val values = (root[language.catalogCode] as? JsonObject)
                    ?.mapNotNull { (key, value) ->
                        val text = (value as? JsonPrimitive)
                            ?.takeIf(JsonPrimitive::isString)
                            ?.content
                        text?.let { key to it }
                    }
                    ?.toMap()
                    ?: return@mapNotNull null
                language to values
            }.toMap()

            require(!strings[AppLanguage.ENGLISH].isNullOrEmpty()) {
                "The app string catalog must contain a non-empty English source language"
            }

            return AppStringCatalog(
                metadataByLanguage = metadata,
                stringsByLanguage = strings,
            )
        }

        private fun decodeMetadata(root: JsonObject): Map<AppLanguage, LanguageMetadata> =
            AppLanguage.entries.mapNotNull { language ->
                val value = root[language.catalogCode] as? JsonObject
                    ?: return@mapNotNull null
                val englishName = value.string("label") ?: return@mapNotNull null
                val nativeName = value.string("native") ?: return@mapNotNull null
                val direction = when (value.string("dir")?.lowercase(Locale.ROOT)) {
                    "ltr" -> TextDirection.LEFT_TO_RIGHT
                    "rtl" -> TextDirection.RIGHT_TO_LEFT
                    else -> throw IllegalArgumentException(
                        "Invalid text direction for ${language.catalogCode}",
                    )
                }
                language to LanguageMetadata(
                    englishName = englishName,
                    nativeName = nativeName,
                    direction = direction,
                )
            }.toMap()

        private fun JsonObject.string(key: String): String? =
            (get(key) as? JsonPrimitive)
                ?.takeIf(JsonPrimitive::isString)
                ?.content
                ?.trimmedOrNull()
    }
}

/** Literal, single-pass replacement prevents replacement text from becoming a second template. */
object SafePlaceholderSubstitution {
    private val placeholder = Regex("\\{([A-Za-z][A-Za-z0-9_]*)\\}")

    fun substitute(template: String, values: Map<String, String>): String =
        placeholder.replace(template) { match ->
            values[match.groupValues[1]] ?: match.value
        }
}

private fun String?.trimmedOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)
