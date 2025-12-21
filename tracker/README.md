# Pine64 Mobile HF Tracker

A mobile HF monitoring system running on Pine64 (or Rock64) single board computer. Decodes FT8 signals, logs GPS position, stores data locally, serves to Android Auto display, and uploads to IoT server when internet is available.

## Hardware

- **SBC**: Pine64 or Rock64 running Armbian (Debian 11 Bullseye)
- **Radio**: Yaesu FT-891 transceiver (or similar HF transceiver)
- **Audio Interface**: Custom isolation transformer board feeding into audio jack
- **GPS**: USB GPS receiver (e.g., VK-172, BU-353)
- **Installation**: Mounted in vehicle trunk

## Features

### Current
- ✅ FT8 signal decoding on all HF bands (80m-10m)
- ✅ GPS position logging with each decode
- ✅ Local SQLite database storage
- ✅ TCP server for Android Auto app
- ✅ Beacon server uploader to dx.jxqz.org (when internet available)
- ✅ Automatic band detection
- ✅ Timestamp synchronization with GPS

### Future
- 🔲 HF APRS over AX.25 (30m band primarily)
- 🔲 WSPR decoding
- 🔲 Multiple simultaneous band monitoring
- 🔲 RabbitMQ message broker integration (enhanced IoT architecture)
- 🔲 Web dashboard for real-time tracking
- 🔲 Multi-station network coordination

## Architecture

```
┌─────────────────┐
│  Yaesu FT-891   │
│   (HF Radio)    │
└────────┬────────┘
         │ Audio
         ↓
┌─────────────────┐     ┌──────────────┐
│  Audio Input    │     │   USB GPS    │
│  (Isolation     │     │   Receiver   │
│   Transformer)  │     └──────┬───────┘
└────────┬────────┘            │
         │                     │
         ↓                     ↓
┌──────────────────────────────────┐
│        Pine64 / Rock64           │
│  ┌────────────────────────────┐  │
│  │   FT8 Decoder (WSJT-X)     │  │
│  │      or ft8_lib            │  │
│  └──────────┬─────────────────┘  │
│             ↓                     │
│  ┌────────────────────────────┐  │
│  │   Tracker Service          │  │
│  │   - GPS Integration        │  │
│  │   - Database Storage       │  │
│  │   - Network Server         │  │
│  │   - IoT Uploader          │  │
│  └──────────┬─────────────────┘  │
└─────────────┼────────────────────┘
              │
    ┌─────────┴─────────┐
    ↓                   ↓
┌─────────┐      ┌──────────────┐
│ Android │      │  dx.jxqz.org │
│  Auto   │      │Beacon Server │
│  App    │      └──────────────┘
└─────────┘
```

## Project Structure

```
tracker/
├── README.md                   # This file
├── INSTALL.md                  # Installation instructions
├── CONFIG.md                   # Configuration guide
├── requirements.txt            # Python dependencies
├── setup.sh                    # Setup script
├── config/
│   ├── tracker.conf            # Main configuration
│   ├── bands.conf              # HF band frequencies
│   └── ft8_decoder.conf        # FT8 decoder settings
├── src/
│   ├── main.py                 # Main service entry point
│   ├── ft8_decoder.py          # FT8 decoder interface
│   ├── gps_handler.py          # GPS integration
│   ├── database.py             # SQLite database layer
│   ├── network_server.py       # TCP server for Android Auto
│   ├── iot_uploader.py         # Upload to dx.jxqz.org
│   ├── band_monitor.py         # Band monitoring and switching
│   └── utils.py                # Utility functions
├── systemd/
│   ├── ft8-tracker.service     # Systemd service
│   └── ft8-tracker-uploader.service
├── scripts/
│   ├── start-tracker.sh        # Start service manually
│   ├── stop-tracker.sh         # Stop service
│   ├── status.sh               # Check status
│   └── test-decode.sh          # Test FT8 decoding
├── data/
│   └── tracker.db              # SQLite database (generated)
├── logs/
│   └── tracker.log             # Log files (generated)
└── java-lib/                   # Legacy (to be deprecated)
```

## Quick Start

### 1. Install Dependencies

```bash
cd /home/steve/GITHUB/hamradio/tracker
./setup.sh
```

### 2. Configure

Edit `config/tracker.conf`:
- GPS device path
- Audio input device
- IoT server credentials
- Network server port

### 3. Start Service

```bash
# Manual start (for testing)
./scripts/start-tracker.sh

# Or enable systemd service
sudo systemctl enable ft8-tracker
sudo systemctl start ft8-tracker
```

### 4. Connect Android Auto

In your Android Auto app, configure connection to:
- IP: Pine64 IP address
- Port: 8080 (default)

## Data Flow

1. **FT8 Decoding**: WSJT-X or ft8_lib decodes audio from radio
2. **GPS Position**: USB GPS provides current location
3. **Database**: Decode + GPS + timestamp stored in SQLite
4. **Android Auto**: TCP server sends recent decodes to phone app
5. **Beacon Server**: When internet available, uploads to dx.jxqz.org beacon server

## FT8 Decoder Options

### Option 1: WSJT-X (Recommended for Pi/Pine64)
- Full-featured
- Proven reliability
- Can run headless
- Parses output log

### Option 2: ft8_lib
- Lightweight
- Python bindings available
- Good for embedded systems

### Option 3: JFTX8
- Java implementation
- Integrates with existing Java code

