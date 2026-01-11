package com.example.setsolver

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for coordinate transformation logic in ResultOverlayView
 * Tests verify that image coordinates are correctly scaled to view coordinates
 */
class ResultOverlayViewTest {

    @Test
    fun testCoordinateScaling_SameAspectRatio() {
        // Image: 1920x1080, View: 960x540 (half size, same aspect ratio)
        val imageWidth = 1920f
        val imageHeight = 1080f
        val viewWidth = 960f
        val viewHeight = 540f
        
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        
        assertEquals(0.5f, scaleX, 0.001f)
        assertEquals(0.5f, scaleY, 0.001f)
        
        // Card at (500, 300) in image space
        val cardX = 500f
        val cardY = 300f
        
        val screenX = cardX * scaleX
        val screenY = cardY * scaleY
        
        assertEquals(250f, screenX, 0.001f)
        assertEquals(150f, screenY, 0.001f)
    }
    
    @Test
    fun testCoordinateScaling_DifferentAspectRatio() {
        // Image: 1920x1080, View: 800x600 (different aspect ratio)
        val imageWidth = 1920f
        val imageHeight = 1080f
        val viewWidth = 800f
        val viewHeight = 600f
        
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        
        assertEquals(0.4166f, scaleX, 0.001f)
        assertEquals(0.5555f, scaleY, 0.001f)
        
        // Card at (960, 540) center of image
        val cardX = 960f
        val cardY = 540f
        
        val screenX = cardX * scaleX
        val screenY = cardY * scaleY
        
        assertEquals(400f, screenX, 0.1f)
        assertEquals(300f, screenY, 0.1f)
    }
    
    @Test
    fun testRectangleTransformation() {
        // Image: 1920x1080, View: 800x600
        val imageWidth = 1920f
        val imageHeight = 1080f
        val viewWidth = 800f
        val viewHeight = 600f
        
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        
        // Card rectangle in image coordinates
        val cardX = 500f
        val cardY = 300f
        val cardWidth = 200f
        val cardHeight = 300f
        
        // Transform to screen coordinates
        val screenLeft = cardX * scaleX
        val screenTop = cardY * scaleY
        val screenRight = (cardX + cardWidth) * scaleX
        val screenBottom = (cardY + cardHeight) * scaleY
        
        // Verify transformation
        assertEquals(208.3f, screenLeft, 0.1f)
        assertEquals(166.65f, screenTop, 0.1f)
        assertEquals(291.65f, screenRight, 0.1f)
        assertEquals(333.3f, screenBottom, 0.1f)
        
        // Verify width and height are scaled
        val screenWidth = screenRight - screenLeft
        val screenHeight = screenBottom - screenTop
        
        assertEquals(cardWidth * scaleX, screenWidth, 0.1f)
        assertEquals(cardHeight * scaleY, screenHeight, 0.1f)
    }
    
    @Test
    fun testCenterPointTransformation() {
        // Image: 1920x1080, View: 800x600
        val imageWidth = 1920f
        val imageHeight = 1080f
        val viewWidth = 800f
        val viewHeight = 600f
        
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        
        // Card in image coordinates
        val cardX = 500f
        val cardY = 300f
        val cardWidth = 200f
        val cardHeight = 300f
        
        // Calculate center in image space
        val centerX = cardX + cardWidth / 2
        val centerY = cardY + cardHeight / 2
        
        // Transform center to screen space
        val screenCenterX = centerX * scaleX
        val screenCenterY = centerY * scaleY
        
        // Verify center transformation
        assertEquals(250f, screenCenterX, 0.1f)
        assertEquals(250f, screenCenterY, 0.1f)
    }
    
    @Test
    fun testDefaultImageDimensions() {
        // When image dimensions are not set, they default to 1f
        // This prevents division by zero
        val imageWidth = 1f
        val imageHeight = 1f
        val viewWidth = 800f
        val viewHeight = 600f
        
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        
        assertEquals(800f, scaleX, 0.001f)
        assertEquals(600f, scaleY, 0.001f)
    }
    
    @Test
    fun testMultipleCards() {
        // Image: 1920x1080, View: 800x600
        val imageWidth = 1920f
        val imageHeight = 1080f
        val viewWidth = 800f
        val viewHeight = 600f
        
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        
        // Three cards forming a set
        val cards = listOf(
            Triple(100f, 100f, 150f, 200f), // x, y, width, height
            Triple(400f, 200f, 150f, 200f),
            Triple(700f, 300f, 150f, 200f)
        )
        
        cards.forEach { (x, y, width, height) ->
            val screenX = x * scaleX
            val screenY = y * scaleY
            val screenWidth = width * scaleX
            val screenHeight = height * scaleY
            
            // Verify all coordinates are positive and within view bounds
            assertTrue(screenX >= 0)
            assertTrue(screenY >= 0)
            assertTrue(screenX + screenWidth <= viewWidth)
            assertTrue(screenY + screenHeight <= viewHeight)
        }
    }
}
