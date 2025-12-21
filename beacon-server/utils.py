"""
Utility functions for FT8 IoT server.
"""
import re
import math


def validate_callsign(callsign):
    """
    Validate amateur radio callsign format.
    
    Args:
        callsign: Callsign string to validate
        
    Returns:
        True if valid, False otherwise
    """
    if not callsign or len(callsign) < 3 or len(callsign) > 20:
        return False
    
    # Basic pattern: prefix (letters/digits), digit, suffix (letters)
    # Also supports SSID (callsign-N)
    pattern = r'^[A-Z0-9]{1,3}[0-9][A-Z0-9]{0,4}(-[A-Z0-9]{1,2})?$'
    return bool(re.match(pattern, callsign.upper()))


def validate_grid(grid):
    """
    Validate Maidenhead grid square format.
    
    Args:
        grid: Grid square string (e.g., "FN42", "FN42ab")
        
    Returns:
        True if valid, False otherwise
    """
    if not grid:
        return False
    
    grid = grid.upper()
    
    # Support 4 or 6 character grids
    if len(grid) not in [4, 6]:
        return False
    
    # Check format: 2 letters, 2 digits, optional 2 letters
    if len(grid) >= 4:
        if not (grid[0].isalpha() and grid[1].isalpha() and
                grid[2].isdigit() and grid[3].isdigit()):
            return False
        
        # Validate ranges
        if not ('A' <= grid[0] <= 'R' and 'A' <= grid[1] <= 'R'):
            return False
    
    if len(grid) == 6:
        if not (grid[4].isalpha() and grid[5].isalpha()):
            return False
        if not ('A' <= grid[4].upper() <= 'X' and 'A' <= grid[5].upper() <= 'X'):
            return False
    
    return True


def maidenhead_to_latlon(grid):
    """
    Convert Maidenhead grid square to latitude/longitude.
    
    Args:
        grid: Grid square string (e.g., "FN42")
        
    Returns:
        Tuple of (latitude, longitude) or None if invalid
    """
    if not validate_grid(grid):
        return None
    
    grid = grid.upper()
    
    # Field (first 2 letters)
    lon = (ord(grid[0]) - ord('A')) * 20 - 180
    lat = (ord(grid[1]) - ord('A')) * 10 - 90
    
    # Square (next 2 digits)
    lon += int(grid[2]) * 2
    lat += int(grid[3])
    
    # Subsquare (optional 2 letters)
    if len(grid) >= 6:
        lon += (ord(grid[4]) - ord('A')) * (2/24)
        lat += (ord(grid[5]) - ord('A')) * (1/24)
        # Center of subsquare
        lon += 1/24
        lat += 1/48
    else:
        # Center of square
        lon += 1
        lat += 0.5
    
    return (lat, lon)


def calculate_distance(lat1, lon1, grid):
    """
    Calculate distance between a position and a grid square.
    
    Args:
        lat1: Latitude of first position
        lon1: Longitude of first position
        grid: Maidenhead grid square of second position
        
    Returns:
        Distance in kilometers or None if invalid
    """
    coords = maidenhead_to_latlon(grid)
    if not coords:
        return None
    
    lat2, lon2 = coords
    
    # Haversine formula
    R = 6371  # Earth radius in km
    
    lat1_rad = math.radians(lat1)
    lat2_rad = math.radians(lat2)
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    
    a = (math.sin(dlat/2) ** 2 +
         math.cos(lat1_rad) * math.cos(lat2_rad) *
         math.sin(dlon/2) ** 2)
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
    
    return R * c


def format_frequency(freq_hz):
    """
    Format frequency in Hz to readable string.
    
    Args:
        freq_hz: Frequency in Hz
        
    Returns:
        Formatted string (e.g., "7.074 MHz")
    """
    if freq_hz >= 1e9:
        return f"{freq_hz/1e9:.3f} GHz"
    elif freq_hz >= 1e6:
        return f"{freq_hz/1e6:.3f} MHz"
    elif freq_hz >= 1e3:
        return f"{freq_hz/1e3:.3f} kHz"
    else:
        return f"{freq_hz} Hz"


def get_band_from_frequency(freq_hz):
    """
    Determine amateur radio band from frequency.
    
    Args:
        freq_hz: Frequency in Hz
        
    Returns:
        Band string (e.g., "40m") or None
    """
    bands = [
        (1800000, 2000000, "160m"),
        (3500000, 4000000, "80m"),
        (5330000, 5405000, "60m"),
        (7000000, 7300000, "40m"),
        (10100000, 10150000, "30m"),
        (14000000, 14350000, "20m"),
        (18068000, 18168000, "17m"),
        (21000000, 21450000, "15m"),
        (24890000, 24990000, "12m"),
        (28000000, 29700000, "10m"),
        (50000000, 54000000, "6m"),
        (144000000, 148000000, "2m"),
        (420000000, 450000000, "70cm"),
    ]
    
    for start, end, band in bands:
        if start <= freq_hz <= end:
            return band
    
    return None
