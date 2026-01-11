# Building the Set Solver Android App

This guide explains how to build and run the Set Solver Android app.

## Prerequisites

1. **Android Studio** (Recommended version: Arctic Fox or later)
   - Download from: https://developer.android.com/studio

2. **Android SDK**
   - Minimum SDK: API 24 (Android 7.0)
   - Target SDK: API 34 (Android 14)
   - Compile SDK: API 34

3. **Gradle** (included via wrapper)
   - All dependencies including OpenCV are automatically fetched from Maven Central

## Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/gfr10598/set-solver.git
cd set-solver
```

### 2. Open Project in Android Studio

1. Launch Android Studio
2. Select "Open an Existing Project"
3. Navigate to the cloned repository and select it
4. Wait for Gradle to sync

### 3. Build the Project

**Note**: Gradle will automatically download all dependencies, including OpenCV 4.12.0 from Maven Central. This may take a few minutes on first build.

#### Using Android Studio

1. Click on "Build" in the menu bar
2. Select "Make Project" or press `Ctrl+F9` (Windows/Linux) or `Cmd+F9` (Mac)

#### Using Command Line

```bash
./gradlew assembleDebug
```

The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

### 4. Run on Device or Emulator

#### Using Android Studio

1. Connect an Android device via USB (with USB debugging enabled) or start an emulator
2. Click the "Run" button (green triangle) or press `Shift+F10`
3. Select your device from the list

#### Using Command Line

```bash
./gradlew installDebug
```

## App Permissions

The app requires the following permission:
- **Camera**: To capture photos of Set cards

The app will request this permission at runtime when first launched.

## How to Use the App

1. **Launch the app** - The camera preview will appear
2. **Position the cards** - Place Set cards in view of the camera
3. **Tap "Capture"** - The app will:
   - Capture the current camera frame
   - Detect individual cards in the image
   - Identify the pattern on each card (number, shape, color, shading)
   - Find all valid Sets among the detected cards
   - Highlight the Sets with colored rectangles and connecting lines

4. **View results** - Sets are highlighted with different colors:
   - Green for the first set
   - Red for the second set
   - Blue for the third set
   - Yellow for the fourth set

## Architecture

The app is structured as follows:

### Core Components

- **MainActivity.kt**: Main activity handling camera, permissions, and UI
- **Card.kt**: Data model for a Set card with its four attributes
- **CardDetector.kt**: Image processing logic to detect and recognize cards
- **SetFinder.kt**: Algorithm to find valid Sets
- **ResultOverlayView.kt**: Custom view to display set highlights
- **ImageUtils.kt**: Utility functions for image conversion

### Card Detection Process

1. **Image Preprocessing**: Convert to grayscale, apply Gaussian blur
2. **Edge Detection**: Use adaptive thresholding
3. **Contour Detection**: Find card boundaries
4. **Card Extraction**: Extract individual card regions
5. **Pattern Recognition**: Analyze each card for:
   - Number (1, 2, or 3 symbols)
   - Shape (Diamond, Oval, or Squiggle)
   - Color (Red, Green, or Purple)
   - Shading (Solid, Striped, or Open)

### Set Finding Algorithm

The algorithm checks all combinations of three cards to find valid Sets.
A Set is valid when for each of the four attributes, the values are either:
- All the same, OR
- All different

## Troubleshooting

### OpenCV Initialization Failed

If you see "OpenCV initialization failed":
1. Ensure Gradle successfully downloaded OpenCV from Maven Central
2. Check your internet connection during the initial build
3. Clean and rebuild the project: `./gradlew clean assembleDebug`

### Camera Permission Denied

If the camera permission is denied:
1. Uninstall the app
2. Reinstall and grant camera permission when prompted
3. Or go to Settings → Apps → Set Solver → Permissions and enable Camera

### Build Errors

If you encounter build errors:
1. Check that you're using a compatible version of Android Studio
2. Ensure all SDK components are installed
3. Try syncing Gradle: File → Sync Project with Gradle Files
4. Clean and rebuild: Build → Clean Project, then Build → Rebuild Project

## Development

### Code Style

- The project uses Kotlin for all source code
- Follow standard Kotlin coding conventions
- Use meaningful variable and function names

### Testing

To run tests:

```bash
./gradlew test
```

### Debugging

To enable debug logging, check the Logcat in Android Studio with the following filters:
- `MainActivity`
- `CardDetector`
- `SetFinder`

## Future Enhancements

Potential improvements:
1. Improve card detection accuracy with machine learning
2. Add real-time detection without capture button
3. Support for different card orientations
4. Add statistics and game tracking
5. Improve shape recognition algorithm
6. Add support for finding Sets in real-time video

## License

This project is open source and available under the MIT License.
