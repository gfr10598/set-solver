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
import kotlin.math.sqrt

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

        // Grid detection constants
        private const val GRID_ROWS = 3
        private const val MIN_GRID_COLS = 4
        private const val MAX_GRID_COLS = 5
        private const val CARD_DIM_VARIANCE_THRESHOLD = 0.3 // 30% variance allowed
        
        // Hybrid detection constants
        private const val SEARCH_MARGIN = 0.15 // 15% expansion of grid cell for search region
        private const val CONTOUR_APPROX_EPSILON = 0.02 // 2% deviation allowed for polygon approximation
        
        // Color clustering constants
        private const val MAX_COLOR_CLUSTERS = 3
        private const val COLOR_KMEANS_ITERATIONS = 10
        private const val COLOR_KMEANS_ATTEMPTS = 3  // Number of attempts with different initial centers
        private const val COLOR_EPSILON = 1.0
        private const val DEFAULT_CLUSTER_VALUE = 128.0  // Default gray value for fallback cluster
        private const val COLOR_DETECTION_THRESHOLD = 200   // Threshold below which a pixel is considered colored (not white)
        
        // Shading detection thresholds (pixel ratio based)
        private const val SOLID_COLORED_RATIO = 0.4    // > 40% colored = SOLID
        private const val STRIPED_COLORED_RATIO = 0.15 // > 15% colored = STRIPED, else OPEN
        
        // Performance tuning
        private const val COLOR_SAMPLING_STEP = 3
    }
    
    // Store detected color clusters globally for all cards
    private var globalColorClusters: List<Triple<Double, Double, Double>> = emptyList()
    
    /**
     * Determines if a pixel is colored (not white/background)
     * A pixel is considered colored if all RGB channels are below the threshold
     */
    private fun isColoredPixel(r: Int, g: Int, b: Int): Boolean {
        return r < COLOR_DETECTION_THRESHOLD && 
               g < COLOR_DETECTION_THRESHOLD && 
               b < COLOR_DETECTION_THRESHOLD
    }
    
    /**
     * Determines if a pixel is colored (not white/background) - Float version
     */
    private fun isColoredPixel(r: Float, g: Float, b: Float): Boolean {
        return r < COLOR_DETECTION_THRESHOLD && 
               g < COLOR_DETECTION_THRESHOLD && 
               b < COLOR_DETECTION_THRESHOLD
    }

    /**
     * Detects cards in an image and returns their locations and attributes
     * Uses grid-based detection instead of contour detection
     */
    fun detectCards(bitmap: Bitmap): List<Card> {
        try {
            diagnosticLogger.logSection("Image Capture")
            diagnosticLogger.log("Image dimensions: ${bitmap.width}x${bitmap.height}")
            diagnosticLogger.log("Image format: ${bitmap.config}")
            
            // Convert bitmap to OpenCV Mat
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            
            diagnosticLogger.logSection("Grid-Based Card Detection")
            
            // Detect grid dimensions (4 or 5 columns)
            val numCols = detectGridColumns(mat)
            diagnosticLogger.log("Detected grid: $GRID_ROWS rows × $numCols columns")
            
            // Extract all card regions from the grid
            val cardRegions = mutableListOf<Pair<Triple<Mat, Rect, Float>, Int>>() // (Mat, Rect, rotation), cardIndex
            for (row in 0 until GRID_ROWS) {
                for (col in 0 until numCols) {
                    val cardIndex = row * numCols + col + 1
                    val (cardRegion, rect, rotation) = extractCardFromGrid(mat, row, col, numCols)
                    if (cardRegion != null && rect != null) {
                        cardRegions.add(Pair(Triple(cardRegion, rect, rotation), cardIndex))
                    }
                }
            }
            
            diagnosticLogger.log("Extracted ${cardRegions.size} card regions from grid")
            
            // Validate card dimensions (ensure all cards have similar size)
            val validatedRegions = validateCardDimensions(cardRegions)
            diagnosticLogger.log("${validatedRegions.size} cards passed dimension validation")
            
            // Build global color histogram from all cards
            globalColorClusters = buildGlobalColorClusters(validatedRegions.map { it.first.first })
            diagnosticLogger.log("Detected ${globalColorClusters.size} color cluster(s)")
            
            diagnosticLogger.logSection("Feature Extraction")
            
            // Recognize each card
            val cards = mutableListOf<Card>()
            for ((cardData, cardIndex) in validatedRegions) {
                val (cardRegion, rect, rotation) = cardData
                diagnosticLogger.log("Processing card $cardIndex at grid position")
                val card = recognizeCard(cardRegion, rect, rotation, cardIndex)
                if (card != null) {
                    cards.add(card)
                }
                cardRegion.release()
            }
            
            diagnosticLogger.log("Cards successfully detected: ${cards.size}")
            
            // Clean up
            mat.release()
            
            Log.d(TAG, "Detected ${cards.size} cards")
            return cards
            
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting cards", e)
            diagnosticLogger.log("ERROR: ${e.message}")
            return emptyList()
        }
    }

    /**
     * Detects whether the grid has 4 or 5 columns by analyzing the image
     * Returns the number of columns detected
     */
    private fun detectGridColumns(mat: Mat): Int {
        // Use aspect ratio to determine grid dimensions
        // For a 3-row × N-column grid, the image aspect ratio depends on card layout
        // Assuming cards are roughly square or slightly taller, a wider image suggests more columns
        val aspectRatio = mat.width().toDouble() / mat.height().toDouble()
        
        // Use threshold at 1.0 with tolerance
        // If aspect ratio is significantly higher than 1.0, it's likely 5 columns
        // If it's close to or below 1.0, it's likely 4 columns
        return if (aspectRatio > 1.0) MAX_GRID_COLS else MIN_GRID_COLS
    }
    
    /**
     * Extracts a card region from the grid at the specified position using hybrid grid + contour detection
     * @return Triple of (cardRegion Mat, bounding Rect, rotation angle) or (null, null, 0f) if extraction fails
     */
    private fun extractCardFromGrid(mat: Mat, row: Int, col: Int, numCols: Int): Triple<Mat?, Rect?, Float> {
        try {
            val imgWidth = mat.width()
            val imgHeight = mat.height()
            
            // Calculate card dimensions for the grid cell
            val cardWidth = imgWidth / numCols
            val cardHeight = imgHeight / GRID_ROWS
            
            // Calculate base grid position
            val baseX = col * cardWidth
            val baseY = row * cardHeight
            
            // Expand search region by SEARCH_MARGIN to allow for card displacement
            val marginX = (cardWidth * SEARCH_MARGIN).toInt()
            val marginY = (cardHeight * SEARCH_MARGIN).toInt()
            
            // Calculate expanded search region
            val searchX = (baseX - marginX).coerceAtLeast(0)
            val searchY = (baseY - marginY).coerceAtLeast(0)
            val searchW = (cardWidth + 2 * marginX).coerceAtMost(imgWidth - searchX)
            val searchH = (cardHeight + 2 * marginY).coerceAtMost(imgHeight - searchY)
            
            // Ensure bounds are valid
            if (searchX < 0 || searchY < 0 || searchX + searchW > imgWidth || searchY + searchH > imgHeight || searchW <= 0 || searchH <= 0) {
                return Triple(null, null, 0f)
            }
            
            val searchRect = Rect(searchX, searchY, searchW, searchH)
            val searchRegion = mat.submat(searchRect)
            
            // Try to find card contour within search region
            val contourResult = findCardContourInRegion(searchRegion)
            
            if (contourResult != null) {
                // Extract rotated card using the detected contour
                val (cardMat, actualRect, rotation) = extractRotatedCard(mat, contourResult, searchX, searchY)
                searchRegion.release()
                return Triple(cardMat, actualRect, rotation)
            } else {
                // Fallback to simple grid extraction with small margin to avoid edges
                val fallbackMarginX = (cardWidth * 0.05).toInt()
                val fallbackMarginY = (cardHeight * 0.05).toInt()
                
                val x = baseX + fallbackMarginX
                val y = baseY + fallbackMarginY
                val w = cardWidth - 2 * fallbackMarginX
                val h = cardHeight - 2 * fallbackMarginY
                
                if (x < 0 || y < 0 || x + w > imgWidth || y + h > imgHeight || w <= 0 || h <= 0) {
                    searchRegion.release()
                    return Triple(null, null, 0f)
                }
                
                val rect = Rect(x, y, w, h)
                val cardRegion = mat.submat(rect)
                searchRegion.release()
                
                return Triple(cardRegion, rect, 0f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting card from grid at ($row, $col)", e)
            return Triple(null, null, 0f)
        }
    }
    
    /**
     * Finds a card contour within a search region using contour detection
     * @param region Search region Mat
     * @return RotatedRect of the card if found, null otherwise
     */
    private fun findCardContourInRegion(region: Mat): RotatedRect? {
        try {
            // Convert to grayscale
            val gray = Mat()
            Imgproc.cvtColor(region, gray, Imgproc.COLOR_RGB2GRAY)
            
            // Apply Gaussian blur to reduce noise
            val blurred = Mat()
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
            
            // Apply adaptive thresholding
            val thresh = Mat()
            Imgproc.adaptiveThreshold(
                blurred, thresh, 255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV,
                11, 2.0
            )
            
            // Find contours
            val contours = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(
                thresh, contours, hierarchy,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
            )
            
            // Find the largest quadrilateral contour that matches card area
            var bestContour: MatOfPoint? = null
            var maxArea = 0.0
            
            for (contour in contours) {
                val area = Imgproc.contourArea(contour)
                
                // Check if area is within expected range for a card
                if (area < MIN_CARD_AREA || area > MAX_CARD_AREA) continue
                
                // Approximate contour to polygon
                val curve = MatOfPoint2f(*contour.toArray())
                val approx = MatOfPoint2f()
                val perimeter = Imgproc.arcLength(curve, true)
                Imgproc.approxPolyDP(curve, approx, CONTOUR_APPROX_EPSILON * perimeter, true)
                
                // Check if it's a quadrilateral (4 vertices)
                if (approx.rows() == 4 && area > maxArea) {
                    maxArea = area
                    bestContour = contour
                }
                
                curve.release()
                approx.release()
            }
            
            // Clean up
            gray.release()
            blurred.release()
            thresh.release()
            hierarchy.release()
            
            // If we found a good contour, get its rotated bounding box
            if (bestContour != null) {
                val points = MatOfPoint2f(*bestContour.toArray())
                val rotatedRect = Imgproc.minAreaRect(points)
                points.release()
                contours.forEach { it.release() }
                return rotatedRect
            }
            
            contours.forEach { it.release() }
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error finding card contour in region", e)
            return null
        }
    }
    
    /**
     * Extracts and de-rotates a card region from the image
     * @param mat Source image
     * @param rotatedRect Rotated rectangle defining the card boundaries
     * @param offsetX X offset of search region in source image
     * @param offsetY Y offset of search region in source image
     * @return Triple of (extracted card Mat, bounding Rect in source coordinates, rotation angle)
     */
    private fun extractRotatedCard(mat: Mat, rotatedRect: RotatedRect, offsetX: Int, offsetY: Int): Triple<Mat, Rect, Float> {
        try {
            // Get rotation angle
            var angle = rotatedRect.angle.toFloat()
            val size = rotatedRect.size
            
            // Adjust angle to ensure card is upright
            // OpenCV's minAreaRect returns angle in range [-90, 0]
            // We need to handle the card orientation properly
            var width = size.width
            var height = size.height
            
            if (width < height) {
                // Swap dimensions and adjust angle
                val temp = width
                width = height
                height = temp
                angle += 90f
            }
            
            // Normalize angle to [-180, 180] range
            if (angle > 180) angle -= 360
            if (angle < -180) angle += 360
            
            // Adjust center to source image coordinates
            val center = Point(
                rotatedRect.center.x + offsetX,
                rotatedRect.center.y + offsetY
            )
            
            // Get rotation matrix
            val rotMat = Imgproc.getRotationMatrix2D(center, angle.toDouble(), 1.0)
            
            // Rotate the entire image
            val rotated = Mat()
            Imgproc.warpAffine(mat, rotated, rotMat, mat.size())
            
            // Extract the upright card region
            val x = (center.x - width / 2.0).toInt().coerceAtLeast(0)
            val y = (center.y - height / 2.0).toInt().coerceAtLeast(0)
            val w = width.toInt().coerceAtMost(rotated.width() - x)
            val h = height.toInt().coerceAtMost(rotated.height() - y)
            
            if (w <= 0 || h <= 0) {
                rotated.release()
                rotMat.release()
                // Return a fallback
                return Triple(Mat(), Rect(offsetX, offsetY, 1, 1), 0f)
            }
            
            val rect = Rect(x, y, w, h)
            val cardRegion = rotated.submat(rect).clone()
            
            // Clean up
            rotated.release()
            rotMat.release()
            
            return Triple(cardRegion, rect, angle)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting rotated card", e)
            return Triple(Mat(), Rect(offsetX, offsetY, 1, 1), 0f)
        }
    }
    
    /**
     * Validates that all card regions have similar dimensions
     * Filters out outliers that differ significantly from the median
     */
    private fun validateCardDimensions(
        cardRegions: List<Pair<Triple<Mat, Rect, Float>, Int>>
    ): List<Pair<Triple<Mat, Rect, Float>, Int>> {
        if (cardRegions.isEmpty()) return emptyList()
        
        // Calculate median width and height
        val widths = cardRegions.map { it.first.second.width }.sorted()
        val heights = cardRegions.map { it.first.second.height }.sorted()
        
        val medianWidth = widths[widths.size / 2]
        val medianHeight = heights[heights.size / 2]
        
        // Filter cards that are within acceptable variance of median
        return cardRegions.filter { (cardData, _) ->
            val rect = cardData.second
            val widthRatio = rect.width.toDouble() / medianWidth
            val heightRatio = rect.height.toDouble() / medianHeight
            
            widthRatio > (1.0 - CARD_DIM_VARIANCE_THRESHOLD) &&
            widthRatio < (1.0 + CARD_DIM_VARIANCE_THRESHOLD) &&
            heightRatio > (1.0 - CARD_DIM_VARIANCE_THRESHOLD) &&
            heightRatio < (1.0 + CARD_DIM_VARIANCE_THRESHOLD)
        }
    }
    
    /**
     * Builds global color clusters from all card regions
     * Uses k-means clustering to identify 1-3 distinct colors
     */
    private fun buildGlobalColorClusters(cardRegions: List<Mat>): List<Triple<Double, Double, Double>> {
        // Collect all colored pixels from all cards
        val coloredPixels = mutableListOf<FloatArray>()
        
        for (cardRegion in cardRegions) {
            // Normalize the card region
            val normalized = normalizeCardRegion(cardRegion)
            
            // Generate symbol mask
            val gray = Mat()
            Imgproc.cvtColor(normalized, gray, Imgproc.COLOR_RGB2GRAY)
            val symbolMask = Mat()
            Imgproc.threshold(gray, symbolMask, 0.0, 255.0, 
                Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)
            
            // Convert to bitmap for pixel access
            val bitmap = Bitmap.createBitmap(normalized.cols(), normalized.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(normalized, bitmap)
            
            // Sample colored pixels
            for (x in 0 until normalized.cols() step COLOR_SAMPLING_STEP) {
                for (y in 0 until normalized.rows() step COLOR_SAMPLING_STEP) {
                    // Only consider pixels that are part of symbols (non-white)
                    val maskValue = symbolMask.get(y, x)[0]
                    if (maskValue > 0.0) {
                        val pixel = bitmap.getPixel(x, y)
                        val r = Color.red(pixel).toFloat()
                        val g = Color.green(pixel).toFloat()
                        val b = Color.blue(pixel).toFloat()
                        
                        // Keep colored pixels (non-white)
                        if (isColoredPixel(r, g, b)) {
                            coloredPixels.add(floatArrayOf(r, g, b))
                        }
                    }
                }
            }
            
            gray.release()
            symbolMask.release()
            bitmap.recycle()
            normalized.release()
        }
        
        if (coloredPixels.isEmpty()) {
            // No colored pixels found, return default cluster
            return listOf(Triple(DEFAULT_CLUSTER_VALUE, DEFAULT_CLUSTER_VALUE, DEFAULT_CLUSTER_VALUE))
        }
        
        // Perform k-means clustering to find color clusters
        return performKMeansClustering(coloredPixels)
    }
    
    /**
     * Performs k-means clustering on colored pixels to find distinct color clusters
     * Tries different k values (1-3) and selects the best based on cluster quality
     */
    private fun performKMeansClustering(
        pixels: List<FloatArray>
    ): List<Triple<Double, Double, Double>> {
        if (pixels.isEmpty()) return emptyList()
        
        // Convert to OpenCV Mat format
        val samples = Mat(pixels.size, 3, org.opencv.core.CvType.CV_32F)
        for (i in pixels.indices) {
            samples.put(i, 0, pixels[i][0].toDouble(), pixels[i][1].toDouble(), pixels[i][2].toDouble())
        }
        
        // Try clustering with different K values (1-3) and pick based on cluster separation
        var bestClusters: List<Triple<Double, Double, Double>> = emptyList()
        var bestK = 1
        var bestCompactness = Double.MAX_VALUE
        
        for (k in 1..MAX_COLOR_CLUSTERS) {
            try {
                val labels = Mat()
                val centers = Mat()
                
                val criteria = org.opencv.core.TermCriteria(
                    org.opencv.core.TermCriteria.EPS + org.opencv.core.TermCriteria.MAX_ITER,
                    COLOR_KMEANS_ITERATIONS,
                    COLOR_EPSILON
                )
                
                // kmeans returns the compactness measure (lower is better for given k)
                val compactness = Core.kmeans(samples, k, labels, criteria, 
                    COLOR_KMEANS_ATTEMPTS, Core.KMEANS_PP_CENTERS, centers)
                
                // Extract cluster centers
                val clusters = mutableListOf<Triple<Double, Double, Double>>()
                for (i in 0 until centers.rows()) {
                    val center = centers.get(i, 0)
                    clusters.add(Triple(center[0], center[1], center[2]))
                }
                
                // Prefer configurations with well-separated clusters
                // For k=1, always accept. For k>1, prefer lower compactness
                if (k == 1 || compactness < bestCompactness) {
                    bestClusters = clusters
                    bestK = k
                    bestCompactness = compactness
                }
                
                labels.release()
                centers.release()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in k-means clustering with k=$k", e)
            }
        }
        
        samples.release()
        
        // Return the best clusters found (or default if none found)
        return if (bestClusters.isNotEmpty()) {
            bestClusters
        } else {
            listOf(Triple(DEFAULT_CLUSTER_VALUE, DEFAULT_CLUSTER_VALUE, DEFAULT_CLUSTER_VALUE))
        }
    }
    
    /**
     * Maps a color cluster to a CardColor enum based on RGB characteristics
     */
    private fun mapClusterToCardColor(cluster: Triple<Double, Double, Double>): Card.CardColor {
        val (r, g, b) = cluster
        
        // Classify based on dominant channel
        return when {
            r > g && r > b -> Card.CardColor.RED     // Red dominant
            g > r && g > b -> Card.CardColor.GREEN   // Green dominant
            else -> Card.CardColor.PURPLE            // Purple: high R+B, low G
        }
    }
    
    /**
     * Calculates the ratio of colored pixels vs total pixels in a symbol region
     */
    private fun calculateColoredPixelRatio(cardRegion: Mat): Double {
        // Convert to grayscale
        val gray = Mat()
        Imgproc.cvtColor(cardRegion, gray, Imgproc.COLOR_RGB2GRAY)
        
        // Threshold to identify symbols (non-white regions)
        val symbolMask = Mat()
        Imgproc.threshold(gray, symbolMask, 0.0, 255.0, 
            Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)
        
        // Convert to bitmap to analyze pixel colors
        val bitmap = Bitmap.createBitmap(cardRegion.cols(), cardRegion.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(cardRegion, bitmap)
        
        var totalSymbolPixels = 0
        var coloredPixels = 0
        
        // Analyze pixels in symbol regions with sampling for performance
        for (x in 0 until cardRegion.cols() step COLOR_SAMPLING_STEP) {
            for (y in 0 until cardRegion.rows() step COLOR_SAMPLING_STEP) {
                val maskValue = symbolMask.get(y, x)[0]
                if (maskValue > 0.0) {
                    totalSymbolPixels++
                    
                    val pixel = bitmap.getPixel(x, y)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    
                    // Count as colored if not white
                    if (isColoredPixel(r, g, b)) {
                        coloredPixels++
                    }
                }
            }
        }
        
        gray.release()
        symbolMask.release()
        bitmap.recycle()
        
        return if (totalSymbolPixels > 0) {
            coloredPixels.toDouble() / totalSymbolPixels
        } else {
            0.0
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
        // Note: CLAHE objects are managed by OpenCV's Java bindings and GC
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
    private fun recognizeCard(cardRegion: Mat, rect: Rect, rotation: Float, cardIndex: Int): Card? {
        try {
            diagnosticLogger.logSection("Feature Extraction - Card $cardIndex")
            // Normalize brightness and contrast
            val normalizedRegion = normalizeCardRegion(cardRegion)
            
            // Analyze the card to determine its attributes
            val number = detectNumber(normalizedRegion)
            diagnosticLogger.log("  Number: ${number.name} (${number.count})")

            val shape = detectShape(normalizedRegion)
            diagnosticLogger.log("  Shape: ${shape.name}")
            
            // Generate symbol mask for color detection
            val gray = Mat()
            Imgproc.cvtColor(normalizedRegion, gray, Imgproc.COLOR_RGB2GRAY)
            val symbolMask = Mat()
            Imgproc.threshold(gray, symbolMask, 0.0, 255.0, 
                Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)
            gray.release()
            
            val color = detectColor(normalizedRegion, symbolMask)
            diagnosticLogger.log("  Color: ${color.name}")
            val shading = detectShading(normalizedRegion)
            diagnosticLogger.log("  Shading: ${shading.name}")
            diagnosticLogger.log("  Bounding box: (${rect.x}, ${rect.y}, ${rect.width}, ${rect.height})")
            diagnosticLogger.log("  Rotation: ${"%.1f".format(rotation)}°")
            
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
        // Need at least 4 points to form 2 edges that could be parallel
        if (points.size < 4) return 0
        
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
                // After normalization, this means angleDiff near 0 or near 180.
                if (angleDiff < PARALLEL_THRESHOLD || 
                    angleDiff > (180 - PARALLEL_THRESHOLD)) {
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
     * Detects the color of symbols on the card using global color clusters
     */
    private fun detectColor(cardRegion: Mat, symbolMask: Mat): Card.CardColor {
        if (globalColorClusters.isEmpty()) {
            // Fallback to default if no clusters available
            return Card.CardColor.PURPLE
        }
        
        // Collect colored pixels from this card
        val bitmap = Bitmap.createBitmap(cardRegion.cols(), cardRegion.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(cardRegion, bitmap)
        
        val coloredPixels = mutableListOf<Triple<Int, Int, Int>>()
        
        // Sample pixels with a step size for efficiency
        for (x in 0 until cardRegion.cols() step COLOR_SAMPLING_STEP) {
            for (y in 0 until cardRegion.rows() step COLOR_SAMPLING_STEP) {
                // Check if this pixel is part of a symbol
                val maskValue = symbolMask.get(y, x)[0]
                if (maskValue == 0.0) continue  // Skip background pixels
                
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                
                // Only consider colored (non-white) pixels
                if (isColoredPixel(r, g, b)) {
                    coloredPixels.add(Triple(r, g, b))
                }
            }
        }
        
        bitmap.recycle()
        
        if (coloredPixels.isEmpty()) {
            // No colored pixels, default to purple
            return Card.CardColor.PURPLE
        }
        
        // Find the closest cluster for each pixel and vote
        val clusterVotes = mutableMapOf<Int, Int>()
        
        for ((r, g, b) in coloredPixels) {
            var closestCluster = 0
            var minDistance = Double.MAX_VALUE
            
            for ((idx, cluster) in globalColorClusters.withIndex()) {
                val distance = colorDistance(r, g, b, cluster)
                if (distance < minDistance) {
                    minDistance = distance
                    closestCluster = idx
                }
            }
            
            clusterVotes[closestCluster] = (clusterVotes[closestCluster] ?: 0) + 1
        }
        
        // Get the cluster with most votes
        val dominantClusterIndex = clusterVotes.maxByOrNull { it.value }?.key
        if (dominantClusterIndex == null) {
            // Safety fallback
            return Card.CardColor.PURPLE
        }
        
        val dominantClusterColor = globalColorClusters[dominantClusterIndex]
        
        // Map cluster to CardColor
        return mapClusterToCardColor(dominantClusterColor)
    }
    
    /**
     * Calculates Euclidean distance between a pixel and a color cluster
     */
    private fun colorDistance(r: Int, g: Int, b: Int, cluster: Triple<Double, Double, Double>): Double {
        val (cr, cg, cb) = cluster
        val dr = r - cr
        val dg = g - cg
        val db = b - cb
        return sqrt(dr * dr + dg * dg + db * db)
    }

    /**
     * Detects the shading of symbols on the card using colored vs white pixel ratio
     */
    private fun detectShading(cardRegion: Mat): Card.Shading {
        // Calculate ratio of colored pixels to total symbol pixels
        val coloredRatio = calculateColoredPixelRatio(cardRegion)
        
        // Classify based on ratio thresholds
        return when {
            coloredRatio > SOLID_COLORED_RATIO -> Card.Shading.SOLID
            coloredRatio > STRIPED_COLORED_RATIO -> Card.Shading.STRIPED
            else -> Card.Shading.OPEN
        }
    }
}
