#!/bin/bash

# Start Android Auto Desktop Head Unit
# This script sets up ADB forwarding and launches the DHU

set -e

echo "Starting Android Auto Desktop Head Unit..."

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "Error: No Android device connected via ADB"
    echo "Please enable USB Debugging on your phone and connect it via USB"
    exit 1
fi

echo "✓ Device connected"

# Set up port forwarding
echo "Setting up ADB port forwarding..."
adb forward tcp:5277 tcp:5277
echo "✓ Port forwarding configured"

# Start DHU with USB transport
echo ""
echo "Launching Desktop Head Unit..."
echo "(You can ignore audio-related errors - they don't affect UI testing)"
echo ""

~/Android/Sdk/extras/google/auto/desktop-head-unit -u

