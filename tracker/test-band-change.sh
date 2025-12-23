#!/bin/bash

# Test script to verify band change logging is working

set -e

HOST="${1:-127.0.0.1}"
TCP_PORT="${2:-8080}"
HTTP_PORT="${3:-8081}"

echo "Testing Band Change Logging"
echo "=============================="
echo "Host: $HOST"
echo "TCP Port (decode stream): $TCP_PORT"
echo "HTTP Port (API): $HTTP_PORT"
echo ""

# Test HTTP server is running
echo "Testing HTTP server connectivity on port $HTTP_PORT..."
if timeout 3 bash -c "echo > /dev/tcp/${HOST}/${HTTP_PORT}" 2>/dev/null; then
    echo "✓ HTTP server is reachable"
else
    echo "✗ HTTP server not responding on port $HTTP_PORT"
    echo "Make sure the tracker is running with:"
    echo "  python3 src/main.py"
    exit 1
fi

echo ""
echo "Sending band change request to /band endpoint..."
echo ""

# Send a band change request
RESPONSE=$(curl -s -X POST "http://${HOST}:${HTTP_PORT}/band" \
  -H "Content-Type: application/json" \
  -d '{"band": "20m"}')

echo "Response from server:"
echo "$RESPONSE" | jq . 2>/dev/null || echo "$RESPONSE"
echo ""

if echo "$RESPONSE" | grep -q "ok"; then
    echo "✓ Band change request accepted"
    echo ""
    echo "Check the tracker logs for band change messages:"
    echo "  - 'Received band change request: 20m' (network_server.py)"
    echo "  - 'Band changed to: 20m' (main.py)"
    echo "  - 'Band change recorded in database' (database.py)"
else
    echo "✗ Band change request failed"
    exit 1
fi
