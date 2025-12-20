"""
Database Layer
SQLite database for storing FT8 decodes with GPS positions
"""

import sqlite3
import logging
from pathlib import Path
from datetime import datetime
from typing import List, Optional, Dict, Any
from contextlib import contextmanager

logger = logging.getLogger(__name__)


class Database:
    """SQLite database for FT8 tracker"""
    
    def __init__(self, db_path: str):
        self.db_path = Path(db_path)
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        self._init_database()
        
    def _init_database(self):
        """Initialize database schema"""
        with self.get_connection() as conn:
            cursor = conn.cursor()
            
            # Create decodes table
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS decodes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    time_str TEXT,
                    callsign TEXT,
                    grid TEXT,
                    snr INTEGER,
                    dt REAL,
                    frequency INTEGER,
                    band TEXT,
                    message TEXT,
                    latitude REAL,
                    longitude REAL,
                    altitude REAL,
                    speed REAL,
                    heading REAL,
                    uploaded INTEGER DEFAULT 0,
                    upload_timestamp INTEGER,
                    created_at INTEGER DEFAULT (strftime('%s', 'now'))
                )
            ''')
            
            # Create indices
            cursor.execute('''
                CREATE INDEX IF NOT EXISTS idx_timestamp 
                ON decodes(timestamp)
            ''')
            
            cursor.execute('''
                CREATE INDEX IF NOT EXISTS idx_uploaded 
                ON decodes(uploaded)
            ''')
            
            cursor.execute('''
                CREATE INDEX IF NOT EXISTS idx_callsign 
                ON decodes(callsign)
            ''')
            
            # Create stats table
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS stats (
                    date TEXT PRIMARY KEY,
                    decode_count INTEGER DEFAULT 0,
                    unique_callsigns INTEGER DEFAULT 0,
                    bands_worked TEXT,
                    distance_traveled REAL DEFAULT 0
                )
            ''')
            
            conn.commit()
            
        logger.info(f"Database initialized: {self.db_path}")
        
    @contextmanager
    def get_connection(self):
        """Context manager for database connections"""
        conn = sqlite3.connect(str(self.db_path))
        conn.row_factory = sqlite3.Row
        try:
            yield conn
        finally:
            conn.close()
            
    def insert_decode(self, decode_data: Dict[str, Any], gps_data: Optional[Dict[str, Any]] = None) -> int:
        """Insert a decode record"""
        with self.get_connection() as conn:
            cursor = conn.cursor()
            
            # Prepare data
            latitude = gps_data['latitude'] if gps_data else None
            longitude = gps_data['longitude'] if gps_data else None
            altitude = gps_data.get('altitude') if gps_data else None
            speed = gps_data.get('speed') if gps_data else None
            heading = gps_data.get('heading') if gps_data else None
            
            # Determine band from frequency
            band = self._frequency_to_band(decode_data.get('frequency', 0))
            
            cursor.execute('''
                INSERT INTO decodes (
                    timestamp, time_str, callsign, grid, snr, dt, frequency, band,
                    message, latitude, longitude, altitude, speed, heading
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                decode_data['timestamp'],
                decode_data.get('time_str', ''),
                decode_data.get('callsign', ''),
                decode_data.get('grid', ''),
                decode_data.get('snr', 0),
                decode_data.get('dt', 0.0),
                decode_data.get('frequency', 0),
                band,
                decode_data.get('message', ''),
                latitude,
                longitude,
                altitude,
                speed,
                heading
            ))
            
            conn.commit()
            decode_id = cursor.lastrowid
            
        logger.debug(f"Inserted decode {decode_id}: {decode_data.get('callsign', 'UNKNOWN')}")
        return decode_id
        
    def _frequency_to_band(self, frequency: int) -> str:
        """Convert frequency to band name"""
        # Frequency is typically the offset within the band
        # We'd need to know the actual dial frequency to determine band
        # For now, return empty string
        # This should be enhanced based on your radio's frequency reporting
        return ""
        
    def get_recent_decodes(self, limit: int = 100) -> List[Dict[str, Any]]:
        """Get recent decodes"""
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute('''
                SELECT * FROM decodes 
                ORDER BY timestamp DESC 
                LIMIT ?
            ''', (limit,))
            
            rows = cursor.fetchall()
            return [dict(row) for row in rows]
            
    def get_unuploaded_decodes(self, limit: int = 100) -> List[Dict[str, Any]]:
        """Get decodes that haven't been uploaded yet"""
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute('''
                SELECT * FROM decodes 
                WHERE uploaded = 0 
                ORDER BY timestamp ASC 
                LIMIT ?
            ''', (limit,))
            
            rows = cursor.fetchall()
            return [dict(row) for row in rows]
            
    def mark_uploaded(self, decode_ids: List[int]):
        """Mark decodes as uploaded"""
        if not decode_ids:
            return
            
        with self.get_connection() as conn:
            cursor = conn.cursor()
            placeholders = ','.join('?' * len(decode_ids))
            cursor.execute(f'''
                UPDATE decodes 
                SET uploaded = 1, upload_timestamp = ? 
                WHERE id IN ({placeholders})
            ''', [int(datetime.now().timestamp())] + decode_ids)
            
            conn.commit()
            
        logger.info(f"Marked {len(decode_ids)} decodes as uploaded")
        
    def get_stats(self, since_timestamp: Optional[int] = None) -> Dict[str, Any]:
        """Get statistics"""
        with self.get_connection() as conn:
            cursor = conn.cursor()
            
            where_clause = ""
            params = []
            if since_timestamp:
                where_clause = "WHERE timestamp >= ?"
                params = [since_timestamp]
                
            # Total decodes
            cursor.execute(f'''
                SELECT COUNT(*) as count FROM decodes {where_clause}
            ''', params)
            total_decodes = cursor.fetchone()['count']
            
            # Unique callsigns
            cursor.execute(f'''
                SELECT COUNT(DISTINCT callsign) as count 
                FROM decodes 
                {where_clause}
                AND callsign != ''
            ''', params)
            unique_callsigns = cursor.fetchone()['count']
            
            # Uploaded count
            cursor.execute(f'''
                SELECT COUNT(*) as count 
                FROM decodes 
                {where_clause}
                AND uploaded = 1
            ''', params)
            uploaded = cursor.fetchone()['count']
            
            # Bands worked
            cursor.execute(f'''
                SELECT DISTINCT band 
                FROM decodes 
                {where_clause}
                AND band != ''
            ''', params)
            bands = [row['band'] for row in cursor.fetchall()]
            
            return {
                'total_decodes': total_decodes,
                'unique_callsigns': unique_callsigns,
                'uploaded': uploaded,
                'pending_upload': total_decodes - uploaded,
                'bands': bands
            }
            
    def cleanup_old_records(self, days: int = 30):
        """Delete records older than specified days"""
        cutoff = int(datetime.now().timestamp()) - (days * 86400)
        
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute('''
                DELETE FROM decodes 
                WHERE timestamp < ? AND uploaded = 1
            ''', (cutoff,))
            
            deleted = cursor.rowcount
            conn.commit()
            
        logger.info(f"Cleaned up {deleted} old records")
        return deleted


if __name__ == '__main__':
    # Test database
    import tempfile
    import os
    
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    # Create temp database
    db_file = tempfile.mktemp(suffix='.db')
    print(f"Test database: {db_file}")
    
    try:
        db = Database(db_file)
        
        # Insert test decode
        decode_data = {
            'timestamp': int(datetime.now().timestamp()),
            'time_str': '134500',
            'callsign': 'K1ABC',
            'grid': 'FN42',
            'snr': -12,
            'dt': 0.3,
            'frequency': 1234,
            'message': 'CQ K1ABC FN42'
        }
        
        gps_data = {
            'latitude': 47.6062,
            'longitude': -122.3321,
            'altitude': 150.0,
            'speed': 65.5,
            'heading': 90.0
        }
        
        decode_id = db.insert_decode(decode_data, gps_data)
        print(f"Inserted decode: {decode_id}")
        
        # Get recent
        recent = db.get_recent_decodes(10)
        print(f"Recent decodes: {len(recent)}")
        for decode in recent:
            print(f"  {decode['callsign']} at {decode['latitude']}, {decode['longitude']}")
            
        # Get stats
        stats = db.get_stats()
        print(f"Stats: {stats}")
        
    finally:
        if os.path.exists(db_file):
            os.unlink(db_file)
            print(f"Cleaned up test database")
