"""
GPS Handler
Interfaces with USB GPS receiver via gpsd
Provides position, speed, heading, and time synchronization
"""

import logging
import threading
import time
from dataclasses import dataclass
from datetime import datetime
from typing import Optional, Dict, Any

logger = logging.getLogger(__name__)

# Try to import gps module (from gpsd)
try:
    import gps
    GPS_AVAILABLE = True
except ImportError:
    GPS_AVAILABLE = False
    logger.warning("gps module not available. Install with: pip install gps")


@dataclass
class GPSPosition:
    """GPS position and metadata"""
    timestamp: datetime
    latitude: float
    longitude: float
    altitude: float = 0.0
    speed: float = 0.0  # km/h
    heading: float = 0.0  # degrees
    satellites: int = 0
    fix_quality: int = 0  # 0=no fix, 1=GPS, 2=DGPS
    hdop: float = 0.0  # Horizontal dilution of precision
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary"""
        return {
            'timestamp': int(self.timestamp.timestamp()),
            'latitude': self.latitude,
            'longitude': self.longitude,
            'altitude': self.altitude,
            'speed': self.speed,
            'heading': self.heading,
            'satellites': self.satellites,
            'fix_quality': self.fix_quality,
            'hdop': self.hdop
        }
    
    def to_maidenhead(self, precision: int = 6) -> str:
        """Convert position to Maidenhead grid square"""
        # Full implementation of Maidenhead conversion
        lon = self.longitude + 180
        lat = self.latitude + 90
        
        grid = ""
        
        # Field (20° longitude, 10° latitude)
        grid += chr(int(lon / 20) + ord('A'))
        grid += chr(int(lat / 10) + ord('A'))
        
        # Square (2° longitude, 1° latitude)
        lon = (lon % 20) / 2
        lat = (lat % 10) / 1
        grid += str(int(lon))
        grid += str(int(lat))
        
        if precision >= 6:
            # Subsquare (5' longitude, 2.5' latitude)
            lon = (lon - int(lon)) * 24
            lat = (lat - int(lat)) * 24
            grid += chr(int(lon) + ord('a'))
            grid += chr(int(lat) + ord('a'))
            
        if precision >= 8:
            # Extended square
            lon = (lon - int(lon)) * 10
            lat = (lat - int(lat)) * 10
            grid += str(int(lon))
            grid += str(int(lat))
        
        return grid
    
    def __str__(self) -> str:
        return (f"GPS: {self.latitude:.6f}, {self.longitude:.6f} "
                f"alt={self.altitude:.1f}m speed={self.speed:.1f}km/h "
                f"sats={self.satellites} grid={self.to_maidenhead()}")


class GPSHandler:
    """GPS handler using gpsd"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.running = False
        self.session = None
        self.current_position: Optional[GPSPosition] = None
        self.last_fix_time: Optional[datetime] = None
        self.callbacks = []
        self.gps_thread = None
        
        if not GPS_AVAILABLE:
            logger.error("GPS module not available")
            
    def add_callback(self, callback):
        """Add callback to be called on position update"""
        self.callbacks.append(callback)
        
    def start(self):
        """Start GPS monitoring"""
        if not GPS_AVAILABLE:
            logger.error("Cannot start GPS: module not available")
            return False
            
        try:
            self.session = gps.gps()
            self.session.stream(gps.WATCH_ENABLE | gps.WATCH_NEWSTYLE)
            self.running = True
            
            self.gps_thread = threading.Thread(target=self._gps_loop)
            self.gps_thread.daemon = True
            self.gps_thread.start()
            
            logger.info("GPS monitoring started")
            return True
            
        except Exception as e:
            logger.error(f"Failed to start GPS: {e}")
            return False
            
    def stop(self):
        """Stop GPS monitoring"""
        self.running = False
        if self.gps_thread:
            self.gps_thread.join(timeout=2)
        logger.info("GPS monitoring stopped")
        
    def _gps_loop(self):
        """Main GPS monitoring loop"""
        while self.running:
            try:
                report = self.session.next()
                
                if report['class'] == 'TPV':
                    # Time-Position-Velocity report
                    self._process_tpv(report)
                    
            except StopIteration:
                logger.warning("GPS session ended")
                break
            except Exception as e:
                logger.error(f"GPS error: {e}")
                time.sleep(1)
                
    def _process_tpv(self, report):
        """Process TPV (Time-Position-Velocity) report"""
        # Check for valid fix
        mode = report.get('mode', 0)
        if mode < 2:  # No fix
            return
            
        # Extract data
        try:
            lat = report.get('lat', 0.0)
            lon = report.get('lon', 0.0)
            
            # Only process if we have valid coordinates
            if lat == 0.0 and lon == 0.0:
                return
                
            alt = report.get('alt', 0.0)
            speed = report.get('speed', 0.0) * 3.6  # m/s to km/h
            heading = report.get('track', 0.0)
            
            # Get timestamp
            time_str = report.get('time', '')
            if time_str:
                timestamp = datetime.fromisoformat(time_str.replace('Z', '+00:00'))
            else:
                timestamp = datetime.now()
                
            # Get fix quality
            satellites = 0
            hdop = 0.0
            
            # Update current position
            self.current_position = GPSPosition(
                timestamp=timestamp,
                latitude=lat,
                longitude=lon,
                altitude=alt,
                speed=speed,
                heading=heading,
                satellites=satellites,
                fix_quality=mode,
                hdop=hdop
            )
            
            self.last_fix_time = datetime.now()
            
            # Notify callbacks
            for callback in self.callbacks:
                try:
                    callback(self.current_position)
                except Exception as e:
                    logger.error(f"Error in GPS callback: {e}")
                    
        except Exception as e:
            logger.error(f"Error processing GPS data: {e}")
            
    def get_position(self) -> Optional[GPSPosition]:
        """Get current GPS position"""
        return self.current_position
        
    def has_fix(self) -> bool:
        """Check if we have a valid GPS fix"""
        if not self.current_position:
            return False
            
        # Check if fix is recent (within last 10 seconds)
        if self.last_fix_time:
            age = (datetime.now() - self.last_fix_time).total_seconds()
            return age < 10
            
        return False
        
    def wait_for_fix(self, timeout: int = 60) -> bool:
        """Wait for GPS fix with timeout"""
        logger.info(f"Waiting for GPS fix (timeout: {timeout}s)...")
        start = time.time()
        
        while time.time() - start < timeout:
            if self.has_fix():
                logger.info(f"GPS fix acquired: {self.current_position}")
                return True
            time.sleep(1)
            
        logger.warning("GPS fix timeout")
        return False


