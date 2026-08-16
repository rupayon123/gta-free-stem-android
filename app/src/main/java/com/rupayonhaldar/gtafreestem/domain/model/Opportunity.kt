package com.rupayonhaldar.gtafreestem.domain.model

import kotlinx.serialization.Serializable

/** A free GTA learning opportunity ready for browse and detail screens. */
@Serializable
data class Opportunity(
    val id: String,
    val title: String,
    val organization: String,
    val description: String,
    val summary: String? = null,
    val category: String,
    val categories: List<String> = emptyList(),
    val city: String,
    val region: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val virtual: Boolean = false,
    val startDate: String? = null,
    val endDate: String? = null,
    val deadline: String? = null,
    val ageMin: Int = 0,
    val ageMax: Int? = null,
    val languages: List<String> = listOf("en"),
    val cost: String,
    val sourceUrl: String,
    val registrationUrl: String? = null,
    val status: String = "active",
    val communityFocus: List<String> = emptyList(),
    val accessibility: List<String> = emptyList(),
    val equipment: String? = null,
    val food: String? = null,
    val capacity: String? = null,
    val commitment: String? = null,
    val providerContact: String? = null,
    val freeStatusProof: String? = null,
    val trustedSource: Boolean = false,
    val volunteerHoursEligible: Boolean = false,
    val coopEligible: Boolean = false,
    val tags: List<String> = emptyList(),
    val sources: List<OpportunitySourceEvidence> = emptyList(),
    val translations: Map<String, OpportunityTranslation> = emptyMap(),
)

@Serializable
data class OpportunitySourceEvidence(
    val label: String = "",
    val url: String = "",
    val capturedAt: String = "",
    val confidence: String = "",
)

@Serializable
data class OpportunityTranslation(
    val title: String? = null,
    val organization: String? = null,
    val description: String? = null,
    val summary: String? = null,
    val category: String? = null,
    val city: String? = null,
    val region: String? = null,
    val address: String? = null,
    val cost: String? = null,
    val tags: List<String>? = null,
)
