package com.example.setsolver

/**
 * Represents a Set card with its four attributes
 */
data class Card(
    val number: Number,
    val shape: Shape,
    val color: CardColor,
    val shading: Shading,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
) {
    enum class Number(val count: Int) {
        ONE(1), TWO(2), THREE(3)
    }

    enum class Shape {
        DIAMOND, OVAL, SQUIGGLE
    }

    enum class CardColor {
        RED, GREEN, PURPLE
    }

    enum class Shading {
        SOLID, STRIPED, OPEN
    }
}
