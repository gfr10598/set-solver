package com.example.setsolver

/**
 * Finds all valid sets among a collection of cards
 */
class SetFinder(private val diagnosticLogger: DiagnosticLogger = NullDiagnosticLogger()) {
    
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
        diagnosticLogger.logSection("Set Finding")
        diagnosticLogger.log("Total cards to analyze: ${cards.size}")
        
        if (cards.size < 3) {
            diagnosticLogger.log("Not enough cards to form a set (need at least 3)")
            return emptyList()
        }
        
        val totalCombinations = (cards.size * (cards.size - 1) * (cards.size - 2)) / 6
        diagnosticLogger.log("Possible 3-card combinations: $totalCombinations")
        
        val sets = mutableListOf<Triple<Card, Card, Card>>()
        var combinationsChecked = 0
        
        for (i in cards.indices) {
            for (j in i + 1 until cards.size) {
                for (k in j + 1 until cards.size) {
                    combinationsChecked++
                    val isValid = isValidSet(cards[i], cards[j], cards[k])
                    
                    if (isValid) {
                        sets.add(Triple(cards[i], cards[j], cards[k]))
                        diagnosticLogger.log("✓ Valid set found: Cards ${i+1}, ${j+1}, ${k+1}")
                        logSetDetails(cards[i], cards[j], cards[k], i+1, j+1, k+1)
                    }
                }
            }
        }
        
        diagnosticLogger.log("Combinations checked: $combinationsChecked")
        diagnosticLogger.log("Valid sets found: ${sets.size}")
        
        if (sets.isEmpty()) {
            diagnosticLogger.log("No valid sets found among the detected cards")
        }
        
        return sets
    }
    
    /**
     * Logs details about a set to help understand why it's valid
     */
    private fun logSetDetails(card1: Card, card2: Card, card3: Card, idx1: Int, idx2: Int, idx3: Int) {
        diagnosticLogger.log("  Card $idx1: ${card1.number.count} ${card1.color.name} ${card1.shape.name} ${card1.shading.name}")
        diagnosticLogger.log("  Card $idx2: ${card2.number.count} ${card2.color.name} ${card2.shape.name} ${card2.shading.name}")
        diagnosticLogger.log("  Card $idx3: ${card3.number.count} ${card3.color.name} ${card3.shape.name} ${card3.shading.name}")
        
        diagnosticLogger.log("  Number: ${getAttributeStatus(card1.number, card2.number, card3.number)}")
        diagnosticLogger.log("  Shape: ${getAttributeStatus(card1.shape, card2.shape, card3.shape)}")
        diagnosticLogger.log("  Color: ${getAttributeStatus(card1.color, card2.color, card3.color)}")
        diagnosticLogger.log("  Shading: ${getAttributeStatus(card1.shading, card2.shading, card3.shading)}")
    }
    
    /**
     * Returns a string describing the status of an attribute
     */
    private fun <T> getAttributeStatus(attr1: T, attr2: T, attr3: T): String {
        return if (attr1 == attr2 && attr2 == attr3) {
            "All same ✓"
        } else if (attr1 != attr2 && attr2 != attr3 && attr1 != attr3) {
            "All different ✓"
        } else {
            "Invalid (2 same, 1 different) ✗"
        }
    }
}
