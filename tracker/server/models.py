"""
Database models for FT8 IoT server.
"""
from datetime import datetime
from flask_sqlalchemy import SQLAlchemy

db = SQLAlchemy()


class Station(db.Model):
    """Tracking station (mobile unit)."""
    __tablename__ = 'stations'
    
    id = db.Column(db.Integer, primary_key=True)
    callsign = db.Column(db.String(20), unique=True, nullable=False, index=True)
    api_key_hash = db.Column(db.String(64), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    last_upload = db.Column(db.DateTime)
    upload_count = db.Column(db.Integer, default=0)
    is_active = db.Column(db.Boolean, default=True)
    
    # Relationships
    uploads = db.relationship('Upload', backref='station', lazy='dynamic')
    decodes = db.relationship('Decode', backref='station', lazy='dynamic')
    
    def __repr__(self):
        return f'<Station {self.callsign}>'


class Upload(db.Model):
    """Track upload events."""
    __tablename__ = 'uploads'
    
    id = db.Column(db.Integer, primary_key=True)
    station_id = db.Column(db.Integer, db.ForeignKey('stations.id'), nullable=False, index=True)
    timestamp = db.Column(db.DateTime, default=datetime.utcnow, index=True)
    decode_count = db.Column(db.Integer)
    ip_address = db.Column(db.String(45))  # IPv6 compatible
    user_agent = db.Column(db.String(200))
    
    def __repr__(self):
        return f'<Upload {self.id} from {self.station.callsign}>'


class Decode(db.Model):
    """FT8 decode with position."""
    __tablename__ = 'decodes'
    
    id = db.Column(db.Integer, primary_key=True)
    station_id = db.Column(db.Integer, db.ForeignKey('stations.id'), nullable=False, index=True)
    upload_id = db.Column(db.Integer, db.ForeignKey('uploads.id'), index=True)
    
    # Decode information
    timestamp = db.Column(db.DateTime, nullable=False, index=True)
    callsign = db.Column(db.String(20), nullable=False, index=True)
    grid = db.Column(db.String(10))
    snr = db.Column(db.Integer)
    frequency = db.Column(db.BigInteger)  # Hz
    band = db.Column(db.String(10), index=True)
    message = db.Column(db.Text)
    
    # GPS position
    latitude = db.Column(db.Float)
    longitude = db.Column(db.Float)
    altitude = db.Column(db.Float)
    speed = db.Column(db.Float)  # km/h
    heading = db.Column(db.Float)  # degrees
    
    # Calculated fields
    distance = db.Column(db.Float)  # km to decoded station
    
    def __repr__(self):
        return f'<Decode {self.callsign} by {self.station.callsign}>'
    
    def to_dict(self):
        """Convert to dictionary for API response."""
        return {
            'id': self.id,
            'station': self.station.callsign,
            'timestamp': self.timestamp.isoformat(),
            'callsign': self.callsign,
            'grid': self.grid,
            'snr': self.snr,
            'frequency': self.frequency,
            'band': self.band,
            'message': self.message,
            'position': {
                'latitude': self.latitude,
                'longitude': self.longitude,
                'altitude': self.altitude,
                'speed': self.speed,
                'heading': self.heading
            } if self.latitude else None,
            'distance': self.distance
        }


class ApiKey(db.Model):
    """API key management."""
    __tablename__ = 'api_keys'
    
    id = db.Column(db.Integer, primary_key=True)
    key_hash = db.Column(db.String(64), unique=True, nullable=False, index=True)
    station_id = db.Column(db.Integer, db.ForeignKey('stations.id'), nullable=False)
    description = db.Column(db.String(200))
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    last_used = db.Column(db.DateTime)
    is_active = db.Column(db.Boolean, default=True)
    
    station = db.relationship('Station', backref='api_keys')
    
    def __repr__(self):
        return f'<ApiKey {self.id} for {self.station.callsign}>'
