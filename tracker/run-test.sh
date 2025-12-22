#!/bin/bash
# Quick test runner for FT8 tracker with test data

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if test file specified
TEST_FILE="${1:-test_decodes.txt}"

# Create necessary directories
mkdir -p logs data

echo -e "${BLUE}=== FT8 Tracker Test Mode ===${NC}"
echo "Test file: $TEST_FILE"
echo ""

# Check if file exists
if [ ! -f "$TEST_FILE" ]; then
    echo "Error: Test file not found: $TEST_FILE"
    echo ""
    echo "Usage: $0 [test_file.txt]"
    echo ""
    echo "Example:"
    echo "  $0                          # Use default test_decodes.txt"
    echo "  $0 my_custom_decodes.txt    # Use custom test file"
    exit 1
fi

# Count decodes in file
DECODE_COUNT=$(grep -c "^[0-9]" "$TEST_FILE" || echo 0)
echo "Decodes in test file: $DECODE_COUNT"
echo ""

echo -e "${GREEN}Starting tracker in test mode...${NC}"
echo "Network server will listen on: 127.0.0.1:8080"
echo "Connect Android app to see decodes!"
echo ""
echo "Press Ctrl+C to stop"
echo ""

# Run tracker
python3 ./src/main.py -c config/tracker.conf --test-file "$TEST_FILE" -v

echo ""
echo -e "${BLUE}Test run complete!${NC}"
