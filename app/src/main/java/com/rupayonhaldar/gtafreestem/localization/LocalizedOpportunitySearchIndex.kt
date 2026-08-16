package com.rupayonhaldar.gtafreestem.localization

import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import java.text.Normalizer
import java.util.Locale

/** Search groundwork that indexes the selected language and the source-language fields. */
object LocalizedOpportunitySearchIndex {
    fun text(
        opportunity: Opportunity,
        language: AppLanguage,
        catalog: AppStringCatalog? = null,
    ): String {
        val localized = OpportunityLocalization.resolve(opportunity, language, catalog)
        return buildList {
            add(localized.title)
            add(localized.organization)
            add(localized.description)
            add(localized.summary)
            add(localized.category)
            add(localized.city)
            add(localized.region)
            localized.address?.let(::add)
            addAll(localized.tags)

            if (language != AppLanguage.ENGLISH) {
                add(opportunity.title)
                add(opportunity.organization)
                add(opportunity.description)
                opportunity.summary?.let(::add)
                add(opportunity.category)
                addAll(opportunity.categories)
                add(opportunity.city)
                add(opportunity.region)
                opportunity.address?.let(::add)
                addAll(opportunity.tags)
            }

            addAll(opportunity.communityFocus)
            addAll(opportunity.accessibility)
        }.mapNotNull { it.trimmedOrNull() }
            .distinct()
            .joinToString(" ")
    }

    fun normalizedText(
        opportunity: Opportunity,
        language: AppLanguage,
        catalog: AppStringCatalog? = null,
    ): String = normalize(text(opportunity, language, catalog))

    fun matches(
        opportunity: Opportunity,
        query: String,
        language: AppLanguage,
        catalog: AppStringCatalog? = null,
    ): Boolean {
        val index = normalizedText(opportunity, language, catalog)
        val terms = normalize(query).split(' ').filter(String::isNotBlank).distinct()
        return terms.all(index::contains)
    }

    private fun normalize(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .lowercase(Locale.ROOT)
        .trim()
        .replace(whitespace, " ")

    private fun String?.trimmedOrNull(): String? =
        this?.trim()?.takeIf(String::isNotEmpty)

    private val combiningMarks = Regex("\\p{M}+")
    private val whitespace = Regex("\\s+")
}
