"""
IoT Uploader
Uploads FT8 decodes to dx.jxqz.org when internet is available
"""

import logging
import threading
import time
import json
import requests
from typing import List, Dict, Any, Optional
from datetime import datetime

logger = logging.getLogger(__name__)


class IoTUploader:
    """Uploads decodes to IoT server (dx.jxqz.org)"""
    
    def __init__(self, config: Dict[str, Any], database):
        self.config = config
        self.database = database
        self.running = False
        self.upload_thread = None
        
        self.server_url = config.get('server', 'https://dx.jxqz.org')
        self.api_key = config.get('api_key', '')
        self.station = config.get('station', 'UNKNOWN')
        self.upload_interval = int(config.get('upload_interval', 300))  # 5 minutes
        self.batch_size = int(config.get('batch_size', 100))
        self.enabled = config.get('enabled', True)
        
        # Statistics
        self.total_uploaded = 0
        self.last_upload_time: Optional[datetime] = None
        self.consecutive_failures = 0
        
    def start(self):
        """Start the uploader thread"""
        if not self.enabled:
            logger.info("IoT uploader disabled in config")
            return
            
        if not self.api_key:
            logger.warning("No API key configured, uploads disabled")
            return
            
        self.running = True
        self.upload_thread = threading.Thread(target=self._upload_loop)
        self.upload_thread.daemon = True
        self.upload_thread.start()
        
        logger.info(f"IoT uploader started (server: {self.server_url}, interval: {self.upload_interval}s)")
        
    def stop(self):
        """Stop the uploader"""
        self.running = False
        if self.upload_thread:
            self.upload_thread.join(timeout=5)
        logger.info("IoT uploader stopped")
        
    def _upload_loop(self):
        """Main upload loop"""
        while self.running:
            try:
                # Check if internet is available
                if not self._check_internet():
                    logger.debug("No internet connection, skipping upload")
                    time.sleep(30)
                    continue
                    
                # Get unuploaded decodes
                decodes = self.database.get_unuploaded_decodes(self.batch_size)
                
                if decodes:
                    logger.info(f"Uploading {len(decodes)} decodes to {self.server_url}")
                    
                    if self._upload_batch(decodes):
                        # Mark as uploaded
                        decode_ids = [d['id'] for d in decodes]
                        self.database.mark_uploaded(decode_ids)
                        
                        self.total_uploaded += len(decodes)
                        self.last_upload_time = datetime.now()
                        self.consecutive_failures = 0
                        
                        logger.info(f"Successfully uploaded {len(decodes)} decodes")
                    else:
                        self.consecutive_failures += 1
                        logger.warning(f"Upload failed (consecutive failures: {self.consecutive_failures})")
                        
                        # Back off on repeated failures
                        if self.consecutive_failures > 3:
                            time.sleep(self.upload_interval * 2)
                            continue
                else:
                    logger.debug("No decodes to upload")
                    
                # Wait for next interval
                time.sleep(self.upload_interval)
                
            except Exception as e:
                logger.error(f"Error in upload loop: {e}")
                time.sleep(60)
                
    def _check_internet(self) -> bool:
        """Check if internet is available"""
        try:
            # Try to resolve DNS
            response = requests.get('https://dns.google', timeout=5)
            return response.status_code == 200
        except:
            return False
            
    def _upload_batch(self, decodes: List[Dict[str, Any]]) -> bool:
        """Upload a batch of decodes to the server"""
        try:
            # Prepare payload
            payload = {
                'station': self.station,
                'timestamp': int(datetime.now().timestamp()),
                'decodes': []
            }
            
            for decode in decodes:
                decode_entry = {
                    'timestamp': decode['timestamp'],
                    'callsign': decode['callsign'] or '',
                    'grid': decode['grid'] or '',
                    'snr': decode['snr'],
                    'frequency': decode['frequency'],
                    'band': decode['band'] or '',
                    'message': decode['message'] or '',
                }
                
                # Add GPS data if available
                if decode.get('latitude') and decode.get('longitude'):
                    decode_entry['position'] = {
                        'latitude': decode['latitude'],
                        'longitude': decode['longitude'],
                        'altitude': decode.get('altitude', 0),
                        'speed': decode.get('speed', 0),
                        'heading': decode.get('heading', 0)
                    }
                    
                payload['decodes'].append(decode_entry)
                
            # Send to server
            headers = {
                'Content-Type': 'application/json',
                'Authorization': f'Bearer {self.api_key}'
            }
            
            endpoint = f"{self.server_url}/api/ft8/upload"
            
            response = requests.post(
                endpoint,
                json=payload,
                headers=headers,
                timeout=30
            )
            
            if response.status_code == 200:
                logger.info(f"Upload successful: {len(decodes)} decodes")
                return True
            else:
                logger.error(f"Upload failed: HTTP {response.status_code} - {response.text}")
                return False
                
        except requests.exceptions.Timeout:
            logger.error("Upload timeout")
            return False
        except requests.exceptions.ConnectionError:
            logger.error("Upload connection error")
            return False
        except Exception as e:
            logger.error(f"Upload error: {e}")
            return False
            
    def get_stats(self) -> Dict[str, Any]:
        """Get uploader statistics"""
        return {
            'enabled': self.enabled,
            'total_uploaded': self.total_uploaded,
            'last_upload_time': self.last_upload_time.isoformat() if self.last_upload_time else None,
            'consecutive_failures': self.consecutive_failures,
            'server_url': self.server_url,
            'station': self.station
        }
        
    def force_upload(self) -> bool:
        """Force an immediate upload"""
        logger.info("Forcing immediate upload...")
        decodes = self.database.get_unuploaded_decodes(self.batch_size)
        
        if not decodes:
            logger.info("No decodes to upload")
            return True
            
        if self._upload_batch(decodes):
            decode_ids = [d['id'] for d in decodes]
            self.database.mark_uploaded(decode_ids)
            logger.info(f"Force upload successful: {len(decodes)} decodes")
            return True
        else:
            logger.error("Force upload failed")
            return False


if __name__ == '__main__':
    # Test uploader
    import sys
    import tempfile
    from database import Database
    
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    # Create temp database
    db_file = tempfile.mktemp(suffix='.db')
    db = Database(db_file)
    
    # Insert test decode
    decode_data = {
        'timestamp': int(datetime.now().timestamp()),
        'time_str': '134500',
        'callsign': 'K1ABC',
        'grid': 'FN42',
        'snr': -12,
        'dt': 0.3,
        'frequency': 7074000,
        'message': 'CQ K1ABC FN42'
    }
    
    gps_data = {
        'latitude': 47.6062,
        'longitude': -122.3321,
        'altitude': 150.0,
        'speed': 0.0,
        'heading': 0.0
    }
    
    db.insert_decode(decode_data, gps_data)
    
    # Create uploader
    config = {
        'enabled': True,
        'server': 'https://dx.jxqz.org',
        'api_key': 'test_key_12345',
        'station': 'N7MKO-TEST',
        'upload_interval': 10,
        'batch_size': 10
    }
    
    uploader = IoTUploader(config, db)
    
    print("Testing IoT uploader...")
    print(f"Stats: {uploader.get_stats()}")
    
    # Note: This will fail without valid API key and server
    print("\nAttempting test upload (will fail without valid credentials)...")
    uploader.force_upload()
    
    print(f"\nFinal stats: {uploader.get_stats()}")
    
    # Cleanup
    import os
    os.unlink(db_file)
