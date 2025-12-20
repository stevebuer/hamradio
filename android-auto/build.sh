#!/bin/bash
# Simple build script for the Android Auto FT8 app

echo "Building FT8 Auto Display..."

# Clean previous builds
./gradlew clean

# Build debug APK
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "Build successful!"
    echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "To install on connected device:"
    echo "  adb install -r app/build/outputs/apk/debug/app-debug.apk"
else
    echo "Build failed!"
    exit 1
fi
