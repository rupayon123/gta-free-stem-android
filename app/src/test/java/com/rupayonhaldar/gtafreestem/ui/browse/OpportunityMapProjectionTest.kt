package com.rupayonhaldar.gtafreestem.ui.browse

import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpportunityMapProjectionTest {
    @Test
    fun invalidCoordinatesAreRejectedAndNeverBecomePins() {
        assertFalse(MapCoordinate(Double.NaN, -79.38).isValid)
        assertFalse(MapCoordinate(43.65, Double.POSITIVE_INFINITY).isValid)
        assertFalse(MapCoordinate(91.0, -79.38).isValid)
        assertFalse(MapCoordinate(43.65, -181.0).isValid)

        assertNull(opportunity(latitude = null, longitude = -79.38).toMapOpportunityPin())
        assertNull(opportunity(latitude = 43.65, longitude = null).toMapOpportunityPin())
        assertNull(opportunity(latitude = 93.0, longitude = -79.38).toMapOpportunityPin())
        assertNotNull(opportunity(latitude = 43.65, longitude = -79.38).toMapOpportunityPin())
    }

    @Test
    fun dynamicViewportContainsEveryValidCoordinateWithPadding() {
        val mississauga = MapCoordinate(latitude = 43.5890, longitude = -79.6441)
        val oshawa = MapCoordinate(latitude = 43.8971, longitude = -78.8658)
        val viewport = mapViewportFor(
            listOf(
                mississauga,
                MapCoordinate(Double.NaN, 0.0),
                oshawa,
            ),
        )

        assertTrue(viewport.contains(mississauga))
        assertTrue(viewport.contains(oshawa))
        assertTrue(viewport.west < mississauga.longitude)
        assertTrue(viewport.east > oshawa.longitude)
        assertTrue(viewport.south < mississauga.latitude)
        assertTrue(viewport.north > oshawa.latitude)
    }

    @Test
    fun projectionKeepsGeographicEastRightAndNorthUp() {
        val southwest = MapCoordinate(latitude = 43.50, longitude = -79.80)
        val northeast = MapCoordinate(latitude = 44.00, longitude = -79.00)
        val viewport = mapViewportFor(listOf(southwest, northeast))

        val southwestPosition = requireNotNull(projectMapCoordinate(southwest, viewport))
        val northeastPosition = requireNotNull(projectMapCoordinate(northeast, viewport))

        assertTrue(southwestPosition.x < northeastPosition.x)
        assertTrue(southwestPosition.y > northeastPosition.y)
        assertTrue(southwestPosition.x in 0f..1f)
        assertTrue(southwestPosition.y in 0f..1f)
        assertTrue(northeastPosition.x in 0f..1f)
        assertTrue(northeastPosition.y in 0f..1f)
    }

    @Test
    fun oneLocationGetsAReadableViewportAndProjectsNearTheCentre() {
        val coordinate = MapCoordinate(latitude = 43.6532, longitude = -79.3832)
        val viewport = mapViewportFor(listOf(coordinate))
        val projected = requireNotNull(projectMapCoordinate(coordinate, viewport))

        assertTrue(viewport.contains(coordinate))
        assertEquals(0.5f, projected.x, 0.001f)
        assertEquals(0.5f, projected.y, 0.001f)
    }

    @Test
    fun projectionRejectsCoordinatesOutsideTheViewport() {
        val outside = MapCoordinate(latitude = 45.0, longitude = -79.0)

        assertNull(projectMapCoordinate(outside, GreaterTorontoAreaViewport))
    }

    @Test
    fun coincidentPinsAreSpreadDeterministicallyForIndividualSelection() {
        val coordinate = MapCoordinate(43.6532, -79.3832)
        val centre = ProjectedMapPosition(0.5f, 0.5f)
        val pins = listOf(
            mapPin("one", coordinate) to centre,
            mapPin("two", coordinate) to centre,
        )

        val firstPass = spreadCoincidentPins(pins)
        val secondPass = spreadCoincidentPins(pins)

        assertEquals(firstPass, secondPass)
        assertNotEquals(firstPass[0].second, firstPass[1].second)
        firstPass.forEach { (_, position) ->
            assertTrue(position.x in 0.04f..0.96f)
            assertTrue(position.y in 0.04f..0.96f)
        }
    }

    @Test
    fun denseProjectedPinsClusterDeterministicallyWithoutLosingRecords() {
        val coordinate = MapCoordinate(43.6532, -79.3832)
        val projectedPins = listOf(
            mapPin("one", coordinate) to ProjectedMapPosition(0.10f, 0.10f),
            mapPin("two", coordinate) to ProjectedMapPosition(0.18f, 0.19f),
            mapPin("three", coordinate) to ProjectedMapPosition(0.82f, 0.84f),
        )

        val firstPass = clusterProjectedPins(projectedPins)
        val secondPass = clusterProjectedPins(projectedPins)

        assertEquals(firstPass, secondPass)
        assertEquals(2, firstPass.size)
        assertEquals(listOf("one", "two"), firstPass.first().pins.map(MapOpportunityPin::id))
        assertEquals(
            projectedPins.map { it.first.id }.sorted(),
            firstPass.flatMap(ProjectedMapCluster::pins).map(MapOpportunityPin::id).sorted(),
        )
        firstPass.forEach { cluster ->
            assertTrue(cluster.position.x in 0f..1f)
            assertTrue(cluster.position.y in 0f..1f)
        }
    }

    private fun opportunity(
        latitude: Double?,
        longitude: Double?,
    ) = Opportunity(
        id = "test",
        title = "Robotics club",
        organization = "Community Lab",
        description = "Build a robot.",
        category = "Coding & Robotics",
        city = "Toronto",
        region = "Toronto",
        latitude = latitude,
        longitude = longitude,
        ageMin = 13,
        cost = "Free",
        sourceUrl = "https://example.org/program",
    )

    private fun mapPin(id: String, coordinate: MapCoordinate) = MapOpportunityPin(
        id = id,
        title = "Opportunity $id",
        organization = "Community Lab",
        locationLabel = "Toronto",
        coordinate = coordinate,
    )
}
