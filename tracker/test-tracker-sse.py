#!/usr/bin/env python3
"""Test full tracker with SSE client in background"""

import subprocess
import time
import requests
import sys
import signal

def run_sse_client():
    """Connect as SSE client and display received decodes"""
    print("SSE client: Connecting to tracker...")
    try:
        response = requests.get(
            'http://127.0.0.1:8080/decodes',
            timeout=60,
            stream=True,
            headers={'Accept': 'text/event-stream'}
        )
        
        if response.status_code == 200:
            print("✓ SSE client connected to tracker")
            line_count = 0
            for line in response.iter_lines(decode_unicode=True):
                if line and not line.startswith(':'):
                    print(f"  > {line[:100]}")
                    line_count += 1
                    if line_count >= 15:  # Show first 15 decodes
                        print("  ... (more data arriving)")
                        break
            return True
        else:
            print(f"✗ SSE connection failed: {response.status_code}")
            return False
    except Exception as e:
        print(f"✗ SSE client error: {e}")
        return False

def main():
    # Start tracker
    print("Starting tracker in test mode...")
    tracker_proc = subprocess.Popen(
        ['python3', 'src/main.py', '-c', 'config/tracker.conf', '--test-file', 'test_decodes.txt', '-v'],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )
    
    # Wait for server to start
    print("Waiting for server to start...")
    for i in range(10):
        try:
            response = requests.get('http://127.0.0.1:8080/health', timeout=1)
            if response.status_code == 200:
                print("✓ Server is ready")
                break
        except:
            time.sleep(0.5)
    else:
        print("✗ Server did not start in time")
        tracker_proc.terminate()
        return False
    
    # Run SSE client
    time.sleep(1)  # Give time for tracker to be ready
    success = run_sse_client()
    
    # Stop tracker
    print("\nStopping tracker...")
    tracker_proc.terminate()
    try:
        tracker_proc.wait(timeout=2)
    except subprocess.TimeoutExpired:
        tracker_proc.kill()
    
    return success

if __name__ == '__main__':
    success = main()
    sys.exit(0 if success else 1)
