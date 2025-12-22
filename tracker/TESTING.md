# FT8 Tracker Testing Guide

## Overview

The FT8 Tracker supports a **test mode** that allows you to test the Android app without needing to set up all the hardware (radio, GPS, audio interface, etc.). Instead of reading from the WSJT-X log file or decoding live audio, the tracker can read pre-recorded FT8 decodes from a test file.

## Test Mode Usage

### Basic Test Mode

Run the tracker in test mode using a test data file:

```bash
python3 ./src/main.py -c config/tracker.conf --test-file test_decodes.txt
```

This will:
1. Start the tracker service normally (database, network server, GPS handler)
2. Override the FT8 decoder to use the test file instead of WSJT-X
3. Read decodes from `test_decodes.txt` and feed them to the tracker as if they were real decodes
4. Send the decodes to connected Android app clients via the network server

### Verbose Output

For debugging, add the `-v` flag to see detailed logs:

```bash
python3 ./src/main.py -c config/tracker.conf --test-file test_decodes.txt -v
```

## Test Data File Format

The test data file uses a simple text format, one decode per line:

```
HHMMSS SNR DT FREQUENCY ~ MESSAGE
```

### Field Definitions

| Field | Description | Example |
|-------|-------------|---------|
| **HHMMSS** | Time in UTC (hours, minutes, seconds) | `120000` = 12:00:00 UTC |
| **SNR** | Signal-to-Noise Ratio in dB (negative for weak, positive for strong) | `-12`, `+5`, `0` |
| **DT** | Time offset in seconds (usually ±0.5) | `0.3`, `-0.1`, `0.0` |
| **FREQUENCY** | Frequency in kHz | `7074`, `14070`, `21074` |
| **~** | Separator (literal tilde character) | `~` |
| **MESSAGE** | FT8 message content | `CQ K1ABC FN42`, `N7MKO W0AIL FM09` |

### File Format Details

- **Comments**: Lines starting with `#` are ignored
- **Blank lines**: Empty lines are skipped
- **Line format**: Must match the pattern exactly or will be skipped
- **Frequency**: Can be any value (40m band: 7070-7100, 20m band: 14070-14100, etc.)
- **Message**: Can be any FT8 message (CQ, responses, grid exchanges, etc.)

### Example Entries

```
# CQ from Colorado
120000 -12 0.3 7074 ~ CQ W0AIL EN90

# Response from Pine64 mobile tracker
120010 +5 -0.1 7076 ~ N7MKO W0AIL EN91

# Exchange of grid squares
120020 -8 0.2 7074 ~ W0AIL N7MKO FM09

# East Coast station calling CQ
120030 -15 -0.4 7075 ~ CQ K1ABC FN42
```

## Common HF FT8 Frequencies

When creating test data, use realistic frequencies:

| Band | Frequency Range (kHz) | Typical Spot |
|------|----------------------|-------------|
| 80m | 3573-3580 | 3573 |
| 60m | 5357 | 5357 (USB) |
| 40m | 7070-7100 | 7074 |
| 30m | 10130-10140 | 10136 |
| 20m | 14070-14100 | 14074 |
| 17m | 18100-18110 | 18104 |
| 15m | 21070-21100 | 21074 |
| 12m | 24915-24925 | 24915 |
| 10m | 28070-28100 | 28074 |
| 6m | 50313 | 50313 |

## Sample Test Data

A sample test file is included: `test_decodes.txt`

This file contains realistic FT8 decodes spanning several minutes with:
- Various SNR levels (weak to strong)
- Different stations and grid squares
- Multiple bands (40m, 20m)
- CQ calls and responses
- Grid square exchanges

You can use this as a template or starting point for creating your own test data.

## Testing With the Android App

1. **Start the tracker in test mode**:
   ```bash
   python3 ./src/main.py -c config/tracker.conf --test-file test_decodes.txt
   ```

