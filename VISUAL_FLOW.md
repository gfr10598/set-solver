# Visual Implementation Flow

## Hybrid Grid + Contour Detection Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    detectCards(bitmap)                          │
│  • Converts bitmap to Mat                                       │
│  • Detects grid columns (4 or 5)                                │
│  • Loops through 3×N grid positions                             │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│         extractCardFromGrid(mat, row, col, numCols)             │
│  Returns: Triple<Mat?, Rect?, Float> (card, rect, rotation)     │
└────────────────────────┬────────────────────────────────────────┘
                         │
         ┌───────────────┴───────────────┐
         │                               │
         ▼                               ▼
┌──────────────────────┐      ┌──────────────────────┐
│  Calculate Grid Cell │      │ Expand Search Region │
│  • Base position     │──────▶ • +15% margin       │
│  • Cell dimensions   │      │ • Handle boundaries  │
└──────────────────────┘      └────────┬─────────────┘
                                       │
                                       ▼
                         ┌──────────────────────────────┐
                         │  findCardContourInRegion()   │
                         │  • Grayscale conversion      │
                         │  • Gaussian blur (5×5)       │
                         │  • Adaptive threshold        │
                         │  • Find contours             │
                         │  • Filter quadrilaterals     │
                         │  • Return RotatedRect        │
                         └────────┬─────────────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    │                           │
           Contour Found?                  Contour Not Found
                    │                           │
                    ▼                           ▼
        ┌───────────────────────┐   ┌──────────────────────┐
        │ extractRotatedCard()  │   │  Fallback to Simple  │
        │ • Get rotation angle  │   │  Grid Extraction     │
        │ • Normalize angle     │   │  • 5% inner margin   │
        │ • Create rotation mtx │   │  • No rotation (0°)  │
        │ • Apply warpAffine()  │   └────────┬─────────────┘
        │ • Extract upright card│            │
        └───────┬───────────────┘            │
                │                            │
                └────────────┬───────────────┘
                             │
                             ▼
                ┌─────────────────────────────┐
                │ Return Triple:              │
                │ • Mat: card region          │
                │ • Rect: bounding box        │
                │ • Float: rotation angle     │
                └──────────┬──────────────────┘
                           │
                           ▼
            ┌──────────────────────────────────┐
            │  validateCardDimensions()        │
            │  • Filter by size consistency    │
            │  • Keep cards within 30% median  │
            └──────────┬───────────────────────┘
                       │
                       ▼
            ┌──────────────────────────────────┐
            │  buildGlobalColorClusters()      │
            │  • Sample colored pixels         │
            │  • K-means clustering (1-3)      │
            └──────────┬───────────────────────┘
                       │
                       ▼
            ┌──────────────────────────────────┐
            │  recognizeCard(region, rect,     │
            │                 rotation, index)  │
            │  • Detect number (1, 2, 3)       │
            │  • Detect shape (D, O, S)        │
            │  • Detect color (R, G, P)        │
            │  • Detect shading (solid,        │
            │    striped, open)                │
            │  • Store rotation angle          │
            └──────────┬───────────────────────┘
                       │
                       ▼
                ┌─────────────────┐
                │  Return Card    │
                │  with rotation  │
                └─────────────────┘
```

## Key Decision Points

### 1. Search Region Calculation
```
Grid Cell: [baseX, baseY, cardWidth, cardHeight]
           ↓ Expand by 15%
Search Region: [baseX - marginX, baseY - marginY, 
                cardWidth + 2*marginX, cardHeight + 2*marginY]
           ↓ Clip to image bounds
Valid Search Region: [searchX, searchY, searchW, searchH]
```

### 2. Contour Detection
```
Search Region
    ↓ Grayscale
Gray Image
    ↓ Gaussian Blur (5×5)
Blurred Image
    ↓ Adaptive Threshold
Binary Image
    ↓ Find Contours
List of Contours
    ↓ Filter by area (MIN_CARD_AREA to MAX_CARD_AREA)
Valid Contours
    ↓ Approximate to polygon (ε = 2% of perimeter)
