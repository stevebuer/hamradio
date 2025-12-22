% FT8 Tracker Test Mode - INDEX & QUICK REFERENCE
% Created: December 21, 2025

# FT8 Tracker Test Mode - Complete Index

## 🚀 Quick Start (Just Want to Test?)

```bash
cd /home/steve/GITHUB/hamradio/tracker
./run-test.sh test_decodes.txt
# Connect Android app to: 127.0.0.1:8080
```

---

## 📂 What Was Added

### Source Code (2 files modified)

| File | Changes | Purpose |
|------|---------|---------|
| `src/ft8_decoder.py` | +140 lines | New `TestFileDecoder` class |
| `src/main.py` | +10 lines | New `--test-file` argument |

### Test Data (1 file)

| File | Size | Content |
|------|------|---------|
| `test_decodes.txt` | 2.4 KB | 40+ example FT8 decodes |

### Scripts (2 executable files)

| File | Purpose | Usage |
|------|---------|-------|
| `run-test.sh` | Quick test launcher | `./run-test.sh [file.txt]` |
| `TEST_QUICK_REFERENCE.sh` | Show quick reference | `./TEST_QUICK_REFERENCE.sh` |

### Documentation (5 files)

| File | Size | Purpose |
|------|------|---------|
| `TESTING.md` | 7.8 KB | Complete testing guide |
| `TEST_MODE_SUMMARY.md` | 4.4 KB | Implementation overview |
| `IMPLEMENTATION_DETAILS.md` | 7.9 KB | Technical details |
| `GETTING_STARTED_WITH_TESTS.md` | 6.5 KB | Friendly getting started |
| `README.md` (updated) | Updated | New testing section |

---

## 📚 Documentation Quick Guide

### For Different Audiences

**I just want to test my app:**
→ Run: `./run-test.sh test_decodes.txt`
→ Read: [GETTING_STARTED_WITH_TESTS.md](GETTING_STARTED_WITH_TESTS.md) (3 min read)

**I want to understand how it works:**
→ Read: [TEST_MODE_SUMMARY.md](TEST_MODE_SUMMARY.md) (5 min read)
→ Then: [IMPLEMENTATION_DETAILS.md](IMPLEMENTATION_DETAILS.md) (10 min read)

**I need detailed testing information:**
→ Read: [TESTING.md](TESTING.md) (20 min read, comprehensive guide)

**I need quick command reference:**
→ Run: `./TEST_QUICK_REFERENCE.sh`