class DummyGPS:
    """Dummy GPS for testing without hardware"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.running = False
        # N7MKO approximate location
        self.current_position = GPSPosition(
            timestamp=datetime.now(),
            latitude=47.6062,  # Seattle area
            longitude=-122.3321,
            altitude=150.0,
            speed=0.0,
            heading=0.0,
            satellites=8,
            fix_quality=1,
            hdop=1.0
        )
        
    def add_callback(self, callback):
        pass
        
    def start(self):
        self.running = True
        logger.info("Dummy GPS started (fixed position)")
        return True
        
    def stop(self):
        self.running = False
        
    def get_position(self) -> Optional[GPSPosition]:
        return self.current_position
        
    def has_fix(self) -> bool:
        return True
        
    def wait_for_fix(self, timeout: int = 60) -> bool:
        return True


if __name__ == '__main__':
    # Test GPS handler
    import sys
    
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    def print_position(pos: GPSPosition):
        print(f"Position update: {pos}")
    
    config = {}
    
    if GPS_AVAILABLE:
        gps_handler = GPSHandler(config)
    else:
        print("GPS module not available, using dummy GPS")
        gps_handler = DummyGPS(config)
        
    gps_handler.add_callback(print_position)
    
    if gps_handler.start():
        try:
            print("Monitoring GPS... Press Ctrl+C to stop")
            gps_handler.wait_for_fix(timeout=30)
            
            while True:
                time.sleep(5)
                if gps_handler.has_fix():
                    pos = gps_handler.get_position()
                    print(f"Current: {pos}")
                else:
                    print("No GPS fix")
                    
        except KeyboardInterrupt:
            print("\nStopping...")
            gps_handler.stop()
    else:
        print("Failed to start GPS")
