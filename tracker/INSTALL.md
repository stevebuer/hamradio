# Installation Guide

## Prerequisites

### Hardware
- Pine64 or Rock64 single board computer
- Yaesu FT-891 (or compatible HF transceiver)
- USB GPS receiver (VK-172, BU-353, etc.)
- Audio isolation transformer interface
- SD card (16GB+ recommended)
- 12V power supply

### Software
- Armbian (Debian 11 Bullseye) or similar Linux distribution
- Python 3.7+
- WSJT-X or compatible FT8 decoder
- GPS daemon (gpsd)

## Step-by-Step Installation

### 1. Prepare the Pine64

#### Install Armbian
```bash
# Download Armbian for your board from:
# https://www.armbian.com/download/

# Flash to SD card using Etcher or dd
sudo dd if=Armbian_*.img of=/dev/sdX bs=4M status=progress
sync
```

#### First Boot
```bash
# SSH into the board (default: root/1234)
ssh root@<pine64-ip>

# Follow first-run setup prompts
# Create user account (steve recommended for matching configs)
# Set timezone
# Enable WiFi if needed
```

#### Update System
```bash
sudo apt-get update
sudo apt-get upgrade -y
```

### 2. Install System Dependencies

```bash
# Install required packages
sudo apt-get install -y \
    python3 \
    python3-pip \
    python3-venv \
    gpsd \
    gpsd-clients \
    python3-gps \
    alsa-utils \
    netcat \
    sqlite3 \
    git \
    build-essential

# Optional: Install WSJT-X
sudo apt-get install -y wsjtx
```

### 3. Setup GPS

#### Connect USB GPS
```bash
# Plug in USB GPS receiver
# Check if detected
dmesg | tail
ls -l /dev/ttyACM* /dev/ttyUSB*

# Typical devices:
# /dev/ttyACM0 - VK-172
# /dev/ttyUSB0 - BU-353
```

#### Configure gpsd
```bash
# Edit gpsd config
sudo nano /etc/default/gpsd

# Set:
START_DAEMON="true"
GPSD_OPTIONS="-n"
DEVICES="/dev/ttyACM0"  # Your GPS device
USBAUTO="true"

# Restart gpsd
sudo systemctl enable gpsd
sudo systemctl restart gpsd

# Test GPS
gpspipe -w -n 10
cgps
```

### 4. Setup Audio

#### Identify Audio Device
```bash
# List audio devices
arecord -l

# Typical output:
# card 0: Device [USB Audio Device], device 0: USB Audio [USB Audio]
# Use as: hw:0,0
```

#### Test Audio Recording
```bash
# Record 10 seconds
arecord -D hw:0,0 -r 12000 -f S16_LE -d 10 test.wav

# Play back
aplay test.wav

# Check levels
alsamixer
```

#### Audio Interface Wiring
```
Radio Audio Out → Isolation Transformer → Pine64 Audio In
Radio PTT → (Not needed for FT8 receive-only)
```

### 5. Install FT8 Tracker

#### Clone Repository
```bash
cd /home/steve/GITHUB/hamradio
# Or clone if not already there:
# git clone <repo-url> hamradio
cd tracker
```

#### Run Setup Script
```bash
chmod +x setup.sh scripts/*.sh
./setup.sh
```

The setup script will:
- Install Python dependencies
- Create virtual environment
- Setup directories
- Configure GPS
- Install systemd service

### 6. Configure WSJT-X

#### Install WSJT-X (if not from package manager)
```bash
# Download from https://physics.princeton.edu/pulsar/k1jt/wsjtx.html
# Or build from source:
cd /tmp
git clone https://git.code.sf.net/p/wsjt/wsjtx
cd wsjtx
mkdir build && cd build
cmake ..
make -j4
sudo make install
```

#### Configure WSJT-X for Headless Operation
```bash
# Run WSJT-X once to create config
wsjtx &

# Configure:
# - Audio input: Select your device
# - Radio: None (if not using CAT control)
# - Reporting: Disable (or configure for PSK Reporter)

# Enable logging
# File → Settings → General
# Check "Enable logging"

# The log file will be at:
# ~/.local/share/WSJT-X/ALL.TXT
```

### 7. Configure FT8 Tracker

#### Edit Configuration
```bash
cd /home/steve/GITHUB/hamradio/tracker
nano config/tracker.conf
```

