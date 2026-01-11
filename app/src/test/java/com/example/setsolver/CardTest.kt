package com.example.setsolver

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for the Card data class
 */
class CardTest {

    @Test
    fun testCardCreation() {
        val card = Card(
            Card.Number.ONE,
            Card.Shape.DIAMOND,
            Card.CardColor.RED,
            Card.Shading.SOLID
        )

        assertEquals(Card.Number.ONE, card.number)
        assertEquals(Card.Shape.DIAMOND, card.shape)
        assertEquals(Card.CardColor.RED, card.color)
        assertEquals(Card.Shading.SOLID, card.shading)
        assertEquals(0f, card.x, 0.01f)
        assertEquals(0f, card.y, 0.01f)
        assertEquals(0f, card.width, 0.01f)
        assertEquals(0f, card.height, 0.01f)
        assertEquals(0f, card.rotation, 0.01f)
    }

    @Test
    fun testCardWithPosition() {
        val card = Card(
            Card.Number.TWO,
            Card.Shape.OVAL,
            Card.CardColor.GREEN,
            Card.Shading.STRIPED,
            x = 100f,
            y = 200f,
            width = 300f,
            height = 400f
        )

        assertEquals(Card.Number.TWO, card.number)
        assertEquals(Card.Shape.OVAL, card.shape)
        assertEquals(Card.CardColor.GREEN, card.color)
        assertEquals(Card.Shading.STRIPED, card.shading)
        assertEquals(100f, card.x, 0.01f)
        assertEquals(200f, card.y, 0.01f)
        assertEquals(300f, card.width, 0.01f)
        assertEquals(400f, card.height, 0.01f)
        assertEquals(0f, card.rotation, 0.01f)
    }

    @Test
    fun testCardEquality() {
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

        assertEquals(card1, card2)
    }

    @Test
    fun testCardInequality() {
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

        assertNotEquals(card1, card2)
    }

    @Test
    fun testNumberEnum() {
        assertEquals(1, Card.Number.ONE.count)
        assertEquals(2, Card.Number.TWO.count)
        assertEquals(3, Card.Number.THREE.count)
    }

    @Test
    fun testAllEnumValues() {
        // Ensure all enum values exist
        assertEquals(3, Card.Number.values().size)
        assertEquals(3, Card.Shape.values().size)
        assertEquals(3, Card.CardColor.values().size)
        assertEquals(3, Card.Shading.values().size)
    }

    @Test
    fun testCardCopy() {
        val card1 = Card(
            Card.Number.ONE,
            Card.Shape.DIAMOND,
            Card.CardColor.RED,
            Card.Shading.SOLID,
            x = 10f,
            y = 20f
        )

        val card2 = card1.copy(x = 30f)

        assertEquals(Card.Number.ONE, card2.number)
        assertEquals(Card.Shape.DIAMOND, card2.shape)
        assertEquals(Card.CardColor.RED, card2.color)
        assertEquals(Card.Shading.SOLID, card2.shading)
        assertEquals(30f, card2.x, 0.01f)
        assertEquals(20f, card2.y, 0.01f)
    }

    @Test
    fun testCardWithRotation() {
        val card = Card(
            Card.Number.THREE,
            Card.Shape.SQUIGGLE,
            Card.CardColor.PURPLE,
            Card.Shading.OPEN,
            x = 50f,
            y = 60f,
            width = 100f,
            height = 150f,
            rotation = 45.0f
        )

        assertEquals(Card.Number.THREE, card.number)
        assertEquals(Card.Shape.SQUIGGLE, card.shape)
        assertEquals(Card.CardColor.PURPLE, card.color)
        assertEquals(Card.Shading.OPEN, card.shading)
        assertEquals(50f, card.x, 0.01f)
        assertEquals(60f, card.y, 0.01f)
        assertEquals(100f, card.width, 0.01f)
        assertEquals(150f, card.height, 0.01f)
        assertEquals(45.0f, card.rotation, 0.01f)
    }

    @Test
    fun testCardRotationCopy() {
        val card1 = Card(
            Card.Number.TWO,
            Card.Shape.OVAL,
            Card.CardColor.GREEN,
            Card.Shading.STRIPED,
            rotation = 30f
        )

        val card2 = card1.copy(rotation = 90f)

        assertEquals(Card.Number.TWO, card2.number)
        assertEquals(Card.Shape.OVAL, card2.shape)
        assertEquals(Card.CardColor.GREEN, card2.color)
        assertEquals(Card.Shading.STRIPED, card2.shading)
        assertEquals(90f, card2.rotation, 0.01f)
    }
}
