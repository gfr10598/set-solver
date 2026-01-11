package com.example.setsolver

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for the SetFinder class
 */
class SetFinderTest {

    private val setFinder = SetFinder()

    @Test
    fun testInvalidSet_allIdentical() {
        // Three identical cards should be rejected (must differ in at least one dimension)
        val card1 = Card(
            Card.Number.ONE,
            Card.Shape.DIAMOND,
            Card.CardColor.RED,
            Card.Shading.SOLID
        )
        val card2 = Card(
            Card.Number.ONE,
            Card.Shape.DIAMOND,
            Card.CardColor.RED,
            Card.Shading.SOLID
        )
        val card3 = Card(
            Card.Number.ONE,
            Card.Shape.DIAMOND,
            Card.CardColor.RED,
            Card.Shading.SOLID
        )

        assertFalse(setFinder.isValidSet(card1, card2, card3))
    }

    @Test
    fun testValidSet_allDifferent() {
        // Three cards with all attributes different
        val card1 = Card(
            Card.Number.ONE,
            Card.Shape.DIAMOND,
            Card.CardColor.RED,
            Card.Shading.SOLID
        )
        val card2 = Card(
            Card.Number.TWO,
            Card.Shape.OVAL,
            Card.CardColor.GREEN,
            Card.Shading.STRIPED
        )
        val card3 = Card(
            Card.Number.THREE,
            Card.Shape.SQUIGGLE,
            Card.CardColor.PURPLE,
            Card.Shading.OPEN
        )

        assertTrue(setFinder.isValidSet(card1, card2, card3))
    }

    @Test
    fun testValidSet_mixedAttributes() {
        // Some attributes same, some different
        val card1 = Card(
            Card.Number.ONE,
            Card.Shape.DIAMOND,
            Card.CardColor.RED,
            Card.Shading.SOLID
        )
        val card2 = Card(
            Card.Number.TWO,
            Card.Shape.DIAMOND,
            Card.CardColor.RED,
            Card.Shading.STRIPED
        )
        val card3 = Card(
            Card.Number.THREE,
            Card.Shape.DIAMOND,
            Card.CardColor.RED,
            Card.Shading.OPEN
        )

        assertTrue(setFinder.isValidSet(card1, card2, card3))
    }

    @Test
    fun testInvalidSet_twoSameOneDifferent() {
        // Invalid: two cards have same number, one different
        val card1 = Card(
            Card.Number.ONE,
            Card.Shape.DIAMOND,
            Card.CardColor.RED,
            Card.Shading.SOLID
        )
        val card2 = Card(
            Card.Number.ONE,
            Card.Shape.OVAL,
            Card.CardColor.GREEN,
            Card.Shading.STRIPED
        )
        val card3 = Card(
            Card.Number.TWO,
            Card.Shape.SQUIGGLE,
            Card.CardColor.PURPLE,
            Card.Shading.OPEN
        )

        assertFalse(setFinder.isValidSet(card1, card2, card3))
    }

    @Test
    fun testInvalidSet_partialMatch() {
        // Invalid: some attributes don't follow the rule
        val card1 = Card(
            Card.Number.ONE,
            Card.Shape.DIAMOND,
            Card.CardColor.RED,
            Card.Shading.SOLID
        )
        val card2 = Card(
            Card.Number.TWO,
            Card.Shape.DIAMOND,
            Card.CardColor.GREEN,
            Card.Shading.SOLID
        )
        val card3 = Card(
            Card.Number.THREE,
            Card.Shape.OVAL,
            Card.CardColor.PURPLE,
            Card.Shading.OPEN
        )

        assertFalse(setFinder.isValidSet(card1, card2, card3))
    }

    @Test
    fun testFindAllSets_noSets() {
        // Cards that don't form any sets
        val cards = listOf(
            Card(Card.Number.ONE, Card.Shape.DIAMOND, Card.CardColor.RED, Card.Shading.SOLID),
            Card(Card.Number.ONE, Card.Shape.OVAL, Card.CardColor.GREEN, Card.Shading.SOLID),
            Card(Card.Number.TWO, Card.Shape.DIAMOND, Card.CardColor.RED, Card.Shading.STRIPED)
        )

        val sets = setFinder.findAllSets(cards)
        assertEquals(0, sets.size)
    }

