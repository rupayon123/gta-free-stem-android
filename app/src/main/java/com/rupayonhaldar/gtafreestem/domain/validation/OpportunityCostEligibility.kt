package com.rupayonhaldar.gtafreestem.domain.validation

import java.text.Normalizer
import java.util.Locale

/** Fail-closed cost classification: ambiguous or partly-paid listings are excluded. */
object OpportunityCostEligibility {
    private val explicitZeroCostPhrases = setOf(
        "free",
        "free admission",
        "free event",
        "free program",
        "free registration",
        "free to attend",
        "free to join",
        "free to participate",
        "free to register",
        "complimentary",
        "no charge",
        "no cost",
        "no fee",
        "no fees",
        "zero charge",
        "zero cost",
        "zero fee",
    )

    private val explicitZeroAmount = Regex(
        """^(?:[${'$'}£€¥₹]\s*0(?:[.,]0{1,2})?|(?:cad|usd|eur|gbp)\s*0(?:[.,]0{1,2})?|0(?:[.,]0{1,2})?\s*(?:cad|usd|eur|gbp|dollars?)?)$""",
        RegexOption.IGNORE_CASE,
    )

    fun isExplicitlyFree(rawCost: String?): Boolean {
        val cost = normalize(rawCost) ?: return false
        return cost in explicitZeroCostPhrases ||
            explicitZeroAmount.matches(cost)
    }

    private fun normalize(rawCost: String?): String? {
        if (rawCost.isNullOrBlank()) return null
        return Normalizer.normalize(rawCost, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .trim()
            .replace(Regex("\\s+"), " ")
            .ifBlank { null }
    }
}
