# FT8 Data Source Configuration Examples

## Option 1: WSJT-X on Linux

### Real-time forwarding
```bash
#!/bin/bash
# forward-wsjtx.sh - Forward WSJT-X decodes to Android

ANDROID_IP="192.168.1.100"  # Your Android device IP
ANDROID_PORT="8080"

# WSJT-X ALL.TXT log file location
WSJTX_LOG="$HOME/.local/share/WSJT-X/ALL.TXT"

echo "Forwarding WSJT-X decodes to $ANDROID_IP:$ANDROID_PORT"

tail -f "$WSJTX_LOG" | while read line; do
    echo "$line" | nc -w 1 $ANDROID_IP $ANDROID_PORT
done
```

## Option 2: WSJT-X on Windows

### PowerShell script
```powershell
# forward-wsjtx.ps1
$androidIP = "192.168.1.100"
$androidPort = 8080
$wsjtxLog = "$env:LOCALAPPDATA\WSJT-X\ALL.TXT"

$tail = Get-Content $wsjtxLog -Wait -Tail 0
$tail | ForEach-Object {
    $tcpClient = New-Object System.Net.Sockets.TcpClient($androidIP, $androidPort)
    $stream = $tcpClient.GetStream()
    $writer = New-Object System.IO.StreamWriter($stream)
    $writer.WriteLine($_)
    $writer.Flush()
    $writer.Close()
    $tcpClient.Close()
}
```

## Option 3: Raspberry Pi with RTL-SDR

### Setup script
```bash
#!/bin/bash
# rpi-ft8-forwarder.sh - Raspberry Pi FT8 decoder and forwarder

ANDROID_IP="192.168.1.100"
ANDROID_PORT="8080"
FREQ="7074000"  # 40m FT8

# Install dependencies (run once)
# sudo apt-get install rtl-sdr wsjtx

# Start WSJT-X in headless mode (if available)
# Or use ft8call, wsprd, etc.

# Forward decodes
tail -f ~/.local/share/WSJT-X/ALL.TXT | \
    grep -v "^$" | \
    while read line; do
        echo "$line" | nc -w 1 $ANDROID_IP $ANDROID_PORT
    done
```

## Option 4: FT8Call

```bash
#!/bin/bash
# Forward FT8Call decodes

ANDROID_IP="192.168.1.100"
ANDROID_PORT="8080"

# Monitor FT8Call's output and forward
tail -f /path/to/ft8call/decodes.txt | \
    while read line; do
        echo "$line" | nc -w 1 $ANDROID_IP $ANDROID_PORT
    done
```

## Option 5: Simple Network Server

If you want the Android device to connect TO your decoder (instead of decoder connecting to Android):

### Python server
```python
#!/usr/bin/env python3
# ft8-server.py - Simple TCP server for FT8 decodes

import socket
import time
import sys

PORT = 8080

def serve_ft8_decodes(log_file):
    """Serve FT8 decodes from a log file to connected clients"""
    
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(('0.0.0.0', PORT))
    server.listen(5)
    
    print(f"FT8 Server listening on port {PORT}")
    print(f"Monitoring: {log_file}")
    
    with open(log_file, 'r') as f:
        # Seek to end of file
        f.seek(0, 2)
        
        while True:
            # Accept connection (non-blocking would be better)
            server.settimeout(1.0)
            try:
                client, addr = server.accept()
                print(f"Client connected: {addr}")
                
                # Send new lines as they appear
                while True:
                    line = f.readline()
                    if line:
                        client.send(line.encode())
                    else:
                        time.sleep(0.5)
                        
            except socket.timeout:
                continue
            except Exception as e:
                print(f"Error: {e}")
                continue

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: ft8-server.py <log_file>")
        sys.exit(1)
    
    serve_ft8_decodes(sys.argv[1])
```

Usage:
```bash
python3 ft8-server.py ~/.local/share/WSJT-X/ALL.TXT
```

## Option 6: WSJT-X UDP Protocol

WSJT-X can send UDP packets. You could create a UDP-to-TCP bridge:

