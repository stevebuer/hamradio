"""
FT8 IoT Server - Receives uploads from mobile trackers.

This Flask application receives FT8 decode data from mobile tracking stations,
stores it in PostgreSQL, and provides APIs for querying the data.
"""
import os
import hashlib
from datetime import datetime, timedelta
from flask import Flask, request, jsonify
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
from dotenv import load_dotenv

from models import db, Station, Upload, Decode, ApiKey
from utils import calculate_distance, validate_callsign, validate_grid

# Load environment variables
load_dotenv()

# Create Flask app
app = Flask(__name__)

# Configuration
app.config['SQLALCHEMY_DATABASE_URI'] = os.getenv(
    'DATABASE_URL',
    'postgresql://ft8user:password@localhost/ft8_iot'
)
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['MAX_CONTENT_LENGTH'] = 1 * 1024 * 1024  # 1MB max upload

# Initialize database
db.init_app(app)

# Rate limiting
limiter = Limiter(
    app=app,
    key_func=get_remote_address,
    default_limits=["100 per hour"],
    storage_uri="memory://"
)


def hash_api_key(api_key):
    """Hash API key using SHA-256."""
    return hashlib.sha256(api_key.encode()).hexdigest()


def verify_api_key(api_key):
    """Verify API key and return station if valid."""
    key_hash = hash_api_key(api_key)
    api_key_obj = ApiKey.query.filter_by(key_hash=key_hash, is_active=True).first()
    
    if api_key_obj:
        api_key_obj.last_used = datetime.utcnow()
        db.session.commit()
        return api_key_obj.station
    
    return None


def require_auth(f):
    """Decorator to require API key authentication."""
    def decorated_function(*args, **kwargs):
        auth_header = request.headers.get('Authorization')
        
        if not auth_header or not auth_header.startswith('Bearer '):
            return jsonify({'error': 'Missing or invalid authorization header'}), 401
        
        api_key = auth_header[7:]  # Remove 'Bearer ' prefix
        station = verify_api_key(api_key)
        
        if not station:
            return jsonify({'error': 'Invalid API key'}), 401
        
        if not station.is_active:
            return jsonify({'error': 'Station is disabled'}), 403
        
        return f(station, *args, **kwargs)
    
    decorated_function.__name__ = f.__name__
    return decorated_function


@app.route('/api/health', methods=['GET'])
def health_check():
    """Health check endpoint."""
    return jsonify({
        'status': 'ok',
        'timestamp': datetime.utcnow().isoformat()
    })


@app.route('/api/ft8/upload', methods=['POST'])
@limiter.limit("1000 per hour")
@require_auth
def upload_decodes(station):
    """
    Upload FT8 decodes from mobile station.
    
    Request body:
    {
        "station": "N7MKO-M",
        "timestamp": 1703012345,
        "decodes": [
            {
                "timestamp": 1703012345,
                "callsign": "K1ABC",
                "grid": "FN42",
                "snr": -12,
                "frequency": 7074000,
                "band": "40m",
                "message": "CQ K1ABC FN42",
                "position": {
                    "latitude": 47.1234,
                    "longitude": -122.5678,
                    "altitude": 150,
                    "speed": 65.5,
                    "heading": 90.0
                }
            }
        ]
    }
    """
    data = request.get_json()
    
    if not data:
        return jsonify({'error': 'No data provided'}), 400
    
    # Validate required fields
    if 'decodes' not in data or not isinstance(data['decodes'], list):
        return jsonify({'error': 'Invalid decodes data'}), 400
    
    # Create upload record
    upload = Upload(
        station_id=station.id,
        decode_count=len(data['decodes']),
        ip_address=request.remote_addr,
        user_agent=request.headers.get('User-Agent', '')[:200]
    )
    db.session.add(upload)
    db.session.flush()  # Get upload.id
    
    # Process each decode
    received_count = 0
    errors = []
    
    for idx, decode_data in enumerate(data['decodes']):
        try:
            # Validate required fields
            if 'timestamp' not in decode_data:
                errors.append(f"Decode {idx}: missing timestamp")
                continue
            
            if 'callsign' not in decode_data:
                errors.append(f"Decode {idx}: missing callsign")
                continue
            
            # Validate callsign
            callsign = decode_data['callsign']
            if not validate_callsign(callsign):
                errors.append(f"Decode {idx}: invalid callsign {callsign}")
                continue
            
            # Validate grid if present
            grid = decode_data.get('grid')
            if grid and not validate_grid(grid):
                errors.append(f"Decode {idx}: invalid grid {grid}")
                grid = None
            
            # Parse timestamp
            timestamp = datetime.fromtimestamp(decode_data['timestamp'])
            
            # Extract position
            position = decode_data.get('position', {})
            latitude = position.get('latitude')
            longitude = position.get('longitude')
            altitude = position.get('altitude')
            speed = position.get('speed')
            heading = position.get('heading')
            
            # Calculate distance if both positions available
            distance = None
            if latitude and longitude and grid:
                try:
                    distance = calculate_distance(latitude, longitude, grid)
                except:
                    pass
            
            # Create decode record
            decode = Decode(
                station_id=station.id,
                upload_id=upload.id,
                timestamp=timestamp,
                callsign=callsign,
                grid=grid,
                snr=decode_data.get('snr'),
                frequency=decode_data.get('frequency'),
                band=decode_data.get('band'),
                message=decode_data.get('message', '')[:500],
                latitude=latitude,
                longitude=longitude,
                altitude=altitude,
                speed=speed,
                heading=heading,
                distance=distance
            )
            db.session.add(decode)
            received_count += 1
            
        except Exception as e:
            errors.append(f"Decode {idx}: {str(e)}")
    
    # Update station stats
    station.last_upload = datetime.utcnow()
    station.upload_count += 1
    
    # Commit all changes
    try:
        db.session.commit()
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'Database error: {str(e)}'}), 500
    
    response = {
        'status': 'success',
        'received': received_count,
        'upload_id': upload.id,
        'timestamp': datetime.utcnow().isoformat()
    }
    
    if errors:
        response['errors'] = errors
    
    return jsonify(response), 200


