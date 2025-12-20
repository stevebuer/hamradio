# GPS Location Sharing Between Android Auto and Tracker

This feature allows your Android Auto app to share its GPS location with the FT8 tracker running on the Pine64 in your trunk, eliminating the need for a separate USB GPS module.

## How It Works

1. **Android Auto App** - Gets GPS location from your vehicle (via Android Auto connection)
2. **HTTP Upload** - Sends GPS coordinates to the tracker via HTTP POST every 30 seconds
3. **Tracker Server** - Receives GPS updates on port 8081 and stores them in the database
4. **Database Storage** - GPS positions are stored in `gps_positions` table with source tracking
5. **Fallback GPS** - If the tracker's local GPS has no fix, it uses the Android GPS data

## Setup Instructions

### 1. Configure the Tracker (Pine64)

Edit your tracker configuration file:
```bash
nano ~/GITHUB/hamradio/tracker/config/tracker.conf
```

Ensure the network section includes:
```ini
[network]
server_enabled = true
server_port = 8080
server_bind = 0.0.0.0
http_port = 8081
```

The tracker will listen on:
- **Port 8080** - TCP server for sending FT8 decodes to Android
- **Port 8081** - HTTP server for receiving GPS updates from Android

### 2. Start the Tracker

```bash
cd ~/GITHUB/hamradio/tracker
python3 src/main.py config/tracker.conf
```

Verify the tracker is listening:
```bash
# Check if HTTP server is running
curl http://localhost:8081/health
```

### 3. Configure Android Auto App

1. Open the FT8 Auto Display app
2. Enter your tracker's IP address (e.g., `192.168.1.100`)
3. Keep the default port `8080` for FT8 decodes
4. Enable the **"Share GPS with Tracker"** toggle switch

The app will:
- Request location permissions if not already granted
- Start uploading GPS location every 30 seconds
- Run as a background foreground service (shows notification)
- Continue uploading even when the screen is off

### 4. Verify GPS Sharing

On the Pine64, check the tracker logs:
```bash
tail -f ~/GITHUB/hamradio/tracker/logs/tracker.log
```

You should see messages like:
```
External GPS update: 47.1234, -122.5678
Inserted GPS position 1 from android_auto: 47.1234, -122.5678
```

Or query the database directly:
```bash
sqlite3 ~/GITHUB/hamradio/tracker/data/tracker.db \
  "SELECT datetime(timestamp, 'unixepoch', 'localtime'), latitude, longitude, source FROM gps_positions ORDER BY timestamp DESC LIMIT 5;"
```

## Network Configuration

### Finding Your Tracker's IP Address

On the Pine64:
```bash
ip addr show | grep inet
```

Or if you're on the same WiFi network:
```bash
hostname -I
```

### Firewall Configuration

If the tracker has a firewall enabled, allow incoming connections:
```bash
sudo ufw allow 8081/tcp
```

### Port Forwarding

If your car creates a WiFi hotspot and the tracker connects to it:
1. The Android phone connects to the car via Android Auto (USB or wireless)
2. The tracker connects to the car's WiFi hotspot
3. Both devices are on the same network - no port forwarding needed

## Database Schema

GPS positions from Android Auto are stored in the `gps_positions` table:

```sql
CREATE TABLE gps_positions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    altitude REAL,
    speed REAL,
    heading REAL,
    accuracy REAL,
    source TEXT DEFAULT 'external',  -- 'android_auto' for shared GPS
    created_at INTEGER DEFAULT (strftime('%s', 'now'))
);
```

When the tracker records FT8 decodes, it automatically uses the most recent GPS position (either local USB GPS or Android GPS) to tag each decode with location data.

## API Endpoint

The tracker exposes an HTTP endpoint for GPS updates:

**POST** `http://<tracker-ip>:8081/gps`

Request body:
```json
{
  "latitude": 47.123456,
  "longitude": -122.654321,
  "altitude": 150.0,
  "speed": 65.5,
  "heading": 90.0,
  "timestamp": 1703012345,
  "source": "android_auto"
}
```

Response (200 OK):
```json
{
  "status": "ok",
  "message": "GPS position received"
}
```

## Troubleshooting

### Android App Not Uploading

1. Check location permissions are granted
2. Verify the host IP address is correct
3. Ensure you're on the same network as the tracker
4. Check the "Share GPS with Tracker" toggle is enabled

### Tracker Not Receiving GPS

1. Verify the tracker is running: `ps aux | grep main.py`
2. Check firewall: `sudo ufw status`
3. Test connectivity: `curl http://<tracker-ip>:8081/health` from another device
4. Check tracker logs for errors

### GPS Data Not Used in Decodes

The tracker will use Android GPS data as a fallback when:
- Local USB GPS is not connected, OR
- Local USB GPS has no fix

Priority: Local GPS > Android GPS > No GPS

## Benefits

- **No USB GPS Required** - Saves cost and installation effort
- **Vehicle GPS Accuracy** - Often more accurate than USB GPS modules
- **Automatic Updates** - GPS shares automatically when connected
- **Seamless Fallback** - Works alongside existing USB GPS if present
- **Low Bandwidth** - Only ~200 bytes per update every 30 seconds
