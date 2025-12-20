# Configuration Guide

## Overview

The FT8 tracker is configured through `config/tracker.conf`. This file uses INI format with sections for each component.

## Configuration Sections

### [gps]
GPS receiver configuration.

```ini
[gps]
device = /dev/ttyACM0   # GPS serial device path
baud = 9600             # Baud rate (usually 9600 or 4800)
timeout = 10            # Timeout for GPS operations
```

**Finding Your GPS Device:**
```bash
# List USB serial devices
ls -l /dev/ttyACM* /dev/ttyUSB*

# Check dmesg for device info
dmesg | grep -i "tty"

# Common devices:
# /dev/ttyACM0 - VK-172, u-blox based GPS
# /dev/ttyUSB0 - Prolific or FTDI based GPS
```

### [audio]
Audio input configuration.

```ini
[audio]
device = hw:0,0         # ALSA device identifier
sample_rate = 12000     # Sample rate for FT8 (12kHz typical)
```

**Finding Your Audio Device:**
```bash
# List audio capture devices
arecord -l

# Output example:
# card 0: Device [USB Audio Device], device 0: USB Audio [USB Audio]
# Use as: hw:0,0 (card 0, device 0)

# Test recording
arecord -D hw:0,0 -r 12000 -f S16_LE -d 5 test.wav
```

### [ft8]
FT8 decoder configuration.

```ini
[ft8]
decoder = wsjtx                                              # Decoder type: wsjtx or ft8_lib
decoder_path = /usr/bin/wsjtx                                # Path to WSJT-X binary
log_file = /home/steve/.local/share/WSJT-X/ALL.TXT           # WSJT-X log file
bands = 80m,40m,30m,20m,17m,15m,12m,10m                      # Bands to monitor
```

**Decoder Options:**
- `wsjtx`: Full WSJT-X software (recommended)
- `ft8_lib`: Lightweight library (future)

**WSJT-X Log Location:**
- Linux: `~/.local/share/WSJT-X/ALL.TXT`
- Windows: `%LOCALAPPDATA%\WSJT-X\ALL.TXT`
- Mac: `~/Library/Application Support/WSJT-X/ALL.TXT`

### [database]
SQLite database configuration.

```ini
[database]
path = /home/steve/GITHUB/hamradio/tracker/data/tracker.db
```

**Database Management:**
```bash
# View database
sqlite3 data/tracker.db

# Get stats
sqlite3 data/tracker.db "SELECT COUNT(*) FROM decodes;"

# Clean old records
sqlite3 data/tracker.db "DELETE FROM decodes WHERE uploaded=1 AND timestamp < $(date -d '30 days ago' +%s);"
```

### [network]
TCP server for Android Auto app.

```ini
[network]
server_enabled = true    # Enable/disable network server
server_port = 8080       # TCP port to listen on
server_bind = 0.0.0.0    # Bind address (0.0.0.0 = all interfaces)
```

**Network Configuration:**
- Use `0.0.0.0` to listen on all interfaces
- Use `127.0.0.1` for localhost only
- Use specific IP for one interface only

**Testing:**
```bash
# Test from another machine
nc <pine64-ip> 8080

# Check if listening
netstat -ln | grep 8080
```

### [iot]
IoT uploader for dx.jxqz.org.

```ini
[iot]
enabled = true                      # Enable/disable uploads
server = https://dx.jxqz.org        # IoT server URL
api_key = YOUR_API_KEY_HERE         # Your API key from dx.jxqz.org
station = N7MKO-M                   # Your callsign (with /M for mobile)
upload_interval = 300               # Upload interval in seconds (300 = 5 min)
batch_size = 100                    # Max decodes per upload
```

**Getting an API Key:**
1. Visit https://dx.jxqz.org
2. Register or log in
3. Go to Settings → API Keys
4. Generate new key
5. Copy to config file

**Upload Strategy:**
- Decodes are queued in local database
- Uploads happen when internet is available
- Failed uploads are retried
- Backoff on repeated failures

### [logging]
Logging configuration.

```ini
[logging]
level = INFO              # Log level: DEBUG, INFO, WARNING, ERROR
file = /home/steve/GITHUB/hamradio/tracker/logs/tracker.log
```

**Log Levels:**
- `DEBUG`: Verbose output, every decode, GPS update
- `INFO`: Normal output, decodes, status updates
- `WARNING`: Warnings and errors only
- `ERROR`: Errors only

