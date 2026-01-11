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

# Make gradlew executable
if [ -f "gradlew" ]; then
    chmod +x gradlew
    echo "✓ Made gradlew executable"
fi

echo ""
echo "Setup complete!"
echo ""
echo "Next steps:"
echo "1. Open project in Android Studio, or"
echo "2. Build from command line: ./gradlew assembleDebug"
echo "   (Dependencies including OpenCV will be downloaded automatically)"
echo ""
