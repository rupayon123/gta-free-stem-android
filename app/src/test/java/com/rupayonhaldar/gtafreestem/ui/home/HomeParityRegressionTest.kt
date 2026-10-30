package com.rupayonhaldar.gtafreestem.ui.home

import com.rupayonhaldar.gtafreestem.ui.design.GtaArtwork
import com.rupayonhaldar.gtafreestem.ui.design.artworkForCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HomeParityRegressionTest {
    @Test fun combinedCategoriesKeepTheirDistinctApprovedArtwork() {
        assertEquals(GtaArtwork.SCIENCE, artworkForCategory("Science & Engineering"))
        assertEquals(GtaArtwork.IDEAS, artworkForCategory("AI & Digital Media"))
        assertEquals(GtaArtwork.CODE, artworkForCategory("Coding & Robotics"))
        assertEquals(GtaArtwork.ENGINEERING, artworkForCategory("Makerspace & Fabrication"))
        assertEquals(GtaArtwork.SCIENCE, artworkForCategory("  SCIENCE & ENGINEERING  "))
        assertEquals(GtaArtwork.LIBRARY, artworkForCategory("Library & Community"))
        assertEquals(GtaArtwork.VOLUNTEER, artworkForCategory("Community Volunteering"))
    }

    @Test fun homeUsesOnlyAvailableCategoriesAndDoesNotHideExtraAvailableTopics() {
        val available = listOf("Coding & Robotics", "AI & Digital Media", "Science & Engineering",
            "Family Learning", "Arts & Media", "Makerspace & Fabrication", "Library Programs",
            "Family Learning", "")
        val topics = availableHomeTopics(available)
        assertEquals(listOf("Science & Engineering", "Coding & Robotics", "Makerspace & Fabrication",
            "Arts & Media", "Family Learning", "AI & Digital Media"), topics)
        assertFalse(topics.contains("Career & Mentorship"))
        assertEquals(listOf("AI & Digital Media", "Library Programs"),
            availableHomeTopics(listOf("Library Programs", "AI & Digital Media")))
        assertEquals(emptyList<String>(), availableHomeTopics(emptyList()))
    }
}