@app.route('/api/stations', methods=['GET'])
def list_stations():
    """Get list of all stations."""
    stations = Station.query.filter_by(is_active=True).all()
    
    result = []
    for station in stations:
        # Get latest decode with position
        latest_decode = Decode.query.filter_by(station_id=station.id)\
            .filter(Decode.latitude.isnot(None))\
            .order_by(Decode.timestamp.desc())\
            .first()
        
        result.append({
            'callsign': station.callsign,
            'last_upload': station.last_upload.isoformat() if station.last_upload else None,
            'upload_count': station.upload_count,
            'decode_count': station.decodes.count(),
            'last_position': {
                'latitude': latest_decode.latitude,
                'longitude': latest_decode.longitude
            } if latest_decode else None
        })
    
    return jsonify({'stations': result})


@app.route('/api/decodes', methods=['GET'])
@limiter.limit("200 per hour")
def list_decodes():
    """
    Get list of decodes with optional filters.
    
    Query parameters:
    - limit: Number of results (default: 100, max: 1000)
    - offset: Offset for pagination (default: 0)
    - station: Filter by station callsign
    - callsign: Filter by decoded callsign
    - band: Filter by band
    - since: Unix timestamp - decodes since this time
    """
    # Parse parameters
    limit = min(int(request.args.get('limit', 100)), 1000)
    offset = int(request.args.get('offset', 0))
    station_filter = request.args.get('station')
    callsign_filter = request.args.get('callsign')
    band_filter = request.args.get('band')
    since = request.args.get('since')
    
    # Build query
    query = Decode.query
    
    if station_filter:
        station = Station.query.filter_by(callsign=station_filter).first()
        if station:
            query = query.filter_by(station_id=station.id)
    
    if callsign_filter:
        query = query.filter_by(callsign=callsign_filter)
    
    if band_filter:
        query = query.filter_by(band=band_filter)
    
    if since:
        since_time = datetime.fromtimestamp(int(since))
        query = query.filter(Decode.timestamp >= since_time)
    
    # Get total count
    total = query.count()
    
    # Get paginated results
    decodes = query.order_by(Decode.timestamp.desc())\
        .limit(limit)\
        .offset(offset)\
        .all()
    
    return jsonify({
        'decodes': [d.to_dict() for d in decodes],
        'total': total,
        'limit': limit,
        'offset': offset
    })


@app.route('/api/stats', methods=['GET'])
def get_stats():
    """Get system statistics."""
    total_stations = Station.query.filter_by(is_active=True).count()
    total_decodes = Decode.query.count()
    total_uploads = Upload.query.count()
    
    # Recent activity (last 24 hours)
    since = datetime.utcnow() - timedelta(hours=24)
    recent_decodes = Decode.query.filter(Decode.timestamp >= since).count()
    recent_uploads = Upload.query.filter(Upload.timestamp >= since).count()
    
    # Decodes by band
    band_stats = db.session.query(
        Decode.band,
        db.func.count(Decode.id)
    ).group_by(Decode.band).all()
    
    return jsonify({
        'total_stations': total_stations,
        'total_decodes': total_decodes,
        'total_uploads': total_uploads,
        'recent_decodes_24h': recent_decodes,
        'recent_uploads_24h': recent_uploads,
        'bands': {band: count for band, count in band_stats if band}
    })


@app.cli.command('init-db')
def init_db_command():
    """Initialize database tables."""
    db.create_all()
    print('Initialized the database.')


@app.cli.command('create-station')
@app.argument('callsign')
def create_station_command(callsign):
    """Create a new station and generate API key."""
    import secrets
    
    # Check if station exists
    existing = Station.query.filter_by(callsign=callsign.upper()).first()
    if existing:
        print(f'Station {callsign} already exists.')
        return
    
    # Generate API key
    api_key = secrets.token_urlsafe(32)
    key_hash = hash_api_key(api_key)
    
    # Create station
    station = Station(
        callsign=callsign.upper(),
        api_key_hash=key_hash
    )
    db.session.add(station)
    
    # Create API key record
    api_key_obj = ApiKey(
        station=station,
        key_hash=key_hash,
        description=f'Default key for {callsign}'
    )
    db.session.add(api_key_obj)
    
    db.session.commit()
    
    print(f'Created station: {callsign}')
    print(f'API Key: {api_key}')
    print()
    print('IMPORTANT: Save this API key securely. It cannot be retrieved later.')


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)
