#!/bin/bash
# Check FT8 Tracker status

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "========================================="
echo "FT8 Tracker Status"
echo "========================================="
echo ""

# Check if process is running
if pgrep -f "python3 src/main.py" > /dev/null; then
    echo "✓ Tracker service: RUNNING"
    PID=$(pgrep -f "python3 src/main.py")
    echo "  PID: $PID"
else
    echo "✗ Tracker service: NOT RUNNING"
fi

echo ""

# Check GPS
if systemctl is-active --quiet gpsd; then
    echo "✓ GPS daemon: RUNNING"
    if command -v gpspipe &> /dev/null; then
        GPS_FIX=$(timeout 3 gpspipe -w -n 10 2>/dev/null | grep -m 1 '"mode":[23]' && echo "YES" || echo "NO")
        echo "  GPS Fix: $GPS_FIX"
    fi
else
    echo "✗ GPS daemon: NOT RUNNING"
fi

echo ""

# Check network server
if netstat -ln 2>/dev/null | grep -q ":8080"; then
    echo "✓ Network server: LISTENING on port 8080"
    CLIENTS=$(netstat -an 2>/dev/null | grep ":8080" | grep "ESTABLISHED" | wc -l)
    echo "  Connected clients: $CLIENTS"
else
    echo "✗ Network server: NOT LISTENING"
fi

echo ""

# Check database
if [ -f "$PROJECT_DIR/data/tracker.db" ]; then
    echo "✓ Database: EXISTS"
    DECODE_COUNT=$(sqlite3 "$PROJECT_DIR/data/tracker.db" "SELECT COUNT(*) FROM decodes" 2>/dev/null || echo "0")
    echo "  Total decodes: $DECODE_COUNT"
    PENDING=$(sqlite3 "$PROJECT_DIR/data/tracker.db" "SELECT COUNT(*) FROM decodes WHERE uploaded=0" 2>/dev/null || echo "0")
    echo "  Pending upload: $PENDING"
else
    echo "✗ Database: NOT FOUND"
fi

echo ""

# Show recent log entries
if [ -f "$PROJECT_DIR/logs/tracker.log" ]; then
    echo "Recent log entries:"
    tail -n 5 "$PROJECT_DIR/logs/tracker.log"
fi

echo ""
echo "========================================="
