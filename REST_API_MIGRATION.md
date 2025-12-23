# REST API Consolidation - Implementation Summary

## Overview
Successfully consolidated the FT8 Tracker to a modern, elegant REST API architecture using Flask with Server-Sent Events (SSE) for decode streaming.

## Architecture Changes

### **Before (Split Architecture)**
- **TCP Port 8080**: Raw socket server for decode streaming
- **HTTP Port 8081**: Separate HTTP server for /gps and /band endpoints
- Problem: Two separate servers on different ports, low-level BaseHTTPRequestHandler

### **After (Unified REST Architecture)**
- **HTTP Port 8080**: Single Flask server handling everything
  - `GET /decodes` - Server-Sent Events stream for FT8 decodes
  - `POST /gps` - GPS position update
  - `POST /band` - Band change notification
  - `GET /health` - Health check endpoint

## Changes Made

### **Tracker (Python)**

#### 1. New Flask Network Server (`src/network_server_flask.py`)
- Modern Flask-based REST API
- Server-Sent Events (SSE) for real-time decode streaming
- JSON request/response bodies
- Proper HTTP status codes and error handling
- Debug and info logging at each step

**Key Features:**
```python
@app.route('/decodes', methods=['GET'])     # SSE stream of FT8 decodes
@app.route('/gps', methods=['POST'])        # Update GPS position
@app.route('/band', methods=['POST'])       # Update operating band
@app.route('/health', methods=['GET'])      # Server health check
```

#### 2. Updated `src/main.py`
- Changed import from `network_server` to `network_server_flask`
- Uses `FlaskNetworkServer` instead of `NetworkServer`
- No changes to callback logic - same band/GPS callbacks

#### 3. Updated `config/tracker.conf`
- Removed `server_http_port` setting
- Single port configuration: `server_port = 8080`

#### 4. Updated `requirements.txt`
- Added `flask>=2.0.0` dependency

### **Android App (Kotlin)**

#### Updated `FT8DataService.kt`
- Changed from raw TCP Socket to HTTP SSE
- Connects to `GET http://host:8080/decodes`
- Parses SSE format: `data: {"decode": "..."}`
- Automatically sends band on connect via POST to `/band`
- Handles connection errors with automatic retry

**Key changes:**
```kotlin
// Before: Socket(host, port)
// After: HttpURLConnection to http://host:port/decodes with SSE parsing
```

## Testing

### **Test Scripts**

1. **test-api.sh** - Interactive REST API testing
   ```bash
   ./test-api.sh 127.0.0.1 8080
   ```
   - Menu for testing GPS and band endpoints
   - Health check
   - Connection test

2. **test-sse-stream.sh** - Test SSE decode stream
   ```bash
   ./test-sse-stream.sh 127.0.0.1 8080
   ```
   - Connects to `/decodes` endpoint
   - Displays incoming decodes in real-time
   - Shows keepalive messages

3. **test-band-change.sh** - Deprecated (but kept for reference)
   - Old script for split ports

### **Manual Testing**

```bash
# Test health endpoint
curl http://localhost:8080/health | jq

# Send band change
curl -X POST http://localhost:8080/band \
  -H "Content-Type: application/json" \
  -d '{"band": "20m"}' | jq

# Send GPS update
curl -X POST http://localhost:8080/gps \
  -H "Content-Type: application/json" \
  -d '{"latitude": 47.606, "longitude": -122.332}' | jq

# Connect to decode stream (Ctrl+C to stop)
curl http://localhost:8080/decodes
```

## Benefits of New Architecture

✅ **Single Port (8080)** - Simpler configuration and firewall rules
✅ **Modern REST API** - Uses industry-standard Flask framework
✅ **SSE Streaming** - Proper HTTP streaming protocol instead of raw TCP
✅ **Better Error Handling** - HTTP status codes, JSON error responses
✅ **Elegant Code** - Flask decorators, clean routing, easy to extend
✅ **Better Logging** - Comprehensive debug and info logging
✅ **JSON Everywhere** - All communication is structured JSON
✅ **Health Checks** - Built-in health monitoring endpoint
✅ **Standards Compliant** - Uses standard HTTP, SSE, and REST conventions

## Backward Compatibility Notes

⚠️ **Breaking Change: Network Protocol**
- Android app must be updated to use SSE instead of TCP socket
- Old TCP clients will no longer work
- All clients must use the new REST API

✅ **Compatible: REST API Endpoints**
- `/gps` endpoint works the same (POST with JSON)
- `/band` endpoint works the same (POST with JSON)
- Same database storage, same callbacks

## Database

✅ No changes to database layer
- `band_changes` table still captures band changes
- `gps_positions` table still captures GPS updates
- All existing queries work unchanged

## Logging

Comprehensive logging at each stage:

**When band changes received:**
1. Flask receives POST to `/band`
2. Validates band format
3. Stores in `current_band`
4. Calls `_on_band_change` callback
5. Database inserts record
6. Logs: `Band changed to: 20m`

**When decodes sent:**
1. Tracker generates decode
2. Queues to all SSE clients
3. SSE format: `data: {"decode": "..."}`
4. Android app receives and parses

## Future Enhancements

Now that we have a proper REST foundation, consider:
- WebSocket instead of SSE for bidirectional communication
- Additional endpoints for stats, history, etc.
- API documentation (Swagger/OpenAPI)
- Authentication/authorization
- Rate limiting
- Caching

## Migration Checklist

- [x] Create Flask network server
- [x] Update main.py to use Flask server
- [x] Update Android app to use SSE
- [x] Update configuration
- [x] Update test scripts
- [x] Add Flask to requirements
- [x] Verify syntax
- [ ] Build and test Android app
- [ ] Deploy and test end-to-end
