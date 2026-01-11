# Set Solver

An Android app that uses computer vision to find Sets in a group of Set cards.

## Features

- **Camera Integration**: Capture photos using the phone camera
- **Card Detection**: Automatically detects individual Set cards in an image
- **Pattern Recognition**: Identifies the four attributes of each card:
  - Number (1, 2, or 3)
  - Shape (Diamond, Oval, or Squiggle)
  - Color (Red, Green, or Purple)
  - Shading (Solid, Striped, or Open)
- **Set Finding**: Searches for all valid Sets among detected cards
- **Visual Highlighting**: Highlights found Sets with colored overlays and connecting lines

## Quick Start

1. Clone the repository
2. Follow the instructions in [BUILD.md](BUILD.md) to set up and build the app
3. Install on an Android device (API 24+)
4. Grant camera permission when prompted
5. Point camera at Set cards and tap "Capture"

## How It Works

The app uses OpenCV for image processing to:
1. Detect card boundaries using contour detection
2. Extract individual card regions
3. Analyze each card to determine its attributes
4. Run the Set-finding algorithm on all detected cards
5. Display results with color-coded highlights

## Requirements

- Android 7.0 (API 24) or higher
- Camera permission
- All dependencies (including OpenCV 4.12.0) are automatically managed via Gradle

## Documentation

- [BUILD.md](BUILD.md) - Detailed build and setup instructions
- [Architecture](#architecture) - See BUILD.md for technical details

## Screenshot

*Note: Screenshots will be added after the app is built and tested*

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues.

## License

This project is open source and available under the MIT License.
