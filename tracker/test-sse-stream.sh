#!/bin/bash

# Test script to verify SSE decode stream

HOST="${1:-127.0.0.1}"
PORT="${2:-8080}"

echo "Testing SSE Decode Stream"
echo "=============================="
echo "Host: $HOST"
echo "Port: $PORT"
echo ""

echo "Connecting to SSE stream at http://${HOST}:${PORT}/decodes"
echo "Press Ctrl+C to stop"
echo ""

# Connect to SSE stream
curl -s "http://${HOST}:${PORT}/decodes" | while IFS= read -r line; do
    if [[ $line == data:* ]]; then
        # Extract JSON and pretty print
        echo "$line" | sed 's/^data: //' | jq . 2>/dev/null || echo "$line"
    elif [[ $line == :* ]]; then
        echo "  [keepalive]"
    fi
done
