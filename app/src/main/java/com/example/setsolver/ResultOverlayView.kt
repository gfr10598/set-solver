package com.example.setsolver

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Custom view to overlay set highlights on the camera preview
 */
class ResultOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paints = mutableListOf<Paint>()
    private var sets: List<Triple<Card, Card, Card>> = emptyList()
    private var allCards: List<Card> = emptyList()
    private var highlightedSetIndex: Int? = null
    
    private var imageWidth: Float = 1f
    private var imageHeight: Float = 1f
    
    private val cardBoundaryPaint = Paint().apply {
        color = context.getColor(R.color.card_boundary)
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    
    private val cardDetectedPaint = Paint().apply {
        color = context.getColor(R.color.card_detected)
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }
    
    private val greyOverlayPaint = Paint().apply {
        color = 0x99808080.toInt() // Semi-transparent grey
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    init {
        // Create paints for different set highlights
        val colors = listOf(
            context.getColor(R.color.set_highlight_1),
            context.getColor(R.color.set_highlight_2),
            context.getColor(R.color.set_highlight_3),
            context.getColor(R.color.set_highlight_4)
        )
        
        colors.forEach { color ->
            paints.add(Paint().apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = 8f
                isAntiAlias = true
            })
        }
    }

    /**
     * Sets the dimensions of the source image for coordinate transformation
     * This must be called whenever a new image is processed
     */
    fun setImageDimensions(width: Int, height: Int) {
        imageWidth = width.toFloat()
        imageHeight = height.toFloat()
        invalidate()
    }

    /**
     * Updates the view with detected sets
     */
    fun setSets(newSets: List<Triple<Card, Card, Card>>) {
        sets = newSets
        // Extract all unique cards from sets
        val cardsInSets = mutableListOf<Card>()
        newSets.forEach {
            cardsInSets.add(it.first)
            cardsInSets.add(it.second)
            cardsInSets.add(it.third)
        }
        allCards = cardsInSets.distinct()
        invalidate()
    }
    
    /**
     * Updates the view with all detected cards
     */
    fun setCards(cards: List<Card>) {
        allCards = cards
        invalidate()
    }

    /**
     * Clears all highlights
     */
    fun clear() {
        sets = emptyList()
        allCards = emptyList()
        highlightedSetIndex = null
        invalidate()
    }

    /**
     * Highlights a specific set by index, or clears highlighting if null
     */
    fun highlightSet(setIndex: Int?) {
        highlightedSetIndex = setIndex
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // First, draw all detected cards with a simple boundary
        if (sets.isEmpty() && allCards.isNotEmpty()) {
            allCards.forEach { card ->
                drawCardRect(canvas, card, cardDetectedPaint)
            }
        }
        
        // If a specific set is highlighted, grey out all other cards
        if (highlightedSetIndex != null && sets.isNotEmpty()) {
            val selectedIndex = highlightedSetIndex!! // Store in local variable
            val highlightedSet = sets.getOrNull(selectedIndex)
            if (highlightedSet != null) {
                // Get all cards from all sets
                val highlightedCards = setOf(
                    highlightedSet.first,
                    highlightedSet.second,
                    highlightedSet.third
                )
                
                // Grey out all cards that are not in the highlighted set
                sets.forEachIndexed { index, set ->
                    if (index != selectedIndex) {
                        drawGreyOverlay(canvas, set.first)
                        drawGreyOverlay(canvas, set.second)
                        drawGreyOverlay(canvas, set.third)
                    }
                }
                
                // Draw only the highlighted set with its color
                val paint = paints[selectedIndex % paints.size]
                drawCardRect(canvas, highlightedSet.first, paint)
                drawCardRect(canvas, highlightedSet.second, paint)
                drawCardRect(canvas, highlightedSet.third, paint)
                drawConnectingLines(canvas, highlightedSet, paint)
            }
        } else {
            // Draw rectangles around cards in each set (normal mode)
            sets.forEachIndexed { index, set ->
                val paint = paints[index % paints.size]
                
                // Draw rectangle for each card in the set
                drawCardRect(canvas, set.first, paint)
                drawCardRect(canvas, set.second, paint)
                drawCardRect(canvas, set.third, paint)
                
                // Draw connecting lines between cards in the set
                drawConnectingLines(canvas, set, paint)
            }
        }
    }

    private fun drawGreyOverlay(canvas: Canvas, card: Card) {
        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight
        
        val rect = RectF(
            card.x * scaleX,
            card.y * scaleY,
            (card.x + card.width) * scaleX,
            (card.y + card.height) * scaleY
        )
        canvas.drawRect(rect, greyOverlayPaint)
    }

    private fun drawCardRect(canvas: Canvas, card: Card, paint: Paint) {
        // Calculate scale factors to map image coordinates to view coordinates
        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight
        
        // Transform card coordinates from image space to screen space
        val rect = RectF(
            card.x * scaleX,
            card.y * scaleY,
            (card.x + card.width) * scaleX,
            (card.y + card.height) * scaleY
        )
        canvas.drawRect(rect, paint)
    }

    private fun drawConnectingLines(canvas: Canvas, set: Triple<Card, Card, Card>, paint: Paint) {
        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight
        
        // Transform card centers from image space to screen space
        val centerX1 = (set.first.x + set.first.width / 2) * scaleX
        val centerY1 = (set.first.y + set.first.height / 2) * scaleY
        val centerX2 = (set.second.x + set.second.width / 2) * scaleX
        val centerY2 = (set.second.y + set.second.height / 2) * scaleY
        val centerX3 = (set.third.x + set.third.width / 2) * scaleX
        val centerY3 = (set.third.y + set.third.height / 2) * scaleY
        
        // Draw thin lines connecting the centers
        val linePaint = Paint(paint).apply {
            strokeWidth = 3f
            alpha = 128
        }
        
        canvas.drawLine(centerX1, centerY1, centerX2, centerY2, linePaint)
        canvas.drawLine(centerX2, centerY2, centerX3, centerY3, linePaint)
        canvas.drawLine(centerX3, centerY3, centerX1, centerY1, linePaint)
    }
}