Polygon Approximations
    ↓ Filter for 4 vertices (quadrilateral)
Card Contours
    ↓ Select largest
Best Card Contour
    ↓ minAreaRect()
RotatedRect (center, size, angle)
```

### 3. Rotation Handling
```
RotatedRect.angle
    ↓ Check width vs height
If width < height: swap dimensions, angle += 90°
    ↓ Normalize to [-180, 180]
angle = ((angle % 360 + 540) % 360 - 180)
    ↓ Create rotation matrix
getRotationMatrix2D(center, angle, scale=1.0)
    ↓ Apply transformation
warpAffine(image, rotationMatrix, size)
    ↓ Extract upright region
Rotated Card Image
```

## Data Structure Evolution

### Before
```kotlin
extractCardFromGrid() -> Pair<Mat?, Rect?>
                          ↓
validateCardDimensions(List<Triple<Mat, Rect, Int>>)
                          ↓
recognizeCard(region, rect, rotation=0f, index)
```

### After
```kotlin
extractCardFromGrid() -> Triple<Mat?, Rect?, Float>
                          ↓
validateCardDimensions(List<Pair<Triple<Mat, Rect, Float>, Int>>)
                          ↓
recognizeCard(region, rect, rotation=detected_angle, index)
```

## Memory Management

```
┌─────────────────────────────────────────┐
│  findCardContourInRegion(region)        │
│                                         │
│  gray = Mat()                           │
│  blurred = Mat()                        │
│  thresh = Mat()                         │
│  hierarchy = Mat()                      │
│  contours = ArrayList<MatOfPoint>()     │
│                                         │
│  for contour in contours:               │
│    curve = MatOfPoint2f()               │
│    approx = MatOfPoint2f()              │
│    try {                                │
│      // Process                         │
│    } finally {                          │
│      curve.release()    ←─────┐        │
│      approx.release()   ←─────┼── Safe │
│    }                           │        │
│                                │        │
│  gray.release()        ←───────┤        │
│  blurred.release()     ←───────┤        │
│  thresh.release()      ←───────┤        │
│  hierarchy.release()   ←───────┤        │
│  contours.forEach {    ←───────┘        │
│    it.release()                         │
│  }                                      │
└─────────────────────────────────────────┘
```

## Configuration Parameters

```
┌─────────────────────────────────────────────┐
│  SEARCH_MARGIN = 0.15                       │
│  • 15% expansion of grid cell               │
│  • Allows ±10-20 pixel displacement         │
│  • Balance between coverage and performance │
├─────────────────────────────────────────────┤
│  CONTOUR_APPROX_EPSILON = 0.02              │
│  • 2% of perimeter for polygon approx       │
│  • Good balance for quadrilateral detection │
│  • Epsilon = 0.02 × arcLength(contour)      │
├─────────────────────────────────────────────┤
│  MIN_CARD_AREA = 5000.0                     │
│  MAX_CARD_AREA = 500000.0                   │
│  • Existing area filtering thresholds       │
│  • Prevents false positives from noise      │
└─────────────────────────────────────────────┘
```

## Error Handling Strategy

```
┌────────────────────────────────────┐
│  Try to detect contour             │
│         ↓                          │
│    Success?                        │
│    ├─ Yes → Extract rotated card   │
│    │        ├─ Success → Return    │
│    │        └─ Fail → Return null  │
│    │                               │
│    └─ No → Fallback to grid        │
│            ├─ Success → Return     │
│            └─ Fail → Return null   │
│                                    │
│  Caller checks null before using   │
└────────────────────────────────────┘
```

## Testing Coverage

```
Test Scenario               | Handled By
─────────────────────────────────────────────────
Perfect grid alignment      | Fallback mechanism
Displaced cards (±10-20px)  | Expanded search region
Rotated cards (±5-10°)      | Contour + rotation detection
Combined displacement+rot   | Full hybrid approach
Poor lighting/conditions    | Graceful fallback
Empty/invalid regions       | Early validation
Memory exceptions           | Try-finally cleanup
```
