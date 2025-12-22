#!/bin/bash
# FT8 Tracker Test Mode - Quick Reference Card

cat << 'EOF'

╔════════════════════════════════════════════════════════════════╗
║                 FT8 TRACKER TEST MODE                          ║
║              Quick Reference & Command Cheat Sheet             ║
╚════════════════════════════════════════════════════════════════╝

📋 QUICK START
──────────────────────────────────────────────────────────────────
1. Start tracker in test mode:
   
   ./run-test.sh test_decodes.txt

2. Connect Android app to: 127.0.0.1:8080 (or your laptop IP)

3. Watch FT8 decodes appear in real-time!


🎯 COMMON COMMANDS
──────────────────────────────────────────────────────────────────

Basic test run:
  python3 ./src/main.py -c config/tracker.conf --test-file test_decodes.txt

Verbose output (debugging):
  python3 ./src/main.py -c config/tracker.conf --test-file test_decodes.txt -v

Using custom test file:
  ./run-test.sh my_custom_decodes.txt

View help:
  python3 ./src/main.py --help


📄 TEST DATA FORMAT
──────────────────────────────────────────────────────────────────

Format: HHMMSS SNR DT FREQUENCY ~ MESSAGE

Example:
  120000 -12 0.3 7074 ~ CQ W0AIL EN90
  │      │   │  │    │ │
  │      │   │  │    │ └─ FT8 Message
  │      │   │  │    └─── Separator (~)
  │      │   │  └─────── Frequency (kHz)
  │      │   └────────── Time offset (seconds)
  │      └────────────── SNR in dB (-12 = weak, +12 = strong)
  └──────────────────── UTC Time (HH:MM:SS)


📊 TEST DATA FILES
──────────────────────────────────────────────────────────────────

Included example:
  test_decodes.txt    - 40+ realistic FT8 decodes

Create new one:
  echo "120000 -10 0.2 7074 ~ CQ N7MKO DM09" > my_test.txt
  echo "120010 +5 -0.1 7076 ~ W0AIL N7MKO EN90" >> my_test.txt
  ./run-test.sh my_test.txt


🔍 VERIFY IT'S WORKING
──────────────────────────────────────────────────────────────────

Check decodes were stored in database:
  python3 << 'PYEOF'
import sqlite3
db = sqlite3.connect('data/tracker.db')
c = db.cursor()
c.execute('SELECT COUNT(*) FROM decodes')
print(f"Decodes in database: {c.fetchone()[0]}")
c.execute('SELECT time_str, snr, frequency, message FROM decodes LIMIT 3')
for row in c.fetchall():
    print(f"  {row[0]} {row[1]:+3d}dB {row[2]}kHz - {row[3]}")
db.close()
PYEOF

Watch logs in real-time:
  tail -f logs/tracker.log | grep "Decode #"

Connect to network server:
  nc 127.0.0.1 8080


📚 DOCUMENTATION
──────────────────────────────────────────────────────────────────

Complete testing guide:
  cat TESTING.md

Full implementation summary:
  cat TEST_MODE_SUMMARY.md

Updated README:
  cat README.md


🎓 COMMON TEST SCENARIOS
──────────────────────────────────────────────────────────────────

Weak signals:  Create decodes with SNR -20 to -10 dB
Strong signals: Create decodes with SNR +5 to +15 dB
Many decodes:  Generate 100+ test entries for stress testing
Mixed bands:   Use 40m (7074), 20m (14074), 10m (28074)
International: Use different grid squares (FN, EN, DM, EL, etc.)


🛠️ TROUBLESHOOTING
──────────────────────────────────────────────────────────────────

"Test file not found":
  → Verify file exists and path is correct
  → Use absolute path: /full/path/to/test_decodes.txt

"Decodes not appearing in Android app":
  → Ensure network server is running (check logs)
  → Verify Android app has correct IP/port
  → Try verbose mode: add -v flag

"Database errors":
  → Check data/ directory exists and is writable
  → Remove old database: rm data/tracker.db

"Test file not being read":
  → Verify file format with: grep "^[0-9]" test_decodes.txt
  → Enable verbose logging: -v flag


📌 TIPS & TRICKS
──────────────────────────────────────────────────────────────────

• Test file can have comments (lines starting with #)
• Blank lines are automatically skipped
• Change delay between decodes in code (default 1.0 sec)
• Can restart tracker without clearing database
• Test data works with all tracker components (DB, network, etc.)
• Use for stress testing (1000+ decodes)
• Create different scenarios for different testing phases


╔════════════════════════════════════════════════════════════════╗
║ For more details, see TESTING.md or TEST_MODE_SUMMARY.md      ║
║ Questions? Check the implementation in:                       ║
║  - src/ft8_decoder.py (TestFileDecoder class)                 ║
║  - src/main.py (test_file parameter)                          ║
╚════════════════════════════════════════════════════════════════╝

EOF
