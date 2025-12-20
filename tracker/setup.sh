#!/bin/bash
# Setup script for Pine64 FT8 Tracker

set -e

echo "========================================="
echo "Pine64 FT8 Tracker Setup"
echo "========================================="
echo ""

# Check if running on correct platform
if [[ ! -f /etc/armbian-release ]]; then
    echo "Warning: This script is designed for Armbian (Pine64/Rock64)"
    echo "It may work on other systems, but YMMV"
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Update system
echo "Step 1: Updating system..."
sudo apt-get update

# Install dependencies
echo ""
echo "Step 2: Installing dependencies..."
sudo apt-get install -y \
    python3 \
    python3-pip \
    python3-venv \
    gpsd \
    gpsd-clients \
    python3-gps \
    alsa-utils \
    netcat \
    sqlite3

# Install WSJT-X (optional, can also be built from source)
echo ""
echo "Step 3: Installing WSJT-X..."
read -p "Install WSJT-X from package manager? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    sudo apt-get install -y wsjtx || echo "WSJT-X not in repos, you'll need to install manually"
fi

# Create virtual environment
echo ""
echo "Step 4: Creating Python virtual environment..."
python3 -m venv venv
source venv/bin/activate

# Install Python dependencies
echo ""
echo "Step 5: Installing Python packages..."
pip install --upgrade pip
pip install -r requirements.txt

# Create directories
echo ""
echo "Step 6: Creating directories..."
mkdir -p data
mkdir -p logs
mkdir -p config

# Setup gpsd
echo ""
echo "Step 7: Configuring GPS..."
echo "Detected GPS devices:"
ls -l /dev/ttyACM* /dev/ttyUSB* 2>/dev/null || echo "No GPS device found"
echo ""
read -p "GPS device path (e.g., /dev/ttyACM0): " GPS_DEVICE

if [ -n "$GPS_DEVICE" ]; then
    echo "Configuring gpsd for $GPS_DEVICE..."
    sudo bash -c "cat > /etc/default/gpsd << EOF
START_DAEMON=\"true\"
GPSD_OPTIONS=\"-n\"
DEVICES=\"$GPS_DEVICE\"
USBAUTO=\"true\"
GPSD_SOCKET=\"/var/run/gpsd.sock\"
EOF"

    sudo systemctl enable gpsd
    sudo systemctl restart gpsd
    echo "gpsd configured and started"
else
    echo "Skipping gpsd configuration"
fi

# Test audio
echo ""
echo "Step 8: Audio configuration..."
echo "Available audio devices:"
arecord -l
echo ""
echo "Test your audio input with: arecord -D hw:0,0 -r 12000 -f S16_LE -d 5 test.wav"

# Configure tracker
echo ""
echo "Step 9: Tracker configuration..."
if [ ! -f config/tracker.conf ]; then
    echo "Configuration file already exists at config/tracker.conf"
    echo "Please edit it with your settings"
else
    echo "Default configuration created at config/tracker.conf"
fi

echo ""
echo "Don't forget to set:"
echo "  1. IoT API key for dx.jxqz.org"
echo "  2. Your station callsign"
echo "  3. GPS device path"
echo "  4. Audio device"

# Install systemd service
echo ""
read -p "Install systemd service for auto-start? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    sudo cp systemd/ft8-tracker.service /etc/systemd/system/
    sudo systemctl daemon-reload
    echo "Systemd service installed"
    echo "Enable with: sudo systemctl enable ft8-tracker"
    echo "Start with: sudo systemctl start ft8-tracker"
fi

echo ""
echo "========================================="
echo "Setup complete!"
echo "========================================="
echo ""
echo "Quick start:"
echo "  1. Edit config/tracker.conf with your settings"
echo "  2. Start tracker: ./scripts/start-tracker.sh"
echo "  3. Check logs: tail -f logs/tracker.log"
echo "  4. Connect Android Auto app to port 8080"
echo ""
echo "For manual control:"
echo "  Start: ./scripts/start-tracker.sh"
echo "  Stop:  ./scripts/stop-tracker.sh"
echo "  Status: ./scripts/status.sh"
echo ""
echo "73!"
