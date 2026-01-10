package com.example.setsolver

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs

/**
 * Detects and recognizes Set cards in an image
 */
class CardDetector {
    
    companion object {
        private const val TAG = "CardDetector"
        private const val MIN_CARD_AREA = 5000.0
        private const val MAX_CARD_AREA = 500000.0
    }

    /**
     * Detects cards in an image and returns their locations and attributes
     */
    fun detectCards(bitmap: Bitmap): List<Card> {
        try {
            // Convert bitmap to OpenCV Mat
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            
            // Convert to grayscale
            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)
            
            // Apply Gaussian blur to reduce noise
            val blurred = Mat()
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
            
            // Apply adaptive threshold
            val threshold = Mat()
            Imgproc.adaptiveThreshold(
                blurred, threshold, 255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV, 11, 2.0
            )
            
            // Find contours
            val contours = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(
                threshold, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
            )
            
            val cards = mutableListOf<Card>()
            
            // Process each contour to find cards
            for (contour in contours) {
                val area = Imgproc.contourArea(contour)
                
                if (area in MIN_CARD_AREA..MAX_CARD_AREA) {
                    // Approximate the contour to a polygon
                    val curve = MatOfPoint2f(*contour.toArray())
                    val approx = MatOfPoint2f()
                    val peri = Imgproc.arcLength(curve, true)
                    Imgproc.approxPolyDP(curve, approx, 0.02 * peri, true)
                    
                    // If it's a quadrilateral (4 vertices), it might be a card
                    if (approx.total() == 4L) {
                        val rect = Imgproc.boundingRect(contour)
                        
                        // Extract the card region
                        val cardRegion = mat.submat(rect)
                        
                        // Recognize the card attributes
                        val card = recognizeCard(cardRegion, rect)
                        if (card != null) {
                            cards.add(card)
                        }
                        
                        cardRegion.release()
                    }
                    
                    approx.release()
                    curve.release()
                }
                
                contour.release()
            }
            
            // Clean up
            mat.release()
            gray.release()
            blurred.release()
            threshold.release()
            hierarchy.release()
            
            Log.d(TAG, "Detected ${cards.size} cards")
            return cards
            
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting cards", e)
            return emptyList()
        }
    }

    /**
     * Recognizes card attributes from a card region
     * This is a simplified implementation using basic heuristics
     */
    private fun recognizeCard(cardRegion: Mat, rect: Rect): Card? {
        try {
            // Convert to bitmap for color analysis
            val bitmap = Bitmap.createBitmap(cardRegion.cols(), cardRegion.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(cardRegion, bitmap)
            
            // Analyze the card to determine its attributes
            val number = detectNumber(cardRegion)
            val shape = detectShape(cardRegion)
            val color = detectColor(bitmap)
            val shading = detectShading(cardRegion)
            
            return Card(
                number = number,
                shape = shape,
                color = color,
                shading = shading,
                x = rect.x.toFloat(),
                y = rect.y.toFloat(),
                width = rect.width.toFloat(),
                height = rect.height.toFloat()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error recognizing card", e)
            return null
        }
    }

    /**
     * Detects the number of symbols on the card
     */
    private fun detectNumber(cardRegion: Mat): Card.Number {
        // Convert to grayscale and threshold
        val gray = Mat()
        Imgproc.cvtColor(cardRegion, gray, Imgproc.COLOR_RGB2GRAY)
        
        val threshold = Mat()
        Imgproc.threshold(gray, threshold, 128.0, 255.0, Imgproc.THRESH_BINARY_INV)
        
        // Find contours of symbols
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            threshold, contours, hierarchy,
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
        )
        
        // Filter contours by area to count symbols
        val symbolContours = contours.filter { 
            val area = Imgproc.contourArea(it)
            area > 100.0 && area < cardRegion.width() * cardRegion.height() * 0.8
        }
        
        val count = symbolContours.size.coerceIn(1, 3)
        
        gray.release()
        threshold.release()
        hierarchy.release()
        contours.forEach { it.release() }
        
        return when (count) {
            1 -> Card.Number.ONE
            2 -> Card.Number.TWO
            else -> Card.Number.THREE
        }
    }

    /**
     * Detects the shape of symbols on the card
     * This is a simplified implementation
     */
    private fun detectShape(cardRegion: Mat): Card.Shape {
        // For now, use a simple heuristic based on aspect ratio
        // In a real implementation, this would use more sophisticated shape analysis
        val gray = Mat()
        Imgproc.cvtColor(cardRegion, gray, Imgproc.COLOR_RGB2GRAY)
        
        val threshold = Mat()
        Imgproc.threshold(gray, threshold, 128.0, 255.0, Imgproc.THRESH_BINARY_INV)
        
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            threshold, contours, hierarchy,
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
        )
        
        var shape = Card.Shape.OVAL // Default
        
        if (contours.isNotEmpty()) {
            // Find the largest contour (likely a symbol)
            val largestContour = contours.maxByOrNull { Imgproc.contourArea(it) }
            
            if (largestContour != null) {
                val rect = Imgproc.boundingRect(largestContour)
                val aspectRatio = rect.width.toDouble() / rect.height.toDouble()
                
                // Simple heuristic for shape detection
                shape = when {
                    aspectRatio > 1.5 -> Card.Shape.OVAL
                    aspectRatio < 0.7 -> Card.Shape.DIAMOND
                    else -> Card.Shape.SQUIGGLE
                }
            }
        }
        
        gray.release()
        threshold.release()
        hierarchy.release()
        contours.forEach { it.release() }
        
        return shape
    }

    /**
     * Detects the color of symbols on the card
     */
    private fun detectColor(bitmap: Bitmap): Card.CardColor {
        var redCount = 0
        var greenCount = 0
        var purpleCount = 0
        
        // Sample pixels to determine dominant color
        val sampleSize = 10
        for (x in 0 until bitmap.width step sampleSize) {
            for (y in 0 until bitmap.height step sampleSize) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                
                // Skip white/black pixels
                if (r > 200 && g > 200 && b > 200) continue
                if (r < 50 && g < 50 && b < 50) continue
                
                // Classify color
                when {
                    r > g && r > b -> redCount++
                    g > r && g > b -> greenCount++
                    b > g || (r > 100 && b > 100) -> purpleCount++
                }
            }
        }
        
        return when {
            redCount > greenCount && redCount > purpleCount -> Card.CardColor.RED
            greenCount > redCount && greenCount > purpleCount -> Card.CardColor.GREEN
            else -> Card.CardColor.PURPLE
        }
    }

    /**
     * Detects the shading of symbols on the card
     */
    private fun detectShading(cardRegion: Mat): Card.Shading {
        // Convert to grayscale
        val gray = Mat()
        Imgproc.cvtColor(cardRegion, gray, Imgproc.COLOR_RGB2GRAY)
        
        val threshold = Mat()
        Imgproc.threshold(gray, threshold, 128.0, 255.0, Imgproc.THRESH_BINARY_INV)
        
        // Calculate the ratio of filled pixels
        val nonZero = Core.countNonZero(threshold)
        val total = threshold.rows() * threshold.cols()
        val fillRatio = nonZero.toDouble() / total
        
        gray.release()
        threshold.release()
        
        // Classify shading based on fill ratio
        return when {
            fillRatio > 0.3 -> Card.Shading.SOLID
            fillRatio > 0.1 -> Card.Shading.STRIPED
            else -> Card.Shading.OPEN
        }
    }
}
