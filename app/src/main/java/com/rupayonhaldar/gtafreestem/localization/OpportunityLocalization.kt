package com.rupayonhaldar.gtafreestem.localization

import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityTranslation
import java.util.Locale

data class LocalizedOpportunityText(
    val title: String,
    val organization: String,
    val description: String,
    val summary: String,
    val category: String,
    val city: String,
    val region: String,
    val address: String?,
    val cost: String,
    val tags: List<String>,
    val hasDirectTranslation: Boolean,
)

/** Resolves translated feed fields while retaining a complete English fallback. */
object OpportunityLocalization {
    fun categoryName(
        category: String,
        language: AppLanguage,
        catalog: AppStringCatalog,
    ): String = localizedCategoryName(category, language, catalog)

    fun translation(
        opportunity: Opportunity,
        language: AppLanguage,
    ): OpportunityTranslation? {
        val candidates = setOf(
            language.catalogCode.normalizedTag(),
            language.localeTag.normalizedTag(),
        )
        opportunity.translations.entries.firstOrNull { (key, value) ->
            key.normalizedTag() in candidates && value.hasContent()
        }?.value?.let { return it }

        return opportunity.translations.entries.firstOrNull { (key, value) ->
            AppLanguage.matching(key) == language && value.hasContent()
        }?.value
    }

    fun resolve(
        opportunity: Opportunity,
        language: AppLanguage,
        catalog: AppStringCatalog? = null,
    ): LocalizedOpportunityText {
        val translation = translation(opportunity, language)
        val category = translation?.category.nonBlankOr(opportunity.category)
        val localizedCategory = if (translation?.category.isNullOrBlank()) {
            localizedCategoryName(category, language, catalog)
        } else {
            category
        }
        val organization = translation?.organization.nonBlankOr(opportunity.organization)
        val city = translation?.city.nonBlankOr(opportunity.city)
        val description = translation?.description.nonBlankOr(opportunity.description)
        val baseSummary = firstNonBlank(
            translation?.summary,
            translation?.description,
            opportunity.summary,
            opportunity.description,
        ) ?: opportunity.description
        val summary = if (
            language != AppLanguage.ENGLISH &&
            translation == null &&
            catalog != null
        ) {
            localizedSummaryFromTemplate(
                baseSummary = baseSummary,
                category = localizedCategory,
                organization = organization,
                city = city,
                ageRange = opportunity.ageMax?.let { "${opportunity.ageMin}-$it" }
                    ?: "${opportunity.ageMin}+",
                language = language,
                catalog = catalog,
            )
        } else {
            baseSummary
        }

        return LocalizedOpportunityText(
            title = translation?.title.nonBlankOr(opportunity.title),
            organization = organization,
            description = description,
            summary = summary,
            category = localizedCategory,
            city = city,
            region = translation?.region.nonBlankOr(opportunity.region),
            address = firstNonBlank(translation?.address, opportunity.address),
            cost = translation?.cost.nonBlankOr(opportunity.cost),
            tags = translation?.tags
                ?.mapNotNull(String::trimmedOrNull)
                ?.takeIf(List<String>::isNotEmpty)
                ?: opportunity.tags,
            hasDirectTranslation = translation != null,
        )
    }

    private fun localizedSummaryFromTemplate(
        baseSummary: String,
        category: String,
        organization: String,
        city: String,
        ageRange: String,
        language: AppLanguage,
        catalog: AppStringCatalog,
    ): String {
        val resolved = catalog.text(
            key = "summaryTemplate",
            language = language,
            placeholders = mapOf(
                "summary" to baseSummary,
                "category" to category,
                "provider" to organization,
                "city" to city,
                "ages" to ageRange,
            ),
        )
        if (resolved == "summaryTemplate") return baseSummary
        return if (resolved.contains(baseSummary)) resolved else "$resolved\n$baseSummary"
    }

    private fun localizedCategoryName(
        category: String,
        language: AppLanguage,
        catalog: AppStringCatalog?,
    ): String {
        if (catalog == null) return category
        val key = "category" + category
            .replace("&", " And ")
            .split(nonAlphanumeric)
            .filter(String::isNotBlank)
            .joinToString("") { word ->
                word.lowercase(Locale.ROOT).replaceFirstChar(Char::uppercaseChar)
            }
        val localized = catalog.text(key, language)
        return localized.takeUnless { it == key } ?: category
    }

    private val nonAlphanumeric = Regex("[^\\p{L}\\p{N}]+")
}

private fun OpportunityTranslation.hasContent(): Boolean =
    listOf(title, organization, description, summary, category, city, region, address, cost)
        .any { it.trimmedOrNull() != null } ||
        tags.orEmpty().any { it.trimmedOrNull() != null }

private fun String.normalizedTag(): String = trim().replace('_', '-').lowercase(Locale.ROOT)

private fun String?.nonBlankOr(fallback: String): String = trimmedOrNull() ?: fallback

private fun String?.trimmedOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private fun firstNonBlank(vararg values: String?): String? =
    values.firstNotNullOfOrNull(String?::trimmedOrNull)
