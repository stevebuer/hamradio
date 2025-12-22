# FT8 Tracker Test Mode - Complete Overview

## 🎯 Mission Accomplished

You now have a **fully functional test mode** for your Pine64 FT8 tracker that allows you to test the Android app **without any hardware**. Simply run the tracker with a test data file, connect your Android app, and start testing!

## 📋 Quick Start (30 seconds)

```bash
cd /home/steve/GITHUB/hamradio/tracker
./run-test.sh test_decodes.txt
# In Android app, connect to: 127.0.0.1:8080 or your laptop IP
# Watch FT8 decodes appear in real-time!
```

## 🔧 What Was Implemented

### 1. **Core Feature: TestFileDecoder Class**
   - **File**: `src/ft8_decoder.py`
   - **What it does**: Reads FT8 decodes from a text file
   - **Format**: Same as WSJT-X log file (HHMMSS SNR DT FREQ ~ MESSAGE)
   - **Features**:
     - Configurable delay between decodes
     - Optional looping mode
     - Full callsign/grid extraction
     - Integrated logging

### 2. **Command-Line Interface**
   - **File**: `src/main.py`
   - **Usage**: `python3 ./src/main.py --test-file myfile.txt`
   - **How it works**: Overrides decoder type to 'test' when specified
   - **Integration**: Works with all tracker components (DB, GPS, network server)

### 3. **Included Test Data**
   - **File**: `test_decodes.txt`
   - **Content**: 40+ realistic FT8 decodes
   - **Bands**: 40m (7074 kHz) and 20m (14074 kHz)
   - **SNR Range**: -20 to +15 dB (weak to strong signals)
   - **Ready to use**: No setup required!

### 4. **Convenience Script**
   - **File**: `run-test.sh`
   - **Usage**: `./run-test.sh [test_file.txt]`
   - **Benefits**: 
     - One-command operation
     - Auto-detects test file
     - Counts decodes
     - Shows network address

## 📚 Documentation Included

| Document | Purpose | Size |
|----------|---------|------|
| **TESTING.md** | Complete testing guide with examples | 7.8 KB |
| **TEST_MODE_SUMMARY.md** | Quick implementation overview | 4.4 KB |
| **IMPLEMENTATION_DETAILS.md** | Technical details and statistics | 7.9 KB |
| **TEST_QUICK_REFERENCE.sh** | Quick command reference card | 6.3 KB |
| **Updated README.md** | New testing section added | Updated |

## 🚀 Usage Scenarios

### Scenario 1: Quick Test Run
```bash
./run-test.sh test_decodes.txt
# Tracks 40+ example decodes, verify basic functionality
```

### Scenario 2: Stress Testing
```bash
# Create 1000 test decodes
python3 -c "
for i in range(1000):
    time = 120000 + i*10
    snr = -15 + (i % 30)
    print(f'{time:06d} {snr:+3d} 0.1 7074 ~ CQ N7MKO DM09')
" > large_test.txt

./run-test.sh large_test.txt
```

### Scenario 3: Custom Test Scenario
```bash
cat > my_scenario.txt << 'EOF'
# Test with weak signals
120000 -20 0.2 7074 ~ CQ VK3 OF87
120010 -18 -0.1 7076 ~ CQ JA2 PM97
120020 -22 0.3 7074 ~ CQ ZL2 RE67

# Strong signals
120100 +12 0.1 7074 ~ CQ W0AIL EN90
120110 +15 -0.2 7076 ~ CQ K1ABC FN42
EOF

./run-test.sh my_scenario.txt
```

### Scenario 4: Testing with Different Bands
```bash
cat > multiband_test.txt << 'EOF'
# 40m band
120000 -10 0.2 7074 ~ CQ N7MKO DM09

# 20m band
120010 +5 -0.1 14074 ~ CQ W0AIL EN90

# 10m band
120020 -8 0.3 28074 ~ CQ K1ABC FN42
EOF

./run-test.sh multiband_test.txt
```

## 📊 Architecture

```
Test Data File (test_decodes.txt)
        ↓
    [Parse FT8 format]
        ↓
  TestFileDecoder (NEW!)
        ↓
  FT8Tracker Service
   ├─ Database (store decodes)
   ├─ GPS Handler (dummy GPS)
   ├─ Network Server (send to app)
   └─ [Other components]
        ↓
  Android App (127.0.0.1:8080)
```

## ✨ Key Features

✅ **No Hardware Required**
   - No radio, audio interface, or GPS needed
   - Test on laptop anywhere

✅ **Easy to Use**
   - Simple text file format
   - One command to start: `./run-test.sh test_decodes.txt`

