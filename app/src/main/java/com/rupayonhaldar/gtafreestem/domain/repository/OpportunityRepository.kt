package com.rupayonhaldar.gtafreestem.domain.repository

import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSnapshot
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters

interface OpportunityRepository {
    /** Loads the best validated last-good or bundled snapshot without waiting for the network. */
    suspend fun bootstrap(): OpportunityFeedSnapshot

    /** Tries GitHub Raw, then jsDelivr, and returns a validated local fallback if both fail. */
    suspend fun refresh(): OpportunityFeedSnapshot

    fun current(): OpportunityFeedSnapshot?

    fun search(
        query: String = "",
        filters: OpportunitySearchFilters = OpportunitySearchFilters(),
    ): List<Opportunity>

    fun findById(id: String): Opportunity?
}

class OpportunityRepositoryException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)
