"""
Main FT8 Tracker Service
Coordinates all components: FT8 decoder, GPS, database, network server, IoT uploader
"""

import logging
import signal
import sys
import time
import configparser
from pathlib import Path
from typing import Dict, Any

# Import our modules
from ft8_decoder import create_decoder, FT8Decode
from gps_handler import GPSHandler, DummyGPS, GPSPosition
from database import Database
from network_server import NetworkServer
from iot_uploader import IoTUploader

logger = logging.getLogger(__name__)


class FT8Tracker:
    """Main FT8 Tracker service"""
    
    def __init__(self, config_file: str):
        self.config = self._load_config(config_file)
        self.running = False
        
        # Components
        self.database = None
        self.ft8_decoder = None
        self.gps_handler = None
        self.network_server = None
        self.iot_uploader = None
        
        # Current state
        self.decode_count = 0
        self.last_gps_position: GPSPosition = None
        
    def _load_config(self, config_file: str) -> Dict[str, Any]:
        """Load configuration from file"""
        config_path = Path(config_file)
        
        if not config_path.exists():
            logger.warning(f"Config file not found: {config_file}, using defaults")
            return self._get_default_config()
            
        parser = configparser.ConfigParser()
        parser.read(config_path)
        
        config = {
            'gps': dict(parser['gps']) if 'gps' in parser else {},
            'audio': dict(parser['audio']) if 'audio' in parser else {},
            'ft8': dict(parser['ft8']) if 'ft8' in parser else {},
            'database': dict(parser['database']) if 'database' in parser else {},
            'network': dict(parser['network']) if 'network' in parser else {},
            'iot': dict(parser['iot']) if 'iot' in parser else {},
            'logging': dict(parser['logging']) if 'logging' in parser else {}
        }
        
        return config
        
    def _get_default_config(self) -> Dict[str, Any]:
        """Get default configuration"""
        return {
            'gps': {'enabled': 'false', 'device': '/dev/ttyACM0', 'baud': '9600'},
            'audio': {'device': 'hw:0,0', 'sample_rate': '12000'},
            'ft8': {
                'decoder': 'wsjtx',
                'decoder_path': '/usr/bin/wsjtx',
                'log_file': str(Path.home() / '.local/share/WSJT-X/ALL.TXT')
            },
            'database': {
                'path': './data/tracker.db'
            },
            'network': {
                'server_enabled': 'true',
                'server_port': '8080',
                'server_bind': '0.0.0.0'
            },
            'iot': {
                'enabled': 'false',
                'server': 'https://dx.jxqz.org',
                'api_key': '',
                'station': 'N7MKO-M',
                'upload_interval': '300',
                'batch_size': '100'
            },
            'logging': {
                'level': 'INFO',
                'file': './logs/tracker.log'
            }
        }
        
    def start(self):
        """Start all tracker components"""
        logger.info("Starting FT8 Tracker...")
        
        try:
            # Initialize database
            db_path = self.config['database'].get('path', './data/tracker.db')
            self.database = Database(db_path)
            logger.info(f"Database initialized: {db_path}")
            
            # Initialize GPS
            if self.config['gps'].get('enabled', 'true').lower() == 'true':
                logger.info("Initializing GPS...")
                try:
                    self.gps_handler = GPSHandler(self.config['gps'])
                    if not self.gps_handler.start():
                        logger.warning("GPS failed to start, using dummy GPS")
                        self.gps_handler = DummyGPS(self.config['gps'])
                        self.gps_handler.start()
                except Exception as e:
                    logger.error(f"GPS error: {e}, using dummy GPS")
                    self.gps_handler = DummyGPS(self.config['gps'])
                    self.gps_handler.start()
            else:
                logger.info("GPS disabled in config, using dummy GPS for external updates")
                self.gps_handler = DummyGPS(self.config['gps'])
                self.gps_handler.start()
                
            self.gps_handler.add_callback(self._on_gps_update)
            
            # Initialize network server
            if self.config['network'].get('server_enabled', 'true').lower() == 'true':
                self.network_server = NetworkServer(self.config['network'])
                # Register GPS callback to store external GPS updates
                self.network_server.set_gps_callback(self._on_external_gps_update)
                # Register band callback to store band changes
                self.network_server.set_band_callback(self._on_band_change)
                if self.network_server.start():
                    logger.info("Network server started")
                else:
                    logger.error("Network server failed to start")
                    
            # Initialize IoT uploader
            if self.config['iot'].get('enabled', 'false').lower() == 'true':
                self.iot_uploader = IoTUploader(self.config['iot'], self.database)
                self.iot_uploader.start()
                logger.info("IoT uploader started")
                
            # Initialize FT8 decoder
            decoder_type = self.config['ft8'].get('decoder', 'wsjtx')
            self.ft8_decoder = create_decoder(decoder_type, self.config['ft8'])
            self.ft8_decoder.add_callback(self._on_decode)
            self.ft8_decoder.start()
            logger.info(f"FT8 decoder started: {decoder_type}")
            
            self.running = True
            logger.info("FT8 Tracker started successfully")
            
            return True
            
        except Exception as e:
            logger.error(f"Failed to start tracker: {e}")
            self.stop()
            return False
            
    def stop(self):
        """Stop all components"""
        logger.info("Stopping FT8 Tracker...")
        self.running = False
        
        if self.ft8_decoder:
            self.ft8_decoder.stop()
            
        if self.gps_handler:
            self.gps_handler.stop()
            
        if self.network_server:
            self.network_server.stop()
            
        if self.iot_uploader:
            self.iot_uploader.stop()
            
        logger.info("FT8 Tracker stopped")
        
    def _on_decode(self, decode: FT8Decode):
        """Handle new FT8 decode"""
        self.decode_count += 1
        
        logger.info(f"Decode #{self.decode_count}: {decode.to_android_format()}")
        
        # Get current GPS position
        gps_data = None
        if self.gps_handler and self.gps_handler.has_fix():
            pos = self.gps_handler.get_position()
            if pos:
                gps_data = pos.to_dict()
                logger.debug(f"  Position: {pos.latitude:.6f}, {pos.longitude:.6f}")
        
        # Get current band from network server (set by Android app)
        current_band = None
        if self.network_server:
            current_band = self.network_server.get_current_band()
            if current_band:
                logger.debug(f"  Band: {current_band}")
                
        # Store in database
        if self.database:
            try:
                decode_dict = decode.to_dict()
                # Add band if we know it
                if current_band:
                    decode_dict['band'] = current_band
                self.database.insert_decode(decode_dict, gps_data)
            except Exception as e:
                logger.error(f"Database error: {e}")
                
        # Send to network clients
        if self.network_server:
            try:
                self.network_server.send_decode(decode.to_android_format())
            except Exception as e:
                logger.error(f"Network server error: {e}")
                
    def _on_gps_update(self, position: GPSPosition):
        """Handle GPS position update from local GPS"""
        self.last_gps_position = position
        logger.debug(f"GPS update: {position}")
    
    def _on_external_gps_update(self, gps_data: Dict[str, Any]):
        """Handle GPS position update from external source (Android Auto)"""
        logger.info(f"External GPS update: {gps_data.get('latitude')}, {gps_data.get('longitude')}")
        
        # Store in database
        if self.database:
            try:
                self.database.insert_gps_position(gps_data, source='android_auto')
            except Exception as e:
                logger.error(f"Failed to store external GPS: {e}")
        
        # If we don't have a local GPS fix, use external GPS as current position
        if self.gps_handler and not self.gps_handler.has_fix():
            try:
                from gps_handler import GPSPosition
                self.last_gps_position = GPSPosition(
                    timestamp=datetime.fromtimestamp(gps_data.get('timestamp', time.time())),
                    latitude=gps_data['latitude'],
                    longitude=gps_data['longitude'],
                    altitude=gps_data.get('altitude', 0.0),
                    speed=gps_data.get('speed', 0.0),
                    heading=gps_data.get('heading', 0.0),
                    satellites=0,
                    fix_quality=1,
                    hdop=0.0
                )
                logger.info("Using external GPS as current position")
            except Exception as e:
                logger.error(f"Failed to create position from external GPS: {e}")
    
    def _on_band_change(self, band: str):
        """Handle band change from Android app"""
        logger.info(f"Band changed to: {band}")
        # Band is stored in network_server.current_band and used in _on_decode()
        
    def get_status(self) -> Dict[str, Any]:
        """Get current tracker status"""
        status = {
            'running': self.running,
            'decode_count': self.decode_count,
            'gps_fix': self.gps_handler.has_fix() if self.gps_handler else False,
            'network_clients': self.network_server.get_client_count() if self.network_server else 0
        }
        
        if self.last_gps_position:
            status['gps_position'] = self.last_gps_position.to_dict()
            
        if self.database:
            status['database'] = self.database.get_stats()
            
        if self.iot_uploader:
            status['iot'] = self.iot_uploader.get_stats()
            
        return status
        
    def run(self):
        """Main run loop"""
        if not self.start():
            return 1
            
        # Setup signal handlers
        signal.signal(signal.SIGINT, lambda s, f: self.stop())
        signal.signal(signal.SIGTERM, lambda s, f: self.stop())
        
        try:
            # Main loop
            while self.running:
                time.sleep(10)
                
                # Print status every minute
                if self.decode_count % 6 == 0:
                    status = self.get_status()
                    logger.info(f"Status: {self.decode_count} decodes, "
                               f"{status['network_clients']} clients, "
                               f"GPS: {'OK' if status['gps_fix'] else 'NO FIX'}")
                    
        except KeyboardInterrupt:
            logger.info("Interrupted by user")
        finally:
            self.stop()
            
        return 0


def main():
    """Main entry point"""
    import argparse
    
    parser = argparse.ArgumentParser(description='FT8 Mobile Tracker')
    parser.add_argument('-c', '--config', default='config/tracker.conf',
                       help='Configuration file path')
    parser.add_argument('-v', '--verbose', action='store_true',
                       help='Verbose logging')
    parser.add_argument('--status', action='store_true',
                       help='Show status and exit')
    
    args = parser.parse_args()
    
    # Setup logging
    log_level = logging.DEBUG if args.verbose else logging.INFO
    logging.basicConfig(
        level=log_level,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.StreamHandler(sys.stdout),
            logging.FileHandler('logs/tracker.log')
        ]
    )
    
    # Create and run tracker
    tracker = FT8Tracker(args.config)
    
    if args.status:
        if tracker.start():
            time.sleep(2)
            status = tracker.get_status()
            print(json.dumps(status, indent=2))
            tracker.stop()
        return 0
        
    return tracker.run()


if __name__ == '__main__':
    import json
    sys.exit(main())
