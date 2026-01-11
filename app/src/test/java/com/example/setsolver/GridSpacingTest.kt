package com.example.setsolver

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for GridSpacing data class
 */
class GridSpacingTest {

    @Test
    fun testGridSpacingCreation() {
        val spacing = CardDetector.GridSpacing(
            cardWidth = 200,
            cardHeight = 300,
            gridOriginX = 50,
            gridOriginY = 60,
            numCols = 4
        )

        assertEquals(200, spacing.cardWidth)
        assertEquals(300, spacing.cardHeight)
        assertEquals(50, spacing.gridOriginX)
        assertEquals(60, spacing.gridOriginY)
        assertEquals(4, spacing.numCols)
    }

    @Test
    fun testGridSpacingEquality() {
        val spacing1 = CardDetector.GridSpacing(
            cardWidth = 200,
            cardHeight = 300,
            gridOriginX = 50,
            gridOriginY = 60,
            numCols = 4
        )
        val spacing2 = CardDetector.GridSpacing(
            cardWidth = 200,
            cardHeight = 300,
            gridOriginX = 50,
            gridOriginY = 60,
            numCols = 4
        )

        assertEquals(spacing1, spacing2)
    }

    @Test
    fun testGridSpacingInequality() {
        val spacing1 = CardDetector.GridSpacing(
            cardWidth = 200,
            cardHeight = 300,
            gridOriginX = 50,
            gridOriginY = 60,
            numCols = 4
        )
        val spacing2 = CardDetector.GridSpacing(
            cardWidth = 250,
            cardHeight = 300,
            gridOriginX = 50,
            gridOriginY = 60,
            numCols = 4
        )

        assertNotEquals(spacing1, spacing2)
    }

    @Test
    fun testGridSpacingWithDifferentColumns() {
        val spacing4Cols = CardDetector.GridSpacing(
            cardWidth = 200,
            cardHeight = 300,
            gridOriginX = 0,
            gridOriginY = 0,
            numCols = 4
        )
        val spacing5Cols = CardDetector.GridSpacing(
            cardWidth = 200,
            cardHeight = 300,
            gridOriginX = 0,
            gridOriginY = 0,
            numCols = 5
        )

        assertEquals(4, spacing4Cols.numCols)
        assertEquals(5, spacing5Cols.numCols)
        assertNotEquals(spacing4Cols, spacing5Cols)
    }

    @Test
    fun testGridSpacingWithMargins() {
        val spacingWithMargins = CardDetector.GridSpacing(
            cardWidth = 180,
            cardHeight = 250,
            gridOriginX = 100,
            gridOriginY = 150,
            numCols = 5
        )

        // Verify that margins are properly accounted for
        assertTrue(spacingWithMargins.gridOriginX > 0)
        assertTrue(spacingWithMargins.gridOriginY > 0)
        assertEquals(100, spacingWithMargins.gridOriginX)
        assertEquals(150, spacingWithMargins.gridOriginY)
    }

    @Test
    fun testGridSpacingCopy() {
        val original = CardDetector.GridSpacing(
            cardWidth = 200,
            cardHeight = 300,
            gridOriginX = 50,
            gridOriginY = 60,
            numCols = 4
        )

        val modified = original.copy(numCols = 5)

        assertEquals(200, modified.cardWidth)
        assertEquals(300, modified.cardHeight)
        assertEquals(50, modified.gridOriginX)
        assertEquals(60, modified.gridOriginY)
        assertEquals(5, modified.numCols)
    }
}
