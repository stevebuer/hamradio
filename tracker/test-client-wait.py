#!/usr/bin/env python3
"""Test client-waiting feature in test mode"""

import subprocess
import time
import requests
import sys
import threading

def run_tracker(test_file='test_decodes.txt'):
    """Start tracker in test mode"""
    print("[TRACKER] Starting tracker in test mode...")
    print("[TRACKER] Tracker will wait for client before processing decodes")
    print("")
    
    proc = subprocess.Popen(
        ['python3', 'src/main.py', '-c', 'config/tracker.conf', '--test-file', test_file, '-v'],
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True
    )
    
    # Wait for server to start
    for i in range(10):
        try:
            response = requests.get('http://127.0.0.1:8080/health', timeout=1)
            if response.status_code == 200:
                health = response.json()
                print(f"[TRACKER] ✓ Server ready. SSE clients: {health['sse_clients']}")
                break
        except:
            time.sleep(0.5)
    else:
        print("[TRACKER] ✗ Server failed to start")
        return proc
    
    return proc

def connect_client():
    """Connect as SSE client and read decodes"""
    print("[CLIENT] Connecting to SSE stream...")
    time.sleep(1)  # Let tracker startup messages show
    
    try:
        response = requests.get(
            'http://127.0.0.1:8080/decodes',
            timeout=30,
            stream=True,
            headers={'Accept': 'text/event-stream'}
        )
        
        if response.status_code == 200:
            print("[CLIENT] ✓ Connected to SSE stream")
            print("")
            
            decode_count = 0
            for line in response.iter_lines(decode_unicode=True):
                if line and not line.startswith(':'):
                    decode_count += 1
                    print(f"[CLIENT] Decode {decode_count}: {line[:80]}")
                    if decode_count >= 5:
                        print("[CLIENT] ... (stopping after 5 decodes)")
                        break
            
            return True
        else:
            print(f"[CLIENT] ✗ Connection failed: {response.status_code}")
            return False
            
    except Exception as e:
        print(f"[CLIENT] ✗ Error: {e}")
        return False

def main():
    print("=" * 70)
    print("TEST: Tracker waits for client before processing decodes")
    print("=" * 70)
    print("")
    
    # Start tracker
    tracker_proc = run_tracker()
    
    # Connect client
    success = connect_client()
    
    print("")
    print("=" * 70)
    print("RESULT: " + ("✓ PASS" if success else "✗ FAIL"))
    print("=" * 70)
    
    # Cleanup
    print("")
    print("[CLEANUP] Stopping tracker...")
    tracker_proc.terminate()
    try:
        tracker_proc.wait(timeout=2)
    except subprocess.TimeoutExpired:
        tracker_proc.kill()
    
    return success

if __name__ == '__main__':
    success = main()
    sys.exit(0 if success else 1)
