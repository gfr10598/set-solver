package com.example.setsolver

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.PI

/**
 * Detects and recognizes Set cards in an image
 */
class CardDetector(private val diagnosticLogger: DiagnosticLogger = NullDiagnosticLogger()) {
    
    companion object {
        private const val TAG = "CardDetector"
        private const val MIN_CARD_AREA = 5000.0
        private const val MAX_CARD_AREA = 500000.0
        
        // Shape detection constants
        /** Epsilon factor for polygon approximation (as fraction of perimeter) */
        private const val POLYGON_APPROX_EPSILON = 0.02
        
        /** Angle tolerance in degrees for detecting parallel edges */
        private const val PARALLEL_THRESHOLD = 10.0
    }

    /**
     * Detects cards in an image and returns their locations and attributes
     */
    fun detectCards(bitmap: Bitmap): List<Card> {
        try {
            diagnosticLogger.logSection("Image Capture")
            diagnosticLogger.log("Image dimensions: ${bitmap.width}x${bitmap.height}")
            diagnosticLogger.log("Image format: ${bitmap.config}")
            
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
            
            diagnosticLogger.logSection("Card Detection")
            
            // Find contours
            val contours = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(
                threshold, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
            )
            
            diagnosticLogger.log("Total contours found: ${contours.size}")
            
            val cards = mutableListOf<Card>()
            var filteredCount = 0
            var quadrilateralCount = 0
            
            // Process each contour to find cards
            for (contour in contours) {
                val area = Imgproc.contourArea(contour)
                
                if (area in MIN_CARD_AREA..MAX_CARD_AREA) {
                    filteredCount++
                    
                    // Approximate the contour to a polygon
                    val curve = MatOfPoint2f(*contour.toArray())
                    val approx = MatOfPoint2f()
                    val peri = Imgproc.arcLength(curve, true)
                    Imgproc.approxPolyDP(curve, approx, 0.02 * peri, true)
                    
                    // If it's a quadrilateral (4 vertices), it might be a card
                    if (approx.total() == 4L) {
                        quadrilateralCount++
                        
                        // Get the minimum area rectangle to determine rotation
                        // Note: OpenCV's minAreaRect returns an angle between -90 and 0 degrees
                        // representing the rotation of the rectangle's longer side relative to horizontal
                        val rotatedRect = Imgproc.minAreaRect(curve)
                        val angle = rotatedRect.angle.toFloat()
                        
                        val rect = Imgproc.boundingRect(contour)
                        
                        diagnosticLogger.log("Card candidate ${cards.size + 1}: area=${"%.0f".format(area)}, rect=(${rect.x},${rect.y},${rect.width},${rect.height})")
                        
                        // Extract the card region
                        val cardRegion = mat.submat(rect)
                        
                        // Recognize the card attributes, passing the rotation angle
                        val card = recognizeCard(cardRegion, rect, angle, cards.size + 1)
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
            
            diagnosticLogger.log("Contours passing size filter: $filteredCount")
            diagnosticLogger.log("Quadrilateral contours: $quadrilateralCount")
            diagnosticLogger.log("Cards successfully detected: ${cards.size}")
            
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
            diagnosticLogger.log("ERROR: ${e.message}")
            return emptyList()
        }
    }

    /**
     * Recognizes card attributes from a card region
     * This is a simplified implementation using basic heuristics
     */
    private fun recognizeCard(cardRegion: Mat, rect: Rect, rotation: Float, cardIndex: Int): Card? {
        try {
            diagnosticLogger.logSection("Feature Extraction - Card $cardIndex")
            
            // Convert to bitmap for color analysis
            val bitmap = Bitmap.createBitmap(cardRegion.cols(), cardRegion.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(cardRegion, bitmap)
            
            // Analyze the card to determine its attributes
            val number = detectNumber(cardRegion)
            diagnosticLogger.log("  Number: ${number.name} (${number.count})")
            
            val shape = detectShape(cardRegion)
            diagnosticLogger.log("  Shape: ${shape.name}")
            
            val color = detectColor(bitmap)
            diagnosticLogger.log("  Color: ${color.name}")
            
            val shading = detectShading(cardRegion)
            diagnosticLogger.log("  Shading: ${shading.name}")
            
            diagnosticLogger.log("  Bounding box: (${rect.x}, ${rect.y}, ${rect.width}, ${rect.height})")
            diagnosticLogger.log("  Rotation: ${"%.1f".format(rotation)}°")
            
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
            diagnosticLogger.log("  ERROR recognizing card: ${e.message}")
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
     * Detects the shape of symbols on the card using parallel edge analysis
     * - Diamond: 2 pairs of parallel edges (rhombus)
     * - Oval: 1 pair of parallel edges (elongated ellipse)
     * - Squiggle: 0 pairs of parallel edges (irregular/wavy)
     */
    private fun detectShape(cardRegion: Mat): Card.Shape {
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
        
        var shape = Card.Shape.SQUIGGLE // Default to squiggle
        
        if (contours.isNotEmpty()) {
            // Find the largest contour (likely a symbol)
            val largestContour = contours.maxByOrNull { Imgproc.contourArea(it) }
            
            if (largestContour != null) {
                // Approximate contour to polygon to get edges
                val curve = MatOfPoint2f(*largestContour.toArray())
                val approx = MatOfPoint2f()
                val perimeter = Imgproc.arcLength(curve, true)
                Imgproc.approxPolyDP(curve, approx, POLYGON_APPROX_EPSILON * perimeter, true)
                
                // Count parallel edge pairs
                val parallelPairs = countParallelEdgePairs(approx)
                
                // Classify based on parallel pairs
                shape = when (parallelPairs) {
                    2 -> Card.Shape.DIAMOND   // 2 pairs = rhombus/diamond
                    1 -> Card.Shape.OVAL      // 1 pair = elongated oval
                    else -> Card.Shape.SQUIGGLE  // 0 pairs = irregular squiggle
                }
                
                curve.release()
                approx.release()
            }
        }
        
        gray.release()
        threshold.release()
        hierarchy.release()
        contours.forEach { it.release() }
        
        return shape
    }

    /**
     * Counts the number of parallel edge pairs in a polygon contour
     * 
     * @param approx Approximated polygon contour
     * @return Number of parallel edge pairs found
     */
    private fun countParallelEdgePairs(approx: MatOfPoint2f): Int {
        val points = approx.toArray()
        if (points.size < 3) return 0
        
        // Extract edges (line segments between consecutive vertices)
        val edges = mutableListOf<Pair<Point, Point>>()
        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % points.size]
            edges.add(Pair(p1, p2))
        }
        
        // Count parallel pairs
        var pairCount = 0
        val used = mutableSetOf<Int>()
        
        for (i in edges.indices) {
            if (i in used) continue
            
            val angle1 = getEdgeAngle(edges[i])
            
            // Look for a parallel edge
            for (j in (i + 1) until edges.size) {
                if (j in used) continue
                
                val angle2 = getEdgeAngle(edges[j])
                
                // Normalize angle difference to 0-180 range
                var angleDiff = abs(angle1 - angle2)
                if (angleDiff > 180) angleDiff = 360 - angleDiff
                
                // Check if parallel: edges are parallel if they have the same angle (0°)
                // or opposite direction (180°). Both cases represent parallel lines.
                if (angleDiff < PARALLEL_THRESHOLD || 
                    abs(angleDiff - 180) < PARALLEL_THRESHOLD) {
                    pairCount++
                    used.add(i)
                    used.add(j)
                    break  // Found a pair for edge i, move to next
                }
            }
        }
        
        return pairCount
    }

    /**
     * Calculates the angle of an edge in degrees
     * 
     * @param edge Pair of points representing the edge
     * @return Angle in degrees (0-360)
     */
    private fun getEdgeAngle(edge: Pair<Point, Point>): Double {
        val dx = edge.second.x - edge.first.x
        val dy = edge.second.y - edge.first.y
        var angle = atan2(dy, dx) * 180.0 / PI
        
        // Normalize to 0-360 range
        if (angle < 0) angle += 360
        
        return angle
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