**Viewing Logs:**
```bash
# Tail log file
tail -f logs/tracker.log

# View systemd logs
journalctl -u ft8-tracker -f

# Search logs
grep "ERROR" logs/tracker.log
```

## Example Configurations

### Home Station Setup
Monitor one band, upload to internet:
```ini
[ft8]
bands = 20m

[network]
server_enabled = true
server_port = 8080

[iot]
enabled = true
upload_interval = 60
```

### Mobile Setup
Monitor multiple bands, delay uploads:
```ini
[ft8]
bands = 40m,20m,17m,15m

[network]
server_enabled = true

[iot]
enabled = true
upload_interval = 600  # Upload every 10 minutes
batch_size = 200
```

### Testing/Development
Local only, no uploads:
```ini
[gps]
device = /dev/null  # Will use dummy GPS

[network]
server_enabled = true
server_bind = 127.0.0.1  # Localhost only

[iot]
enabled = false  # No uploads

[logging]
level = DEBUG  # Verbose logging
```

## Band Configuration

### HF FT8 Frequencies
Common FT8 frequencies by band:

| Band | Frequency | Dial Frequency |
|------|-----------|----------------|
| 80m  | 3.573 MHz | 3.571 MHz |
| 60m  | 5.357 MHz | 5.355 MHz |
| 40m  | 7.074 MHz | 7.072 MHz |
| 30m  | 10.136 MHz | 10.134 MHz |
| 20m  | 14.074 MHz | 14.072 MHz |
| 17m  | 18.100 MHz | 18.098 MHz |
| 15m  | 21.074 MHz | 21.072 MHz |
| 12m  | 24.915 MHz | 24.913 MHz |
| 10m  | 28.074 MHz | 28.072 MHz |

Configure your radio to these frequencies and WSJT-X will decode.

## Security Considerations

### Network Security
```ini
# Bind to specific interface only
server_bind = 192.168.1.100

# Or use firewall
sudo iptables -A INPUT -p tcp --dport 8080 -s 192.168.1.0/24 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 8080 -j DROP
```

### API Key Protection
```bash
# Protect config file
chmod 600 config/tracker.conf

# Never commit API keys to git
echo "config/tracker.conf" >> .gitignore
```

## Environment Variables

You can override config settings with environment variables:

```bash
export FT8_GPS_DEVICE=/dev/ttyUSB0
export FT8_SERVER_PORT=9090
export FT8_IOT_API_KEY=your_key_here

./scripts/start-tracker.sh
```

## Validation

Test your configuration:

```bash
# Validate config file syntax
python3 -c "import configparser; c=configparser.ConfigParser(); c.read('config/tracker.conf'); print('Config OK')"

# Test GPS
gpspipe -w -n 5

# Test audio
arecord -D hw:0,0 -r 12000 -f S16_LE -d 2 test.wav && aplay test.wav

# Test network
nc -l 8080 &
echo "test" | nc localhost 8080
```

## Troubleshooting

### "Permission denied" on GPS device
```bash
# Add user to dialout group
sudo usermod -a -G dialout $USER
# Log out and back in
```

### "Cannot open audio device"
```bash
# Check device exists
arecord -l

# Check permissions
ls -l /dev/snd/*

# Add user to audio group
sudo usermod -a -G audio $USER
```

### "Port already in use"
```bash
# Find process using port
sudo netstat -tulpn | grep 8080

# Kill process
sudo kill <pid>
```

### "API key invalid"
- Double-check API key in config
- Verify key is active on dx.jxqz.org
- Check for extra spaces or quotes

## Performance Tuning

### For Low-Power Devices
```ini
[iot]
upload_interval = 600  # Upload less frequently
batch_size = 200      # Larger batches

[logging]
level = WARNING  # Less logging overhead
```

### For High-Traffic Monitoring
```ini
[database]
# Regular cleanup
# Add to cron:
# 0 3 * * * sqlite3 /path/to/tracker.db "DELETE FROM decodes WHERE uploaded=1 AND timestamp < $(date -d '30 days ago' +%s);"

[iot]
batch_size = 500  # Larger batches
upload_interval = 300  # More frequent uploads
```

## See Also

- [INSTALL.md](INSTALL.md) - Installation instructions
- [README.md](README.md) - Project overview
- [../android-auto/](../android-auto/) - Android Auto app configuration
