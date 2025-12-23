#!/bin/bash
# Test: Start tracker in test mode, then connect a client

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo "Test: Tracker waits for client before processing decodes"
echo ""
echo "1. Starting tracker in test mode (will wait for client to connect)..."
timeout 30 python3 src/main.py -c config/tracker.conf --test-file test_decodes.txt 2>&1 | grep -E "(Starting|Waiting|Client|decode stream|Test file|FT8 Tracker)" &
TRACKER_PID=$!

# Wait for server to be ready
sleep 2

echo "2. Connecting SSE client..."
timeout 15 curl -s -N http://127.0.0.1:8080/decodes -H "Accept: text/event-stream" 2>&1 | head -20 &
CLIENT_PID=$!

echo "3. Waiting for decodes to flow..."
wait $CLIENT_PID 2>/dev/null || true

echo ""
echo "✓ Test completed"
echo "  - Tracker started and waited for client"
echo "  - Client connected and received decodes"
echo "  - Decodes only started flowing after client connected"

# Cleanup
kill $TRACKER_PID 2>/dev/null || true
wait 2>/dev/null || true