2. **Note the network server details** (default: `127.0.0.1:8080` or your laptop's IP)

3. **Connect Android app** to the tracker:
   - Open Android Auto app
   - Enter the server IP and port
   - Connect

4. **Observe FT8 decodes** appearing in real-time as the test file is processed

5. **Test Android app features** without hardware:
   - Test UI responsiveness with high decode rates
   - Test band switching functionality
   - Test GPS position updates (can be simulated via network server)
   - Test network reconnection
   - Record multiple sessions with different test data

## Creating Custom Test Data

### Method 1: Record Real Decodes

1. Run WSJT-X normally and capture FT8 decodes
2. Copy lines from `~/.local/share/WSJT-X/ALL.TXT`
3. Paste into a new test file
4. Adjust timestamps if needed

### Method 2: Create Synthetic Data

Create realistic test data by:

```bash
cat > my_test_decodes.txt << 'EOF'
# My custom test session - 20m band on Dec 21
200000 -10 0.2 14074 ~ CQ N7MKO DM09
200010 +3 -0.1 14075 ~ W0AIL N7MKO EN90
200020 -7 0.3 14074 ~ N7MKO W0AIL FM09
200030 -12 0.0 14076 ~ CQ K1ABC FN42
EOF

python3 ./src/main.py -c config/tracker.conf --test-file my_test_decodes.txt
```

### Method 3: Generate With Script

For repetitive testing, create decodes programmatically:

```python
#!/usr/bin/env python3
import sys

def generate_test_decodes(count=100, base_time=120000):
    """Generate synthetic test decodes"""
    callsigns = ['W0AIL', 'N7MKO', 'K1ABC', 'W5XYZ', 'VE3ABC']
    grids = ['EN90', 'DM09', 'FN42', 'EM65', 'FM09']
    freqs = [7074, 7076, 7078, 14074, 14075]
    
    for i in range(count):
        time = base_time + i*10  # 10 seconds between decodes
        snr = -15 + (i % 30)  # Vary SNR from -15 to +15
        dt = (i % 10 - 5) / 10  # -0.5 to +0.5
        freq = freqs[i % len(freqs)]
        call = callsigns[i % len(callsigns)]
        grid = grids[i % len(grids)]
        
        time_str = f"{time:06d}"
        print(f"{time_str} {snr:+3d} {dt:5.1f} {freq} ~ CQ {call} {grid}")

if __name__ == '__main__':
    generate_test_decodes(100)
```

Save as `generate_test_decodes.py` and run:
```bash
python3 generate_test_decodes.py > large_test_file.txt
python3 ./src/main.py -c config/tracker.conf --test-file large_test_file.txt
```

## Testing Tips

### Rapid Testing

To quickly process all decodes:
1. In `ft8_decoder.py`, set the `delay` config to `0.0` (no delay between decodes)
2. Run test mode - decodes will be processed as fast as possible

### Looping Test Data

To continuously loop through the test file:
1. Add to config: `delay=0.1` (seconds between decodes)
2. The tracker will re-read the file and loop indefinitely
3. Useful for long-running app testing

### Database Verification

After running test mode, verify decodes were stored:

```bash
python3 -c "
import sqlite3
db = sqlite3.connect('data/tracker.db')
cursor = db.cursor()
cursor.execute('SELECT COUNT(*) FROM decodes')
print(f'Total decodes in database: {cursor.fetchone()[0]}')
db.close()
"
```

### Monitor in Real-Time

In another terminal, connect to the network server:

```bash
nc localhost 8080
```

You'll see real-time FT8 decode messages as they're sent to connected clients.

## Troubleshooting

### Test File Not Found
```
ERROR: Test file not found: test_decodes.txt
```
**Solution**: Provide full path or ensure file exists in current directory
```bash
python3 ./src/main.py -c config/tracker.conf --test-file /full/path/to/test_decodes.txt
```

### Decodes Not Appearing
1. Check file format matches pattern (use `grep` to validate):
   ```bash
   grep "^[0-9]" test_decodes.txt | head -5
   ```
2. Enable verbose logging: `-v`
3. Check Android app is connected to network server

### Wrong Timestamps
- Test file timestamps use UTC
- Tracker converts HHMMSS to today's date in UTC
- If testing across midnight, adjust your test file times

## Next Steps

Once testing is working:

1. Create comprehensive test data sets for different scenarios:
   - High activity (many stations)
   - Low activity (few stations)
   - Mixed SNR (realistic conditions)
   - International DX

2. Test Android app features systematically:
   - Band switching and filtering
   - GPS position updates
   - Network reconnection
   - Database queries

3. Stress test with large datasets:
   - Generate 1000+ test decodes
   - Test UI performance

4. Simulate real hardware conditions:
   - Create test data that mimics actual WSJT-X output
   - Include realistic time spacing and SNR distributions
