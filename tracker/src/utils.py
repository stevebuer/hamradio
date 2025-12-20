"""
Utility functions for FT8 tracker
"""

import logging
from typing import Dict, Any

logger = logging.getLogger(__name__)


def frequency_to_band(frequency_hz: int) -> str:
    """Convert frequency in Hz to ham band name"""
    freq_mhz = frequency_hz / 1_000_000
    
    bands = [
        (1.8, 2.0, "160m"),
        (3.5, 4.0, "80m"),
        (5.3, 5.4, "60m"),
        (7.0, 7.3, "40m"),
        (10.1, 10.15, "30m"),
        (14.0, 14.35, "20m"),
        (18.068, 18.168, "17m"),
        (21.0, 21.45, "15m"),
        (24.89, 24.99, "12m"),
        (28.0, 29.7, "10m"),
    ]
    
    for start, end, band in bands:
        if start <= freq_mhz <= end:
            return band
            
    return "UNKNOWN"


def maidenhead_to_latlon(grid: str) -> tuple:
    """Convert Maidenhead grid square to lat/lon (center of square)"""
    if len(grid) < 4:
        return None, None
        
    grid = grid.upper()
    
    # Field (20° longitude, 10° latitude)
    lon = (ord(grid[0]) - ord('A')) * 20 - 180
    lat = (ord(grid[1]) - ord('A')) * 10 - 90
    
    # Square (2° longitude, 1° latitude)
    lon += int(grid[2]) * 2
    lat += int(grid[3]) * 1
    
    # Subsquare (if provided - 5' longitude, 2.5' latitude)
    if len(grid) >= 6:
        lon += (ord(grid[4]) - ord('A')) / 12
        lat += (ord(grid[5]) - ord('A')) / 24
    else:
        # Center of square
        lon += 1
        lat += 0.5
        
    return lat, lon


def calculate_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Calculate distance in km between two points using Haversine formula"""
    from math import radians, sin, cos, sqrt, atan2
    
    R = 6371  # Earth's radius in km
    
    lat1_rad = radians(lat1)
    lat2_rad = radians(lat2)
    dlon = radians(lon2 - lon1)
    dlat = radians(lat2 - lat1)
    
    a = sin(dlat/2)**2 + cos(lat1_rad) * cos(lat2_rad) * sin(dlon/2)**2
    c = 2 * atan2(sqrt(a), sqrt(1-a))
    
    return R * c


def calculate_bearing(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Calculate bearing in degrees from point 1 to point 2"""
    from math import radians, sin, cos, atan2, degrees
    
    lat1_rad = radians(lat1)
    lat2_rad = radians(lat2)
    dlon = radians(lon2 - lon1)
    
    x = sin(dlon) * cos(lat2_rad)
    y = cos(lat1_rad) * sin(lat2_rad) - sin(lat1_rad) * cos(lat2_rad) * cos(dlon)
    
    bearing = atan2(x, y)
    bearing = degrees(bearing)
    bearing = (bearing + 360) % 360
    
    return bearing


def format_callsign(callsign: str) -> str:
    """Format callsign consistently"""
    return callsign.upper().strip()


def validate_grid(grid: str) -> bool:
    """Validate Maidenhead grid square format"""
    import re
    
    # 4 or 6 character grid
    pattern = r'^[A-R]{2}[0-9]{2}([A-X]{2})?$'
    return bool(re.match(pattern, grid.upper()))


if __name__ == '__main__':
    # Test utilities
    print("Utility Functions Test")
    print("=" * 40)
    
    # Test frequency to band
    print(f"7074000 Hz = {frequency_to_band(7074000)}")
    print(f"14074000 Hz = {frequency_to_band(14074000)}")
    
    # Test Maidenhead
    lat, lon = maidenhead_to_latlon("FN42")
    print(f"FN42 = {lat:.2f}, {lon:.2f}")
    
    # Test distance
    # Seattle to New York
    dist = calculate_distance(47.6062, -122.3321, 40.7128, -74.0060)
    print(f"Seattle to NYC = {dist:.0f} km")
    
    # Test bearing
    bearing = calculate_bearing(47.6062, -122.3321, 40.7128, -74.0060)
    print(f"Seattle to NYC bearing = {bearing:.0f}°")
    
    # Test grid validation
    print(f"FN42 valid? {validate_grid('FN42')}")
    print(f"XX99 valid? {validate_grid('XX99')}")
