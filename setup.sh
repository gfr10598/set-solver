#!/bin/bash

# Setup script for Set Solver Android app

echo "Set Solver - Setup Script"
echo "=========================="
echo ""

# Check if Android SDK is available
if [ -z "$ANDROID_HOME" ]; then
    echo "Warning: ANDROID_HOME is not set."
    echo "Please install Android SDK and set ANDROID_HOME environment variable."
    echo ""
fi

# Check for OpenCV
if [ ! -f "app/libs/opencv-android-sdk.aar" ]; then
    echo "Warning: OpenCV AAR file not found in app/libs/"
    echo ""
    echo "Please download OpenCV for Android from:"
    echo "https://opencv.org/releases/"
    echo ""
    echo "Then copy the AAR file to: app/libs/opencv-android-sdk.aar"
    echo ""
    echo "For detailed instructions, see BUILD.md"
    echo ""
fi

# Make gradlew executable
if [ -f "gradlew" ]; then
    chmod +x gradlew
    echo "✓ Made gradlew executable"
fi

echo ""
echo "Setup complete!"
echo ""
echo "Next steps:"
echo "1. Install OpenCV (if not already done)"
echo "2. Open project in Android Studio, or"
echo "3. Build from command line: ./gradlew assembleDebug"
echo ""