✅ **Realistic Data**
   - Uses actual WSJT-X format
   - Includes SNR, timing, frequency data
   - Multiple bands supported

✅ **Fully Integrated**
   - Works with all tracker components
   - Decodes stored in database
   - Sent to Android app over network
   - Same as real hardware operation

✅ **Flexible Testing**
   - Create any test scenario
   - Stress test with 1000s of decodes
   - Repeatable consistent testing

✅ **Well Documented**
   - 4 documentation files
   - Examples and quick reference
   - Troubleshooting guide

## 📝 Test Data Format

```
HHMMSS SNR DT FREQUENCY ~ MESSAGE

120000 -12 0.3 7074 ~ CQ W0AIL EN90
├─────┤ └┬┘ └─┬┘ └────┤ └─────────┬─────┘
│       │  │   │   Freq   Message (FT8)
│       │  │   └ Time offset (sec)
│       │  └── SNR in dB
│       └─ Negative (weak signal)
└─ UTC Time (12:00:00)
```

## 🎓 Testing Best Practices

1. **Start Simple**: Use provided `test_decodes.txt`
2. **Verify Integration**: Check database has entries
3. **Test Edge Cases**: Create scenarios with weak signals, high decode rates
4. **Stress Test**: Generate large files for performance testing
5. **Document Results**: Note what scenarios work/fail

## 🔍 Verification

After running test mode, verify it worked:

```bash
# Check database entries
python3 << 'EOF'
import sqlite3
db = sqlite3.connect('data/tracker.db')
c = db.cursor()
c.execute('SELECT COUNT(*) FROM decodes')
print(f"Decodes stored: {c.fetchone()[0]}")
db.close()
EOF

# Check logs
tail -f logs/tracker.log | grep "Decode #"

# Connect to network server
nc 127.0.0.1 8080
```

## 🆘 Troubleshooting

**Problem**: "Test file not found"
```
Solution: Verify file exists and use correct path
$ ls -la test_decodes.txt
$ ./run-test.sh /full/path/to/test_decodes.txt
```

**Problem**: "Decodes not in database"
```
Solution: Check format is correct
$ grep "^[0-9]" test_decodes.txt | head -5
```

**Problem**: "Android app not connecting"
```
Solution: Verify network server is running
$ tail logs/tracker.log | grep "Network server"
```

## 📚 Files Reference

### Source Code Changes
- `src/ft8_decoder.py` (+140 lines) - TestFileDecoder class
- `src/main.py` (+10 lines) - --test-file argument

### New Test Files
- `test_decodes.txt` - 40+ example FT8 decodes
- `run-test.sh` - Convenience launcher script

### Documentation
- `TESTING.md` - Comprehensive testing guide
- `TEST_MODE_SUMMARY.md` - Implementation summary
- `IMPLEMENTATION_DETAILS.md` - Technical details
- `TEST_QUICK_REFERENCE.sh` - Quick command reference
- Updated `README.md` - New testing section

## 🎯 Next Steps

1. **Try it now**:
   ```bash
   ./run-test.sh test_decodes.txt
   ```

2. **Connect Android app**:
   - Use IP: 127.0.0.1:8080 (or your laptop IP)
   - Watch decodes appear!

3. **Create custom test data**:
   - Copy `test_decodes.txt`
   - Edit with your scenarios
   - Run with `./run-test.sh`

4. **Test new features**:
   - All Android features can now be tested without hardware
   - Iterate fast!

## 💡 Tips & Tricks

**Rapid Testing**: Set `delay=0.0` in code for fast decode processing

**Looping Tests**: Set `loop=true` in config to re-read file indefinitely

**Different Scenarios**: Create scenario-specific test files
- `weak_signals.txt` - Testing weak signal handling
- `high_activity.txt` - Testing high decode rates
- `multiband.txt` - Testing multiple bands

**Python Generation**: Script to create test files programmatically:
```python
#!/usr/bin/env python3
for i in range(100):
    time = 120000 + i*10
    snr = -15 + (i % 30)
    print(f'{time:06d} {snr:+3d} 0.1 7074 ~ CQ N7MKO DM09')
```

## 🎉 Summary

You now have a complete, documented, and tested implementation of FT8 Tracker test mode. This enables:

- ✅ Fast Android app development and testing
- ✅ Testing without hardware setup
- ✅ Repeatable, consistent test scenarios
- ✅ Stress testing capabilities
- ✅ Integration with all tracker components

**Ready to test your Android app!** 🚀

---

**Questions?** Check the detailed documentation in TESTING.md or IMPLEMENTATION_DETAILS.md
