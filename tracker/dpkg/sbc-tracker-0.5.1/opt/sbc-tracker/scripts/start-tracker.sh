#!/bin/bash
# Start FT8 Tracker

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

# Activate virtual environment if it exists
if [ -d "venv" ]; then
    source venv/bin/activate
fi

# Create logs directory
mkdir -p logs

# Start the tracker
echo "Starting FT8 Tracker..."
python3 src/main.py -c config/tracker.conf