    @Test
    fun testFindAllSets_oneSet() {
        // Exactly three cards that form one set
        val cards = listOf(
            Card(Card.Number.ONE, Card.Shape.DIAMOND, Card.CardColor.RED, Card.Shading.SOLID),
            Card(Card.Number.TWO, Card.Shape.OVAL, Card.CardColor.GREEN, Card.Shading.STRIPED),
            Card(Card.Number.THREE, Card.Shape.SQUIGGLE, Card.CardColor.PURPLE, Card.Shading.OPEN)
        )

        val sets = setFinder.findAllSets(cards)
        assertEquals(1, sets.size)
    }

    @Test
    fun testFindAllSets_multipleSets() {
        // Cards that form multiple sets
        val cards = listOf(
            // Set 1: all same except number
            Card(Card.Number.ONE, Card.Shape.DIAMOND, Card.CardColor.RED, Card.Shading.SOLID),
            Card(Card.Number.TWO, Card.Shape.DIAMOND, Card.CardColor.RED, Card.Shading.SOLID),
            Card(Card.Number.THREE, Card.Shape.DIAMOND, Card.CardColor.RED, Card.Shading.SOLID),
            
            // Set 2: all different
            Card(Card.Number.ONE, Card.Shape.OVAL, Card.CardColor.GREEN, Card.Shading.STRIPED),
            Card(Card.Number.TWO, Card.Shape.SQUIGGLE, Card.CardColor.PURPLE, Card.Shading.OPEN),
            Card(Card.Number.THREE, Card.Shape.DIAMOND, Card.CardColor.RED, Card.Shading.SOLID)
        )

        val sets = setFinder.findAllSets(cards)
        assertTrue(sets.size >= 2)
    }

    @Test
    fun testFindAllSets_emptyList() {
        val cards = emptyList<Card>()
        val sets = setFinder.findAllSets(cards)
        assertEquals(0, sets.size)
    }

    @Test
    fun testFindAllSets_twoCards() {
        // Not enough cards to form a set
        val cards = listOf(
            Card(Card.Number.ONE, Card.Shape.DIAMOND, Card.CardColor.RED, Card.Shading.SOLID),
            Card(Card.Number.TWO, Card.Shape.OVAL, Card.CardColor.GREEN, Card.Shading.STRIPED)
        )

        val sets = setFinder.findAllSets(cards)
        assertEquals(0, sets.size)
    }

    @Test
    fun testValidSet_realExample1() {
        // Real Set game example
        val card1 = Card(Card.Number.ONE, Card.Shape.DIAMOND, Card.CardColor.RED, Card.Shading.SOLID)
        val card2 = Card(Card.Number.TWO, Card.Shape.DIAMOND, Card.CardColor.RED, Card.Shading.SOLID)
        val card3 = Card(Card.Number.THREE, Card.Shape.DIAMOND, Card.CardColor.RED, Card.Shading.SOLID)

        assertTrue(setFinder.isValidSet(card1, card2, card3))
    }

    @Test
    fun testValidSet_realExample2() {
        // Real Set game example - all different
        val card1 = Card(Card.Number.ONE, Card.Shape.DIAMOND, Card.CardColor.RED, Card.Shading.OPEN)
        val card2 = Card(Card.Number.TWO, Card.Shape.OVAL, Card.CardColor.GREEN, Card.Shading.STRIPED)
        val card3 = Card(Card.Number.THREE, Card.Shape.SQUIGGLE, Card.CardColor.PURPLE, Card.Shading.SOLID)

        assertTrue(setFinder.isValidSet(card1, card2, card3))
    }

