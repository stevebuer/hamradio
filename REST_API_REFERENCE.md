# REST API Quick Reference

## Server Endpoints (Single Port: 8080)

### GET /decodes
**Server-Sent Events stream of FT8 decodes**
- Returns: `text/event-stream`
- Format: `data: {"decode": "TIME SNR DT FREQ ~ CALLSIGN GRID"}`
- Usage: Connect and receive real-time decode stream

```bash
# Test with curl
curl http://localhost:8080/decodes

# Example in Android/Kotlin
val url = URL("http://host:8080/decodes")
val connection = url.openConnection() as HttpURLConnection
val reader = BufferedReader(InputStreamReader(connection.inputStream))
while (true) {
    val line = reader.readLine()
    if (line?.startsWith("data: ") == true) {
        val json = JSONObject(line.substring(6))
        val decode = json.getString("decode")
    }
}
```

### POST /gps
**Update GPS position**
- Content-Type: `application/json`
- Body: `{"latitude": 47.606, "longitude": -122.332, ...}`
- Returns: `{"status": "ok", "message": "..."}`

```bash
# Test with curl
curl -X POST http://localhost:8080/gps \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 47.606,
    "longitude": -122.332,
    "altitude": 100,
    "accuracy": 5,
    "timestamp": 1234567890
  }'
```

**Optional fields:**
- `timestamp` - Unix timestamp (auto-added if missing)
- `altitude` - Altitude in meters
- `speed` - Speed in m/s
- `heading` - Heading in degrees
- `accuracy` - Position accuracy in meters

### POST /band
**Update operating band**
- Content-Type: `application/json`
- Body: `{"band": "20m"}`
- Returns: `{"status": "ok", "message": "Band set to 20m"}`

```bash
# Test with curl
curl -X POST http://localhost:8080/band \
  -H "Content-Type: application/json" \
  -d '{"band": "20m"}'
```

**Valid bands:** 80m, 60m, 40m, 30m, 20m, 17m, 15m, 12m, 10m, 6m

### GET /health
**Server health check**
- Returns: `{"status": "ok", "current_band": "20m", "last_gps": {...}, "sse_clients": 1}`

```bash
# Test with curl
curl http://localhost:8080/health | jq
```

## Testing Tools

### test-api.sh - Interactive REST API Testing
```bash
cd tracker
./test-api.sh [host] [port]
# Default: ./test-api.sh 127.0.0.1 8080

# Menu options:
# 1) Send GPS position update
# 2) Send band change notification
# 3) Check server health
# 4) Test connection
# 5) Exit
```

### test-sse-stream.sh - Test Decode Stream
```bash
cd tracker
./test-sse-stream.sh [host] [port]
# Default: ./test-sse-stream.sh 127.0.0.1 8080

# Connects to SSE stream and displays decodes as they arrive
# Press Ctrl+C to stop
```

## Configuration

Edit `config/tracker.conf`:
```ini
[network]
server_enabled = true
server_port = 8080          # REST API port
server_bind = 0.0.0.0       # Bind address (all interfaces)
```

## Starting the Server

```bash
cd /home/steve/GITHUB/hamradio/tracker
python3 src/main.py
```

**Expected output:**
```
INFO: Flask server started on 0.0.0.0:8080
INFO: REST API endpoints:
INFO:   GET  /decodes  - Server-Sent Events stream for FT8 decodes
INFO:   POST /gps      - GPS position update
INFO:   POST /band     - Band change notification
INFO:   GET  /health   - Health check
```

## Error Responses

All errors return JSON with HTTP status codes:

```json
// 400 Bad Request
{"error": "Missing latitude or longitude"}

// 404 Not Found
{"error": "Not found"}

// 500 Internal Server Error
{"error": "Internal server error"}
```

## Android App Integration

The Android app (`FT8DataService.kt`) now:
1. Connects to `GET http://host:8080/decodes`
2. Reads Server-Sent Events stream
3. Parses JSON decode objects
4. On connection, sends band via `POST http://host:8080/band`

## Database

Band changes and GPS updates are automatically stored in SQLite:
- `band_changes` table - All band changes with timestamp
- `gps_positions` table - All GPS updates with source

Query examples:
```python
# Get band change history
changes = db.get_band_changes(limit=50)

# Get bands worked today
from datetime import datetime
today = int(datetime.now().replace(hour=0, minute=0, second=0).timestamp())
bands = db.get_bands_worked(since_timestamp=today)
```

## Logging

Configure logging level in `config/tracker.conf`:
```ini
[logging]
level = DEBUG    # INFO, DEBUG, WARNING, ERROR
file = ./logs/tracker.log
```

## Common Issues

**Port already in use:**
```bash
# Find what's using port 8080
sudo lsof -i :8080

# Kill the process
kill -9 <PID>
```

**Flask not found:**
```bash
pip install flask
```

**Connection refused:**
- Verify tracker is running
- Check firewall rules
- Verify correct host/port in Android app settings

## Performance Notes

- SSE supports multiple concurrent clients
- Each connected client is tracked in `/health` endpoint
- Decode queue is non-blocking - clients that can't keep up will drop decodes
- Logging is debug-level by default for minimal overhead
