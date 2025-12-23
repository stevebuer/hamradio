#!/usr/bin/env python3
"""Simple SSE test with proper server initialization"""

import sys
import os
import time
import threading
import requests
import json

# Add src to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

from network_server_flask import FlaskNetworkServer

def test_sse():
    """Test SSE streaming"""
    
    # Create server with test config
    config = {
        'server_bind': '127.0.0.1',
        'server_port': 8080
    }
    
    server = FlaskNetworkServer(config)
    
    # Start server
    if not server.start():
        print("ERROR: Failed to start server")
        return False
    
    print("✓ Server started")
    time.sleep(1)
    
    # Test health endpoint
    try:
        response = requests.get('http://127.0.0.1:8080/health', timeout=2)
        if response.status_code == 200:
            print(f"✓ Health endpoint OK: {response.json()}")
        else:
            print(f"✗ Health endpoint returned {response.status_code}")
            return False
    except Exception as e:
        print(f"✗ Health endpoint failed: {e}")
        return False
    
    # Test SSE streaming in a separate thread
    def test_client():
        print("→ SSE client connecting...")
        try:
            # This should connect and receive SSE data
            response = requests.get(
                'http://127.0.0.1:8080/decodes',
                timeout=10,
                stream=True,
                headers={'Accept': 'text/event-stream'}
            )
            
            if response.status_code == 200:
                print(f"✓ SSE client connected")
                print(f"  Content-Type: {response.headers.get('Content-Type')}")
                
                line_count = 0
                for line in response.iter_lines(decode_unicode=True):
                    if line:
                        print(f"  > {line[:80]}")
                        line_count += 1
                        if line_count >= 5:
                            break
                
                return True
            else:
                print(f"✗ SSE connection failed: {response.status_code}")
                return False
                
        except Exception as e:
            print(f"✗ SSE client error: {e}")
            return False
    
    # Send some test decodes while client is reading
    def send_decodes():
        print("→ Sending test decodes...")
        time.sleep(1)  # Wait for client to connect
        
        test_decodes = [
            {'freq': 7074000, 'snr': -12, 'dt': 0.3, 'delta': 1234, 'msg': '~ CQ TEST0 FN42'},
            {'freq': 7074500, 'snr': -8, 'dt': 0.5, 'delta': 500, 'msg': '~ K5ABC K1DEF'},
            {'freq': 7075000, 'snr': -5, 'dt': 0.2, 'delta': 2000, 'msg': '~ W1ABC W2XYZ R-12'},
        ]
        
        for i, decode in enumerate(test_decodes):
            print(f"  Sending decode {i+1}...")
            server.send_decode(decode)
            time.sleep(0.5)
    
    # Start client and decoder threads
    client_thread = threading.Thread(target=test_client)
    decoder_thread = threading.Thread(target=send_decodes)
    
    client_thread.start()
    decoder_thread.start()
    
    client_thread.join(timeout=15)
    decoder_thread.join(timeout=15)
    
    print("\n✓ Test completed")
    server.stop()
    time.sleep(0.5)
    
    return True

if __name__ == '__main__':
    success = test_sse()
    sys.exit(0 if success else 1)
