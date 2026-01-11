# Hybrid Grid + Contour Detection - Testing Guide

## Overview

This document describes the new hybrid grid + contour detection feature and how to test it.

## Feature Summary

The card detection now uses a **hybrid approach** that combines grid-based estimation with contour detection:

1. **Grid-based initial estimate** - Divides the image into a 3×N grid
2. **Expanded search region** - Adds 15% margin around each grid cell to handle displaced cards
3. **Contour detection** - Finds actual card boundaries using adaptive thresholding
4. **Polygon approximation** - Identifies quadrilateral card shapes
5. **Rotation detection** - Uses `minAreaRect()` to detect card rotation
6. **Image de-rotation** - Extracts properly oriented cards using `warpAffine()`

## Changes Made

### Modified Files
- `CardDetector.kt`

### New Constants
- `SEARCH_MARGIN = 0.15` - 15% expansion of grid cell for search region
- `CONTOUR_APPROX_EPSILON = 0.02` - 2% deviation allowed for polygon approximation

### New Methods
- `findCardContourInRegion(region: Mat): RotatedRect?` - Detects card contour within a search region
- `extractRotatedCard(mat: Mat, rotatedRect: RotatedRect, offsetX: Int, offsetY: Int): Triple<Mat, Rect, Float>` - Extracts and de-rotates card region

### Modified Methods
- `extractCardFromGrid()` - Now returns `Triple<Mat?, Rect?, Float>` with rotation angle
- `validateCardDimensions()` - Updated to work with new data structure
- `detectCards()` - Updated to pass rotation angle to `recognizeCard()`

## Testing Scenarios

### 1. Perfectly Aligned Grid (Backward Compatibility)
**Setup:** Place cards in a perfect 3×4 or 3×5 grid with uniform spacing and no rotation.

**Expected Behavior:**
- Cards should be detected successfully (same as before)
- Rotation angles should be close to 0°
- Detection accuracy should be maintained or improved

**How to Test:**
1. Launch the app
2. Arrange Set cards in a perfect grid
3. Tap "Capture" button
4. Verify all cards are detected
5. Check diagnostics view for rotation angles (should be near 0°)

### 2. Displaced Cards
**Setup:** Place cards with ±10-20 pixel displacement from ideal grid positions.

**Expected Behavior:**
- Cards should still be detected within the expanded search region
- Contour detection should find the actual card boundaries
- Rotation angles may vary slightly

**How to Test:**
1. Arrange cards in an approximate grid with intentional displacement
2. Cards should be shifted left/right/up/down by 10-20 pixels
3. Capture and verify detection
4. Check diagnostics for successful contour detection

### 3. Rotated Cards
**Setup:** Place cards with ±5-10 degree rotation from vertical.

**Expected Behavior:**
- Contour detection should identify the rotated card boundaries
- `minAreaRect()` should detect the rotation angle
- Cards should be de-rotated before feature extraction
- Rotation angles should be logged in diagnostics

**How to Test:**
1. Arrange cards with slight rotation (5-10 degrees)
2. Ensure cards are still roughly in grid positions
3. Capture and verify detection
4. Check diagnostics view for detected rotation angles
5. Verify cards are recognized correctly despite rotation

### 4. Combined Displacement + Rotation
**Setup:** Place cards with both displacement AND rotation.

**Expected Behavior:**
- Hybrid detection should handle both displacement and rotation
- All cards should be detected and recognized correctly
- Rotation angles should be properly logged

**How to Test:**
1. Arrange cards with both displacement (10-20 pixels) and rotation (5-10 degrees)
2. Capture and verify detection
3. Check diagnostics for both aspects

### 5. Fallback to Grid Detection
**Setup:** Use lighting or card conditions where contour detection might fail.

**Expected Behavior:**
- If contour detection fails, should gracefully fall back to simple grid extraction
- Detection should still work, though may be less accurate for rotated/displaced cards
- No crashes or errors

**How to Test:**
1. Test with poor lighting conditions
2. Test with reflective card surfaces
3. Verify app doesn't crash and still attempts detection

## Diagnostic Logging

The diagnostics view will show:
- Grid dimensions detected (3×4 or 3×5)
- Number of card regions extracted
- Number passing dimension validation
- Rotation angle for each card in degrees
- Feature extraction results per card

### Key Log Entries to Check:
```
=== Grid-Based Card Detection ===
Detected grid: 3 rows × 4 columns
Extracted 12 card regions from grid
12 cards passed dimension validation

=== Feature Extraction - Card 1 ===
Number: ONE (1)
Shape: DIAMOND
Color: RED
Shading: SOLID
Bounding box: (x, y, width, height)
Rotation: 5.3°    <-- Check this value
```

## Performance Considerations

- **Search margin:** 15% expansion balances detection accuracy with performance
- **Contour approximation:** 2% epsilon ensures good quadrilateral detection
- **Area filtering:** Uses existing MIN_CARD_AREA and MAX_CARD_AREA thresholds
- **Memory management:** All OpenCV Mat objects are properly released

## Known Limitations

1. **Rotation range:** Best results with ±10 degrees. Larger rotations may fail.
2. **Displacement range:** Works well within ±15-20% of card size.
3. **Lighting:** Requires reasonable lighting for contour detection.
4. **Card overlap:** Cards must not overlap significantly.

## Troubleshooting

### Cards not detected with rotation
- Check if rotation is within ±10 degree range
- Verify lighting conditions are adequate
- Check diagnostics to see if contour detection is working

### Rotation angle seems wrong
- OpenCV's `minAreaRect()` can be ambiguous for rectangles
- The code normalizes angles to [-180, 180] range
- Check if card is being de-rotated correctly in feature extraction

### Performance degradation
- Contour detection adds processing time per card
- Consider optimizing search region size if needed
- Ensure image resolution is appropriate

## Code Review Checklist

- [x] Constants added for search margin and epsilon
- [x] `findCardContourInRegion()` implemented with proper cleanup
- [x] `extractRotatedCard()` handles rotation correctly
- [x] `extractCardFromGrid()` has graceful fallback
- [x] Data flow updated throughout detection pipeline
- [x] Memory management - all Mat objects released
- [x] Error handling in all new methods
- [x] Backward compatibility maintained

## Next Steps

After testing, consider:
1. Fine-tuning SEARCH_MARGIN if needed
2. Adjusting CONTOUR_APPROX_EPSILON for better quadrilateral detection
3. Adding more sophisticated rotation angle normalization
4. Implementing confidence scoring for contour detection