    @Test
    fun testInvalidSet_realExample() {
        // Real Set game example that's NOT a valid set
        val card1 = Card(Card.Number.ONE, Card.Shape.DIAMOND, Card.CardColor.RED, Card.Shading.SOLID)
        val card2 = Card(Card.Number.ONE, Card.Shape.OVAL, Card.CardColor.RED, Card.Shading.STRIPED)
        val card3 = Card(Card.Number.THREE, Card.Shape.SQUIGGLE, Card.CardColor.PURPLE, Card.Shading.OPEN)

        assertFalse(setFinder.isValidSet(card1, card2, card3))
    }

    @Test
    fun testValidSet_allSameExceptNumber() {
        // Cards with all attributes the same except number - should be valid
        val card1 = Card(
            Card.Number.ONE,
            Card.Shape.DIAMOND,
            Card.CardColor.RED,
            Card.Shading.SOLID
        )
        val card2 = Card(
            Card.Number.TWO,
            Card.Shape.DIAMOND,
            Card.CardColor.RED,
            Card.Shading.SOLID
        )
        val card3 = Card(
            Card.Number.THREE,
            Card.Shape.DIAMOND,
            Card.CardColor.RED,
            Card.Shading.SOLID
        )

        assertTrue(setFinder.isValidSet(card1, card2, card3))
    }

    @Test
    fun testValidSet_allSameExceptShape() {
        // Cards with all attributes the same except shape - should be valid
        val card1 = Card(
            Card.Number.TWO,
            Card.Shape.DIAMOND,
            Card.CardColor.GREEN,
            Card.Shading.STRIPED
        )
        val card2 = Card(
            Card.Number.TWO,
            Card.Shape.OVAL,
            Card.CardColor.GREEN,
            Card.Shading.STRIPED
        )
        val card3 = Card(
            Card.Number.TWO,
            Card.Shape.SQUIGGLE,
            Card.CardColor.GREEN,
            Card.Shading.STRIPED
        )

        assertTrue(setFinder.isValidSet(card1, card2, card3))
    }

    @Test
    fun testValidSet_allSameExceptColor() {
        // Cards with all attributes the same except color - should be valid
        val card1 = Card(
            Card.Number.THREE,
            Card.Shape.OVAL,
            Card.CardColor.RED,
            Card.Shading.OPEN
        )
        val card2 = Card(
            Card.Number.THREE,
            Card.Shape.OVAL,
            Card.CardColor.GREEN,
            Card.Shading.OPEN
        )
        val card3 = Card(
            Card.Number.THREE,
            Card.Shape.OVAL,
            Card.CardColor.PURPLE,
            Card.Shading.OPEN
        )

        assertTrue(setFinder.isValidSet(card1, card2, card3))
    }

    @Test
    fun testValidSet_allSameExceptShading() {
        // Cards with all attributes the same except shading - should be valid
        val card1 = Card(
            Card.Number.ONE,
            Card.Shape.SQUIGGLE,
            Card.CardColor.PURPLE,
            Card.Shading.SOLID
        )
        val card2 = Card(
            Card.Number.ONE,
            Card.Shape.SQUIGGLE,
            Card.CardColor.PURPLE,
            Card.Shading.STRIPED
        )
        val card3 = Card(
            Card.Number.ONE,
            Card.Shape.SQUIGGLE,
            Card.CardColor.PURPLE,
            Card.Shading.OPEN
        )

        assertTrue(setFinder.isValidSet(card1, card2, card3))
    }

    @Test
    fun testDiagnosticLogging_identicalCards() {
        // Test that diagnostic logging works for identical cards
        val logMessages = mutableListOf<String>()
        val logger = object : DiagnosticLogger {
            override fun log(message: String) {
                logMessages.add(message)
            }
            override fun logSection(title: String) {}
            override fun clear() {}
        }
        
        val finder = SetFinder(logger)
        val card = Card(
            Card.Number.ONE,
            Card.Shape.DIAMOND,
            Card.CardColor.RED,
            Card.Shading.SOLID
        )
        
        finder.isValidSet(card, card, card)
        
        assertTrue(logMessages.any { it.contains("identical") && it.contains("differ in at least one dimension") })
    }
}
