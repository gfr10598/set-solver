package com.example.setsolver

/**
 * Finds all valid sets among a collection of cards
 */
class SetFinder {
    
    /**
     * Checks if three cards form a valid set
     * A set is valid if for each of the four attributes,
     * the values are either all the same or all different
     */
    fun isValidSet(card1: Card, card2: Card, card3: Card): Boolean {
        return isValidAttribute(card1.number, card2.number, card3.number) &&
               isValidAttribute(card1.shape, card2.shape, card3.shape) &&
               isValidAttribute(card1.color, card2.color, card3.color) &&
               isValidAttribute(card1.shading, card2.shading, card3.shading)
    }

    private fun <T> isValidAttribute(attr1: T, attr2: T, attr3: T): Boolean {
        return (attr1 == attr2 && attr2 == attr3) || 
               (attr1 != attr2 && attr2 != attr3 && attr1 != attr3)
    }

    /**
     * Finds all valid sets in a collection of cards
     */
    fun findAllSets(cards: List<Card>): List<Triple<Card, Card, Card>> {
        val sets = mutableListOf<Triple<Card, Card, Card>>()
        
        for (i in cards.indices) {
            for (j in i + 1 until cards.size) {
                for (k in j + 1 until cards.size) {
                    if (isValidSet(cards[i], cards[j], cards[k])) {
                        sets.add(Triple(cards[i], cards[j], cards[k]))
                    }
                }
            }
        }
        
        return sets
    }
}
