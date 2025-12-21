#!/bin/bash
# Stop FT8 Tracker

echo "Stopping FT8 Tracker..."
pkill -f "python3 src/main.py"
echo "Tracker stopped"