**I need help troubleshooting:**
→ See: [TESTING.md#troubleshooting](TESTING.md) troubleshooting section

---

## 🔧 Command Cheat Sheet

```bash
# Basic test run
./run-test.sh test_decodes.txt

# Custom test file
./run-test.sh /path/to/my_test_file.txt

# Direct command with verbose output
python3 ./src/main.py -c config/tracker.conf --test-file test_decodes.txt -v

# View help
python3 ./src/main.py --help

# Quick reference card
./TEST_QUICK_REFERENCE.sh

# Verify decodes in database
python3 << 'EOF'
import sqlite3
db = sqlite3.connect('data/tracker.db')
c = db.cursor()
c.execute('SELECT COUNT(*) FROM decodes')
print(f"Decodes: {c.fetchone()[0]}")
db.close()
EOF
```

---

## 📊 Test Data Format Reference

```
HHMMSS SNR DT FREQUENCY ~ MESSAGE

120000 -12 0.3 7074 ~ CQ W0AIL EN90

Field meanings:
  HHMMSS  = UTC time (hours, minutes, seconds)
  SNR     = Signal-to-Noise Ratio in dB (-20 to +20)
  DT      = Time offset in seconds (-0.5 to +0.5)
  FREQ    = Frequency in kHz (7074 for 40m, 14074 for 20m, etc.)
  MESSAGE = FT8 message (CQ calls, responses, grid exchanges)
```

### Common FT8 Frequencies

| Band | Frequency (kHz) | Example |
|------|-----------------|---------|
| 80m | 3573-3580 | 3573 |
| 40m | 7070-7100 | 7074 |
| 20m | 14070-14100 | 14074 |
| 10m | 28070-28100 | 28074 |

---

## ✨ Features & Benefits

| Feature | Benefit |
|---------|---------|
| **No Hardware** | Test on laptop anywhere |
| **Easy to Use** | One command: `./run-test.sh` |
| **Realistic** | Real FT8 format (WSJT-X compatible) |
| **Integrated** | Works with all tracker components |
| **Flexible** | Create any test scenario |
| **Repeatable** | Same test data for consistent results |
| **Scalable** | Test with 1000s of decodes |
| **Documented** | 4+ comprehensive guides included |

---

## 🎯 Common Use Cases

### Use Case 1: Quick Verification
```bash
./run-test.sh test_decodes.txt
# Verifies basic functionality with 40+ example decodes
```

### Use Case 2: Test New Feature
```bash
# Create custom test data for new feature
cat > my_feature_test.txt << 'EOF'
120000 -10 0.2 7074 ~ CQ N7MKO DM09
120010 +5 -0.1 7076 ~ W0AIL N7MKO EN90
EOF

./run-test.sh my_feature_test.txt
```

### Use Case 3: Stress Testing
```bash
# Generate 1000 test decodes
python3 << 'EOF'
for i in range(1000):
    time = 120000 + i*10
    snr = -15 + (i % 30)
    print(f'{time:06d} {snr:+3d} 0.1 7074 ~ CQ N7MKO DM09')
EOF > large_test.txt

./run-test.sh large_test.txt
```

### Use Case 4: Different Scenarios
```bash
# Weak signals
cat > weak.txt << 'EOF'
120000 -20 0.2 7074 ~ CQ VK3 OF87
120010 -18 -0.1 7076 ~ CQ JA2 PM97
EOF

# Strong signals
cat > strong.txt << 'EOF'
120000 +12 0.1 7074 ~ CQ W0AIL EN90
120010 +15 -0.2 7076 ~ CQ K1ABC FN42
EOF

./run-test.sh weak.txt   # Test weak signal handling
./run-test.sh strong.txt # Test strong signal handling
```

---

## 📈 Verification Checklist

After running test mode, verify:

- [ ] Tracker starts without errors
- [ ] Network server listening on 127.0.0.1:8080
- [ ] Android app connects successfully
- [ ] Decodes appear in Android app
- [ ] Decodes stored in database (check with SQL query)
- [ ] Logs show "Decode #1", "Decode #2", etc.

---

## 🆘 Troubleshooting Quick Links

| Problem | Solution | More Info |
|---------|----------|-----------|
| Test file not found | Check file exists and path | TESTING.md |
| Wrong test file format | Verify format with grep | TESTING.md |
| Android app not connecting | Check network server logs | TESTING.md |
| Decodes not in database | Check file format | TESTING.md |
| Performance issues | Enable verbose logging | TESTING.md |

---

## 📞 Getting Help

1. **Quick answers**: Run `./TEST_QUICK_REFERENCE.sh`
2. **How-to guides**: Read [TESTING.md](TESTING.md)
3. **Implementation details**: Read [IMPLEMENTATION_DETAILS.md](IMPLEMENTATION_DETAILS.md)
4. **Getting started**: Read [GETTING_STARTED_WITH_TESTS.md](GETTING_STARTED_WITH_TESTS.md)
5. **Troubleshooting**: See TESTING.md troubleshooting section

---

## 📊 Statistics

| Category | Details |
|----------|---------|
| **Code Changes** | 150 lines total |
| **Documentation** | 32+ KB, 5 files |
| **Test Data** | 40+ example decodes |
| **Scripts** | 2 executable scripts |
| **Total Files** | 9 new/modified |
| **Time to Start** | < 30 seconds |

---

## 🚀 Next Steps

1. **Start test mode**:
   ```bash
   ./run-test.sh test_decodes.txt
   ```

2. **Connect Android app**:
   - Use IP: `127.0.0.1:8080`
   - Or your laptop IP if testing remotely

3. **Watch decodes**:
   - FT8 decodes appear in real-time
   - All components fully functional
   - Database stores all decodes

4. **Create custom tests**:
   - Modify test_decodes.txt
   - Or create scenario-specific files
   - Test different situations

5. **Iterate rapidly**:
   - Test new features
   - No hardware setup needed
   - Repeatable consistent testing

---

## 📖 Reading Order Recommendation

1. **First time?** → GETTING_STARTED_WITH_TESTS.md (5 min)
2. **Want details?** → TEST_MODE_SUMMARY.md (5 min)
3. **Need to troubleshoot?** → TESTING.md (20 min)
4. **Want all technical details?** → IMPLEMENTATION_DETAILS.md (15 min)
5. **Need quick commands?** → Run: `./TEST_QUICK_REFERENCE.sh`

---

## ✅ Everything's Ready

- ✓ Code implemented and tested
- ✓ Test data files provided
- ✓ Documentation comprehensive
- ✓ Scripts ready to use
- ✓ No additional setup needed

**You're all set to test your Android app!** 🎉

---

*For the latest information and updates, see the individual documentation files listed above.*
