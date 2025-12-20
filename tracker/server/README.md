# FT8 IoT Server

Flask application for receiving FT8 decode data from mobile tracking stations.

## Quick Start

### 1. Install Dependencies

```bash
cd server
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

### 2. Configure Database

```bash
# Create PostgreSQL database
sudo -u postgres psql
CREATE DATABASE ft8_iot;
CREATE USER ft8user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE ft8_iot TO ft8user;
\q
```

### 3. Configure Environment

```bash
cp .env.example .env
# Edit .env with your settings
nano .env
```

### 4. Initialize Database

```bash
flask init-db
```

### 5. Create Station and API Key

```bash
flask create-station N7MKO-M
```

Save the API key that's printed - you'll need it for the tracker configuration.

### 6. Run Server

Development:
```bash
flask run
```

Production (with Gunicorn):
```bash
gunicorn --bind 0.0.0.0:5000 --workers 4 app:app
```

## API Endpoints

### POST /api/ft8/upload
Upload FT8 decodes (requires authentication).

### GET /api/stations
List all tracking stations.

### GET /api/decodes
Get decode history with filters.

### GET /api/stats
System statistics.

### GET /api/health
Health check.

## Authentication

All upload requests require a Bearer token:

```bash
curl -X POST https://dx.jxqz.org/api/ft8/upload \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d @data.json
```

## Production Deployment

See [SERVER_SETUP.md](../SERVER_SETUP.md) for complete production deployment instructions including:
- Nginx configuration
- SSL certificates
- Systemd service
- Database backups
- Monitoring

## Database Schema

- **stations**: Tracking stations (mobile units)
- **api_keys**: API authentication keys
- **uploads**: Upload event tracking
- **decodes**: FT8 decodes with GPS positions

## Monitoring

```bash
# View logs
journalctl -u ft8-iot -f

# Check database
psql ft8_iot -c "SELECT station, COUNT(*) FROM decodes GROUP BY station;"

# Stats
curl http://localhost:5000/api/stats
```

## Security

- API keys are hashed using SHA-256
- Rate limiting on all endpoints
- Input validation on all data
- SSL/TLS required in production
- Database credentials in environment variables

73!
