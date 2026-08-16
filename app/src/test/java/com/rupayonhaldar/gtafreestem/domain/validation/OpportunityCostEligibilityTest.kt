package com.rupayonhaldar.gtafreestem.domain.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpportunityCostEligibilityTest {
    @Test
    fun `accepts only explicit unqualified zero-cost wording`() {
        listOf("Free", "Free to join", "No cost", "No fees", "Complimentary", "${'$'}0.00", "0 CAD")
            .forEach { assertTrue(it, OpportunityCostEligibility.isExplicitlyFree(it)) }

        listOf(
            null,
            "",
            "Ask provider",
            "Free trial",
            "Free to join; ${'$'}15 materials fee",
            "Free registration; materials fee may apply",
            "Free to join with optional paid supplies",
            "No cost, but payment required",
            "${'$'}25",
        ).forEach { assertFalse(it, OpportunityCostEligibility.isExplicitlyFree(it)) }
    }
}
