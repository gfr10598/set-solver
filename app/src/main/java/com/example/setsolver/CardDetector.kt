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
                        // Get the minimum area rectangle to determine rotation
                        // Note: OpenCV's minAreaRect returns an angle between -90 and 0 degrees
                        // representing the rotation of the rectangle's longer side relative to horizontal
                        val rotatedRect = Imgproc.minAreaRect(curve)
                        val angle = rotatedRect.angle.toFloat()
                        
                        val rect = Imgproc.boundingRect(contour)
                        
                        // Extract the card region
                        val cardRegion = mat.submat(rect)
                        
                        // Recognize the card attributes, passing the rotation angle
                        val card = recognizeCard(cardRegion, rect, angle)
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
     * Normalizes brightness and contrast of a card region for robust feature detection
     * This makes color and shading detection less sensitive to lighting conditions
     */
    private fun normalizeCardRegion(cardRegion: Mat): Mat {
        val normalized = Mat()
        
        // Use CLAHE (Contrast Limited Adaptive Histogram Equalization)
        // This enhances local contrast while preventing over-amplification of noise
        val lab = Mat()
        Imgproc.cvtColor(cardRegion, lab, Imgproc.COLOR_RGB2Lab)
        
        // Split into L, a, b channels
        val channels = ArrayList<Mat>()
        Core.split(lab, channels)
        
        // Apply CLAHE to the L (lightness) channel
        val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
        clahe.apply(channels[0], channels[0])
        
        // Merge channels back
        Core.merge(channels, lab)
        
        // Convert back to RGB
        Imgproc.cvtColor(lab, normalized, Imgproc.COLOR_Lab2RGB)
        
        // Release resources
        lab.release()
        channels.forEach { it.release() }
        
        return normalized
    }

    /**
     * Recognizes card attributes from a card region
     * This is a simplified implementation using basic heuristics
     */
    private fun recognizeCard(cardRegion: Mat, rect: Rect, rotation: Float): Card? {
        try {
            // Normalize brightness and contrast
            val normalizedRegion = normalizeCardRegion(cardRegion)
            
            // Analyze the card to determine its attributes
            val number = detectNumber(normalizedRegion)
            val shape = detectShape(normalizedRegion)
            
            // Generate symbol mask for color detection
            val gray = Mat()
            Imgproc.cvtColor(normalizedRegion, gray, Imgproc.COLOR_RGB2GRAY)
            val symbolMask = Mat()
            Imgproc.threshold(gray, symbolMask, 0.0, 255.0, 
                Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)
            gray.release()
            
            val color = detectColor(normalizedRegion, symbolMask)
            val shading = detectShading(normalizedRegion)
            
            // Clean up
            normalizedRegion.release()
            symbolMask.release()
            
            return Card(
                number = number,
                shape = shape,
                color = color,
                shading = shading,
                x = rect.x.toFloat(),
                y = rect.y.toFloat(),
                width = rect.width.toFloat(),
                height = rect.height.toFloat(),
                rotation = rotation
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
    private fun detectColor(cardRegion: Mat, symbolMask: Mat): Card.CardColor {
        var redCount = 0
        var greenCount = 0
        var purpleCount = 0
        
        // Convert to bitmap for pixel access
        val bitmap = Bitmap.createBitmap(cardRegion.cols(), cardRegion.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(cardRegion, bitmap)
        
        // Sample pixels with a step size for efficiency, but check mask for each
        val stepSize = 3
        for (x in 0 until cardRegion.cols() step stepSize) {
            for (y in 0 until cardRegion.rows() step stepSize) {
                // Check if this pixel is part of a symbol
                val maskValue = symbolMask.get(y, x)[0]
                if (maskValue == 0.0) continue  // Skip background pixels
                
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                
                // Classify color based on dominant channel
                when {
                    r > g + 20 && r > b + 20 -> redCount++
                    g > r + 20 && g > b + 20 -> greenCount++
                    b > r + 20 || (r > 80 && b > 80 && b > g) -> purpleCount++
                }
            }
        }
        
        // Clean up
        bitmap.recycle()
        
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
        
        // Use Otsu's method for adaptive thresholding
        val threshold = Mat()
        Imgproc.threshold(gray, threshold, 0.0, 255.0, 
            Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)
        
        // Calculate the ratio of filled pixels
        val nonZero = Core.countNonZero(threshold)
        val total = threshold.rows() * threshold.cols()
        val fillRatio = nonZero.toDouble() / total
        
        gray.release()
        threshold.release()
        
        // Adjusted thresholds based on normalized images
        return when {
            fillRatio > 0.35 -> Card.Shading.SOLID
            fillRatio > 0.15 -> Card.Shading.STRIPED
            else -> Card.Shading.OPEN
        }
    }
}
