"""
FT8 Decoder Interface
Supports multiple FT8 decoder backends (WSJT-X, ft8_lib)
"""

import subprocess
import threading
import queue
import re
import logging
from pathlib import Path
from typing import Optional, Dict, Any
from dataclasses import dataclass
from datetime import datetime

logger = logging.getLogger(__name__)


@dataclass
class FT8Decode:
    """Represents a single FT8 decode"""
    timestamp: datetime
    time_str: str  # HHMMSS
    snr: int
    dt: float
    frequency: int
    message: str
    callsign: str = ""
    grid: str = ""
    
    def to_android_format(self) -> str:
        """Format for Android Auto app"""
        return f"{self.time_str} {self.snr:+3d}  {self.dt:4.1f} {self.frequency:4d} ~ {self.message}"
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for database/JSON"""
        return {
            'timestamp': int(self.timestamp.timestamp()),
            'time_str': self.time_str,
            'snr': self.snr,
            'dt': self.dt,
            'frequency': self.frequency,
            'message': self.message,
            'callsign': self.callsign,
            'grid': self.grid
        }


class FT8Decoder:
    """Base class for FT8 decoders"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.running = False
        self.decode_queue = queue.Queue()
        self.callbacks = []
        
    def add_callback(self, callback):
        """Add callback to be called for each decode"""
        self.callbacks.append(callback)
        
    def start(self):
        """Start decoding"""
        raise NotImplementedError
        
    def stop(self):
        """Stop decoding"""
        self.running = False
        
    def _notify_decode(self, decode: FT8Decode):
        """Notify all callbacks of new decode"""
        for callback in self.callbacks:
            try:
                callback(decode)
            except Exception as e:
                logger.error(f"Error in decode callback: {e}")


class WSJTXDecoder(FT8Decoder):
    """WSJT-X log file monitor"""
    
    # WSJT-X ALL.TXT format:
    # 134500 -12  0.3 1234 ~  CQ K1ABC FN42
    WSJTX_PATTERN = re.compile(
        r'(\d{6})\s+([+-]?\d+)\s+([+-]?\d+\.\d+)\s+(\d+)\s+~?\s+(.+)'
    )
    
    def __init__(self, config: Dict[str, Any]):
        super().__init__(config)
        self.log_file = Path(config.get('log_file', 
            Path.home() / '.local/share/WSJT-X/ALL.TXT'))
        self.follow_thread = None
        
    def start(self):
        """Start monitoring WSJT-X log file"""
        if not self.log_file.exists():
            logger.warning(f"WSJT-X log file not found: {self.log_file}")
            return
            
        self.running = True
        self.follow_thread = threading.Thread(target=self._follow_log)
        self.follow_thread.daemon = True
        self.follow_thread.start()
        logger.info(f"Started monitoring WSJT-X log: {self.log_file}")
        
    def _follow_log(self):
        """Follow log file like 'tail -f'"""
        with open(self.log_file, 'r') as f:
            # Seek to end
            f.seek(0, 2)
            
            while self.running:
                line = f.readline()
                if line:
                    self._process_line(line.strip())
                else:
                    import time
                    time.sleep(0.1)
                    
    def _process_line(self, line: str):
        """Parse and process a log line"""
        match = self.WSJTX_PATTERN.match(line)
        if not match:
            return
            
        try:
            time_str = match.group(1)
            snr = int(match.group(2))
            dt = float(match.group(3))
            freq = int(match.group(4))
            message = match.group(5).strip()
            
            # Parse timestamp (HHMMSS)
            hour = int(time_str[0:2])
            minute = int(time_str[2:4])
            second = int(time_str[4:6])
            now = datetime.now()
            timestamp = now.replace(hour=hour, minute=minute, second=second, microsecond=0)
            
            # Extract callsign and grid from message
            callsign, grid = self._extract_callsign_grid(message)
            
            decode = FT8Decode(
                timestamp=timestamp,
                time_str=time_str,
                snr=snr,
                dt=dt,
                frequency=freq,
                message=message,
                callsign=callsign,
                grid=grid
            )
            
            self._notify_decode(decode)
            
        except Exception as e:
            logger.error(f"Error parsing line '{line}': {e}")
            
    def _extract_callsign_grid(self, message: str) -> tuple:
        """Extract callsign and grid square from message"""
        words = message.split()
        
        callsign = ""
        grid = ""
        
        # Look for callsign pattern
        callsign_pattern = re.compile(r'^[A-Z0-9]{1,3}[0-9][A-Z0-9]{0,3}(?:/[A-Z0-9]+)?$')
        for word in words:
            if word not in ['CQ', 'DE', 'TNX', '73', 'RRR', 'RR73']:
                if callsign_pattern.match(word):
                    callsign = word
                    break
                    
        # Look for grid square
        grid_pattern = re.compile(r'^[A-R]{2}[0-9]{2}(?:[A-X]{2})?$')
        for word in words:
            if grid_pattern.match(word):
                grid = word
                break
                
        return callsign, grid


class FT8LibDecoder(FT8Decoder):
    """ft8_lib based decoder (for future implementation)"""
    
    def __init__(self, config: Dict[str, Any]):
        super().__init__(config)
        logger.warning("ft8_lib decoder not yet implemented")
        
    def start(self):
        """Start ft8_lib decoder"""
        # TODO: Implement ft8_lib integration
        # This would decode audio directly without WSJT-X
        logger.info("ft8_lib decoder: not implemented")


def create_decoder(decoder_type: str, config: Dict[str, Any]) -> FT8Decoder:
    """Factory function to create appropriate decoder"""
    if decoder_type.lower() == 'wsjtx':
        return WSJTXDecoder(config)
    elif decoder_type.lower() == 'ft8_lib':
        return FT8LibDecoder(config)
    else:
        raise ValueError(f"Unknown decoder type: {decoder_type}")


if __name__ == '__main__':
    # Test the decoder
    import sys
    
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    def print_decode(decode: FT8Decode):
        print(f"Decode: {decode.to_android_format()}")
        print(f"  Callsign: {decode.callsign}, Grid: {decode.grid}")
    
    config = {
        'log_file': Path.home() / '.local/share/WSJT-X/ALL.TXT'
    }
    
    decoder = create_decoder('wsjtx', config)
    decoder.add_callback(print_decode)
    decoder.start()
    
    try:
        print("Monitoring WSJT-X for FT8 decodes... Press Ctrl+C to stop")
        while True:
            import time
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nStopping...")
        decoder.stop()
