# Pine64 Mobile HF Tracker - Requirements

## Hardware Requirements

### Required
- **SBC**: Pine64 or Rock64 (1GB+ RAM recommended)
- **Radio**: Yaesu FT-891 or compatible HF transceiver
- **GPS**: USB GPS receiver (VK-172, BU-353, or compatible)
- **Audio Interface**: Isolation transformer board (radio audio → Pine64 audio in)
- **Storage**: 16GB+ SD card
- **Power**: 12V to 5V converter (3A+ recommended)

### Optional
- Heat sinks for Pine64/Rock64
- Case for Pine64
- External antenna for GPS (if mounting in trunk)

## Software Requirements

### Operating System
- Armbian (Debian 11 Bullseye) or compatible Linux
- Python 3.7 or newer

### System Packages
- `gpsd` and `gpsd-clients` - GPS daemon
- `python3-gps` - GPS Python bindings
- `alsa-utils` - Audio utilities
- `sqlite3` - Database
- `wsjtx` - FT8 decoder (or compatible)

### Python Packages
See `requirements.txt`:
- `gps3` - GPS integration
- `requests` - HTTP client for IoT uploads

## Network Requirements

### For Android Auto Connection
- WiFi or mobile hotspot on Pine64
- TCP port 8080 accessible from Android device

### For IoT Uploads
- Internet connection (WiFi, mobile hotspot, or cellular)
- Outbound HTTPS access to dx.jxqz.org
- API key from dx.jxqz.org

## Feature Status

### Implemented ✅
- FT8 decoding (via WSJT-X)
- GPS position tracking
- Local SQLite database storage
- TCP server for Android Auto
- IoT uploads to dx.jxqz.org
- Automatic reconnection
- Systemd service integration

### Planned 🔲
- HF APRS over AX.25 (30m band)
- Direct FT8 decoding (ft8_lib)
- WSPR mode support
- Web interface for configuration

## See Also

- [README.md](README.md) - Project overview
- [INSTALL.md](INSTALL.md) - Installation guide
- [CONFIG.md](CONFIG.md) - Configuration details
