#!/usr/bin/env python3
"""
Quick test of Flask SSE functionality
"""

import sys
sys.path.insert(0, '/home/steve/GITHUB/hamradio/tracker/src')

from network_server_flask import FlaskNetworkServer
import time
import threading

config = {
    'server_bind': '127.0.0.1',
    'server_port': 8080
}

print("Starting Flask SSE test server...")
server = FlaskNetworkServer(config)

if not server.start():
    print("Failed to start server")
    sys.exit(1)

print("Server started. Waiting for it to be ready...")
time.sleep(2)

print("\nSending test decodes in 2 seconds...")
print("In another terminal, run: curl http://localhost:8080/decodes")
print("")

# Give user time to connect
time.sleep(2)

# Send some test decodes
for i in range(5):
    now = time.strftime("%H%M%S")
    test_decode = f"{now} -12  0.3 1234 ~ CQ TEST{i} FN42"
    print(f"Sending: {test_decode} (clients: {server.get_client_count()})")
    server.send_decode(test_decode)
    time.sleep(1)

print("\nTest complete. Press Ctrl+C to stop server")

try:
    while True:
        time.sleep(1)
except KeyboardInterrupt:
    print("\nStopping server...")
    server.stop()