Key settings to change:
```ini
[gps]
device = /dev/ttyACM0  # Your GPS device

[ft8]
log_file = /home/steve/.local/share/WSJT-X/ALL.TXT

[network]
server_port = 8080  # Port for Android Auto app

[iot]
enabled = true
api_key = YOUR_API_KEY_FROM_JXQZ_ORG
station = N7MKO-M  # Your callsign
```

#### Get API Key for dx.jxqz.org
```bash
# Visit https://dx.jxqz.org
# Register account
# Generate API key
# Copy to config file
```

### 8. Test the System

#### Manual Test
```bash
# Start WSJT-X in one terminal
wsjtx &

# Start tracker in another terminal
./scripts/start-tracker.sh

# Check status
./scripts/status.sh

# View logs
tail -f logs/tracker.log
```

#### Test Android Auto Connection
```bash
# From your Android device, connect to:
nc <pine64-ip> 8080

# You should see FT8 decodes streaming
```

### 9. Enable Auto-Start

#### Install Systemd Service
```bash
sudo cp systemd/ft8-tracker.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable ft8-tracker
sudo systemctl start ft8-tracker
```

#### Check Service Status
```bash
sudo systemctl status ft8-tracker
journalctl -u ft8-tracker -f
```

#### Also Enable WSJT-X Auto-Start
```bash
# Create systemd user service
mkdir -p ~/.config/systemd/user

cat > ~/.config/systemd/user/wsjtx.service << 'EOF'
[Unit]
Description=WSJT-X FT8 Decoder
After=sound.target

[Service]
Type=simple
ExecStart=/usr/bin/wsjtx
Restart=always
RestartSec=10

[Install]
WantedBy=default.target
EOF

systemctl --user enable wsjtx
systemctl --user start wsjtx
```

### 10. Vehicle Installation

#### Power
```bash
# Use 12V to 5V converter (3A+ recommended)
# Connect to vehicle battery or fuse box
# Add inline fuse (5A)
```

#### Mounting
- Secure Pine64 in trunk
- Ensure good ventilation
- Protect from moisture
- Mount GPS receiver with sky view (under parcel shelf, etc.)

#### Radio Connection
- Connect audio cable from radio to Pine64
- Secure cables to prevent disconnection
- Test in vehicle before final installation

### 11. Android Auto Setup

#### On Your Android Device
1. Install the FT8 Auto Display app (see ../android-auto/)
2. Configure connection:
   - Host: Pine64 IP address
   - Port: 8080
3. Connect and verify decodes appear

#### Network Options
- **Option A**: Phone connects to Pine64 via WiFi hotspot
- **Option B**: Both on same WiFi network
- **Option C**: Phone connects via mobile data + VPN

## Troubleshooting

### GPS Not Working
```bash
# Check device
ls -l /dev/ttyACM*

# Check gpsd
sudo systemctl status gpsd
gpspipe -w -n 10

# Restart gpsd
sudo systemctl restart gpsd
```

### No Audio Input
```bash
# List devices
arecord -l

# Test recording
arecord -D hw:0,0 -r 12000 -f S16_LE -d 5 test.wav

# Check mixer
alsamixer
```

### WSJT-X Not Decoding
```bash
# Check if running
ps aux | grep wsjtx

# Check audio levels (should see waveform)
# Check frequency is correct
# Ensure radio is on FT8 frequency
```

### Tracker Not Starting
```bash
# Check logs
tail -f logs/tracker.log

# Check dependencies
source venv/bin/activate
pip install -r requirements.txt

# Run manually with verbose
python3 src/main.py -c config/tracker.conf -v
```

### No Network Clients
```bash
# Check if server is listening
netstat -ln | grep 8080

# Test connection
echo "test" | nc <pine64-ip> 8080

# Check firewall (usually disabled on Armbian)
sudo iptables -L
```

## Performance Tuning

### CPU Governor
```bash
# For better performance
echo performance | sudo tee /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
```

### Swap File (if low memory)
```bash
sudo fallocate -l 1G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
# Add to /etc/fstab for persistence
```

### Log Rotation
```bash
# Add to /etc/logrotate.d/ft8-tracker
sudo nano /etc/logrotate.d/ft8-tracker

# Add:
/home/steve/GITHUB/hamradio/tracker/logs/*.log {
    daily
    rotate 7
    compress
    missingok
    notifempty
}
```

## Next Steps

- Configure bands in WSJT-X
- Test in vehicle
- Setup Android Auto display
- Monitor uploads to dx.jxqz.org
- Tune for your specific radio

## Support

See README.md for additional documentation.

73!