See [INSTALL.md](INSTALL.md) for decoder setup instructions.

## GPS Integration

Supports standard USB GPS receivers:
- VK-172 (u-blox 7)
- BU-353S4
- GlobalSat BU-353
- Any NMEA-compatible GPS

GPS provides:
- Latitude/Longitude
- Altitude
- Speed
- Heading
- Timestamp (for system time sync)

## Database Schema

### `decodes` table
```sql
CREATE TABLE decodes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER NOT NULL,
    callsign TEXT,
    grid TEXT,
    snr INTEGER,
    frequency INTEGER,
    band TEXT,
    message TEXT,
    latitude REAL,
    longitude REAL,
    altitude REAL,
    speed REAL,
    uploaded INTEGER DEFAULT 0
);
```

## Network Protocol

### Android Auto Server
- Protocol: TCP
- Port: 8080 (configurable)
- Format: Line-based, newline-delimited
- Message: `HHMMSS SNR DT FREQ ~ MESSAGE`

### IoT Upload (dx.jxqz.org)
- Protocol: HTTPS POST
- Format: JSON
- Authentication: API key
- Batched uploads when internet available

Example JSON:
```json
{
  "station": "N7MKO-M",
  "decodes": [
    {
      "timestamp": 1703012345,
      "callsign": "K1ABC",
      "grid": "FN42",
      "snr": -12,
      "frequency": 7074000,
      "band": "40m",
      "message": "CQ K1ABC FN42",
      "position": {
        "lat": 47.1234,
        "lon": -122.5678,
        "alt": 150,
        "speed": 65.5
      }
    }
  ]
}
```

## Configuration

### config/tracker.conf
```ini
[gps]
device = /dev/ttyACM0
baud = 9600
timeout = 10

[audio]
device = hw:0,0
sample_rate = 12000

[ft8]
decoder = wsjtx
decoder_path = /usr/bin/wsjtx
bands = 80m,40m,30m,20m,17m,15m,12m,10m

[database]
path = /home/steve/GITHUB/hamradio/tracker/data/tracker.db

[network]
server_enabled = true
server_port = 8080
server_bind = 0.0.0.0

[iot]
enabled = true
server = https://dx.jxqz.org
api_key = YOUR_API_KEY_HERE
station = N7MKO-M
upload_interval = 300
batch_size = 100

[logging]
level = INFO
file = /home/steve/GITHUB/hamradio/tracker/logs/tracker.log
```

## Systemd Service

The tracker runs as a systemd service for automatic startup and monitoring.

```bash
# Enable on boot
sudo systemctl enable ft8-tracker

# Start/stop/restart
sudo systemctl start ft8-tracker
sudo systemctl stop ft8-tracker
sudo systemctl restart ft8-tracker

# View logs
journalctl -u ft8-tracker -f
```

## Future: HF APRS Support

When HF APRS is implemented:
- Monitor 10.1478 MHz (30m APRS frequency)
- Decode AX.25 packets
- Same GPS/storage/upload pipeline
- Combined FT8 + APRS display

## Troubleshooting

### No GPS fix
```bash
# Check GPS device
ls -l /dev/ttyACM* /dev/ttyUSB*

# Test GPS
gpspipe -r -n 10
```

### No audio input
```bash
# List audio devices
arecord -l

# Test recording
arecord -D hw:0,0 -r 12000 -f S16_LE -d 10 test.wav
```

### FT8 not decoding
```bash
# Check WSJT-X is running
ps aux | grep wsjtx

# Check audio levels
alsamixer
```

### Network connectivity
```bash
# Test Android Auto connection
echo "Test message" | nc <android-ip> 8080

# Check service status
./scripts/status.sh
```

## Performance

On Pine64/Rock64:
- CPU usage: ~15-25% (single band FT8)
- Memory: ~150MB
- Storage: ~1MB per day (compressed database)
- Power: ~2-3W typical

## Future Architecture Enhancements

### RabbitMQ Message Broker (Planned)

For improved scalability and IoT best practices, future versions may incorporate RabbitMQ as a message broker between mobile stations and the IoT server:

**Benefits:**
- **Resilience**: Messages persist in queue during server maintenance
- **Decoupling**: Mobile units and server operate independently
- **Scalability**: Multiple consumers can process same data stream
- **Reliability**: Built-in acknowledgment and retry mechanisms
- **Flexibility**: Easy to add new data consumers (analytics, alerts, etc.)

**Architecture:**
```
Mobile Tracker → RabbitMQ Exchange → Multiple Queues → Consumers
                                            ↓
                                    - Database Writer
                                    - Real-time Dashboard
                                    - Analytics Engine
                                    - Alert System
```

**Implementation Considerations:**
- AMQP protocol over TLS for secure communication
- Dead letter queues for failed message handling
- Message TTL for storage management
- Exchange routing for multi-station networks

This approach demonstrates modern IoT messaging patterns while maintaining the simplicity of the current HTTP implementation for initial deployment.

## License

Open source for ham radio use.

## Credits

- Uses WSJT-X for FT8 decoding
- gpsd for GPS integration
- Python 3 with asyncio

## 73!

Mobile HF monitoring for the 21st century!

---

**See also:**
- [INSTALL.md](INSTALL.md) - Installation instructions
- [CONFIG.md](CONFIG.md) - Configuration details
- [android-auto/](../android-auto/) - Android Auto display app