```python
#!/usr/bin/env python3
# wsjtx-udp-bridge.py - Convert WSJT-X UDP to TCP

import socket
import struct

WSJTX_UDP_PORT = 2237
TCP_PORT = 8080

# Listen for WSJT-X UDP packets
udp_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
udp_sock.bind(('0.0.0.0', WSJTX_UDP_PORT))

# TCP server for Android client
tcp_server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
tcp_server.bind(('0.0.0.0', TCP_PORT))
tcp_server.listen(1)

print(f"Listening for WSJT-X UDP on port {WSJTX_UDP_PORT}")
print(f"Serving TCP on port {TCP_PORT}")

# Accept Android client
client, addr = tcp_server.accept()
print(f"Android client connected: {addr}")

# Forward UDP decode packets to TCP client
while True:
    data, addr = udp_sock.recvfrom(1024)
    # Parse WSJT-X UDP format (simplified)
    # Send to Android
    client.send(data)
```

## Option 7: Test Data Generator

For testing without actual FT8 decodes:

```bash
#!/bin/bash
# test-generator.sh - Generate fake FT8 decodes for testing

ANDROID_IP="192.168.1.100"
PORT="8080"

while true; do
    TIME=$(date +%H%M%S)
    SNR=$((RANDOM % 40 - 20))
    DT=$(echo "scale=1; ($RANDOM % 20 - 10) / 10" | bc)
    FREQ=$((RANDOM % 3000 + 500))
    
    # Random callsign and grid
    CALLS=("K1ABC" "W2XYZ" "N7MKO" "VE3XYZ" "G4ABC")
    GRIDS=("FN42" "CN87" "DN06" "EM12" "IO91")
    
    CALL=${CALLS[$RANDOM % ${#CALLS[@]}]}
    GRID=${GRIDS[$RANDOM % ${#GRIDS[@]}]}
    
    MSG="CQ $CALL $GRID"
    DECODE="$TIME $SNR  $DT $FREQ ~ $MSG"
    
    echo "$DECODE"
    echo "$DECODE" | nc -w 1 $ANDROID_IP $PORT
    
    sleep $((RANDOM % 3 + 2))
done
```

## Data Format Examples

### WSJT-X ALL.TXT format
```
134500 -12  0.3 1234 ~  CQ K1ABC FN42
134515  -8  0.1 1456 ~  K1ABC W2XYZ FN31
134530  +2  0.5 1890 ~  W2XYZ K1ABC R-15
134545 -15  0.2 2100 ~  K1ABC W2XYZ RRR
135000  -5  0.4 1678 ~  W2XYZ K1ABC 73
```

### Simple format (custom)
```
K1ABC FN42 -12
W2XYZ FN31 -8
N7MKO DN06 +2
```

## Network Configuration

### Find your Android device IP
On Android:
- Settings > About Phone > Status > IP Address
- Or use app like "Network Info II"

### Test connectivity
```bash
# From decoder computer, test if Android is reachable
ping <android-ip>

# Test if app is listening (if app is server mode)
telnet <android-ip> 8080
```

### Firewall rules
If using Android hotspot or restrictive network:
```bash
# Allow incoming on port 8080 (if needed)
# Android doesn't usually need this for client mode
```

## Troubleshooting

### Connection refused
- Check Android device IP
- Ensure app is running and service started
- Check network (same WiFi/subnet)
- Verify port number (default 8080)

### No data appearing
- Test with test-sender.sh first
- Check data format matches expected patterns
- Look at logcat: `adb logcat | grep FT8`

### Data appears but not parsed
- Check format in FT8Parser.kt
- May need to adjust regex patterns
- Try simple format first

## Advanced: Bluetooth Serial

For Bluetooth serial connection (future enhancement):

```kotlin
// In Android app - connect to Bluetooth serial device
// Requires Bluetooth permissions and pairing
// Similar to USB serial but uses BluetoothSocket
```

## Recommended Setup

For best results:
1. Use Raspberry Pi with RTL-SDR at home/shack
2. Run WSJT-X or similar FT8 decoder
3. Forward decodes to Android device over WiFi
4. Android device in car connected to Android Auto

This gives you real-time FT8 monitoring in your car from your home station!
