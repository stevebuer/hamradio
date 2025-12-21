# dx.jxqz.org Beacon Server Setup

## Overview

This guide shows how to set up the beacon server component on dx.jxqz.org to receive FT8 decode data from mobile trackers. The beacon server provides permanent cloud storage for viewing and analyzing collected data.

## Architecture

```
Mobile Tracker(s) → HTTPS POST → dx.jxqz.org API → Database → Web Dashboard
                                        ↓
                                   Authentication
                                   Rate Limiting
                                   Data Validation
```

## Requirements

### Software Stack Options

#### Option 1: Python (Flask/FastAPI) - Recommended
- Lightweight and easy to deploy
- Good Python ecosystem
- Easy integration with existing tools

#### Option 2: Node.js (Express)
- Fast and efficient
- Good for real-time features
- Large package ecosystem

#### Option 3: Go
- High performance
- Low resource usage
- Single binary deployment

## Installation - Python/Flask

### 1. System Requirements

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install -y \
    python3 \
    python3-pip \
    python3-venv \
    nginx \
    postgresql \
    postgresql-contrib \
    certbot \
    python3-certbot-nginx
```

### 2. Create Project Structure

```bash
# Create directory
sudo mkdir -p /opt/ft8-iot
cd /opt/ft8-iot

# Create virtual environment
python3 -m venv venv
source venv/bin/activate

# Install dependencies
pip install flask flask-sqlalchemy flask-limiter psycopg2-binary gunicorn
```

### 3. Database Setup

```bash
# Switch to postgres user
sudo -u postgres psql

# In PostgreSQL:
CREATE DATABASE ft8_iot;
CREATE USER ft8user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE ft8_iot TO ft8user;
\q
```

### 4. Create Flask Application

See the example code in `../beacon-server/` directory.

### 5. Configure Nginx

```nginx
# /etc/nginx/sites-available/dx.jxqz.org
server {
    listen 80;
    server_name dx.jxqz.org;

    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name dx.jxqz.org;

    # SSL Configuration
    ssl_certificate /etc/letsencrypt/live/dx.jxqz.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/dx.jxqz.org/privkey.pem;

    # API endpoint
    location /api/ {
        proxy_pass http://127.0.0.1:5000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Increase timeout for uploads
        proxy_read_timeout 300;
        proxy_connect_timeout 300;
        proxy_send_timeout 300;
    }

    # Web dashboard (optional)
    location / {
        root /var/www/dx.jxqz.org;
        index index.html;
        try_files $uri $uri/ =404;
    }
}
```

Enable site:
```bash
sudo ln -s /etc/nginx/sites-available/dx.jxqz.org /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 6. Get SSL Certificate

```bash
sudo certbot --nginx -d dx.jxqz.org
```

### 7. Create Systemd Service

```ini
# /etc/systemd/system/ft8-iot.service
[Unit]
Description=FT8 IoT API Server
After=network.target postgresql.service

[Service]
Type=notify
User=www-data
Group=www-data
WorkingDirectory=/opt/ft8-iot
Environment="PATH=/opt/ft8-iot/venv/bin"
ExecStart=/opt/ft8-iot/venv/bin/gunicorn \
    --bind 127.0.0.1:5000 \
    --workers 4 \
    --timeout 300 \
    --access-logfile /var/log/ft8-iot/access.log \
    --error-logfile /var/log/ft8-iot/error.log \
    app:app

Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo mkdir -p /var/log/ft8-iot
sudo chown www-data:www-data /var/log/ft8-iot
sudo systemctl daemon-reload
sudo systemctl enable ft8-iot
sudo systemctl start ft8-iot
```

## API Endpoint Specification

### POST /api/ft8/upload

Upload FT8 decode data.

**Authentication:** Bearer token in Authorization header

**Request:**
```json
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
```

**Response:**
```json
{
  "status": "success",
  "received": 1,
  "timestamp": 1703012350
}
```

### GET /api/stations

List all stations.

**Response:**
```json
{
  "stations": [
    {
      "callsign": "N7MKO-M",
      "last_seen": 1703012345,
      "decode_count": 1234,
      "last_position": {
        "latitude": 47.1234,
        "longitude": -122.5678
      }
    }
  ]
}
```

### GET /api/decodes

Get recent decodes (with pagination).

**Parameters:**
- `limit`: Number of results (default: 100, max: 1000)
- `offset`: Offset for pagination
- `station`: Filter by station callsign
- `callsign`: Filter by decoded callsign
- `since`: Unix timestamp - decodes since this time

**Response:**
```json
{
  "decodes": [...],
  "total": 5000,
  "limit": 100,
  "offset": 0
}
```

## Security Considerations

### API Key Generation

```python
import secrets

def generate_api_key():
    return secrets.token_urlsafe(32)

# Generate key
print(generate_api_key())
```

Store API keys hashed in database:
```python
import hashlib

def hash_api_key(api_key):
    return hashlib.sha256(api_key.encode()).hexdigest()
```

### Rate Limiting

Implement rate limiting to prevent abuse:
- Per IP: 100 requests/hour
- Per API key: 1000 requests/hour
- Upload size limit: 1MB

### Input Validation

- Validate all input data
- Check coordinate bounds
- Verify callsign format
- Sanitize SQL inputs (use ORM)

## Monitoring

### Log Files

```bash
# API access logs
tail -f /var/log/ft8-iot/access.log

# Error logs
tail -f /var/log/ft8-iot/error.log

# System logs
journalctl -u ft8-iot -f
```

### Database Queries

```sql
-- Recent uploads
SELECT station, COUNT(*), MAX(timestamp) as last_upload
FROM uploads
GROUP BY station
ORDER BY last_upload DESC;

-- Total decodes
SELECT COUNT(*) FROM decodes;

-- Decodes per band
SELECT band, COUNT(*) 
FROM decodes 
GROUP BY band 
ORDER BY COUNT(*) DESC;
```

## Backup Strategy

```bash
# Database backup script
#!/bin/bash
# /opt/ft8-iot/backup.sh

BACKUP_DIR="/backup/ft8-iot"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# Backup database
pg_dump -U ft8user ft8_iot | gzip > $BACKUP_DIR/ft8_iot_$DATE.sql.gz

# Keep only last 30 days
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete

# Add to cron:
# 0 2 * * * /opt/ft8-iot/backup.sh
```

## Performance Tuning

### PostgreSQL Configuration

```ini
# /etc/postgresql/13/main/postgresql.conf

# Memory
shared_buffers = 256MB
effective_cache_size = 1GB
work_mem = 16MB

# Connections
max_connections = 100

# Performance
random_page_cost = 1.1
effective_io_concurrency = 200
```

### Database Indexes

```sql
-- Important indexes
CREATE INDEX idx_decodes_timestamp ON decodes(timestamp DESC);
CREATE INDEX idx_decodes_station ON decodes(station_id);
CREATE INDEX idx_decodes_callsign ON decodes(callsign);
CREATE INDEX idx_positions_timestamp ON positions(timestamp DESC);
```

## Alternative: Docker Deployment

```yaml
# docker-compose.yml
version: '3.8'

services:
  db:
    image: postgres:13
    environment:
      POSTGRES_DB: ft8_iot
      POSTGRES_USER: ft8user
      POSTGRES_PASSWORD: your_secure_password
    volumes:
      - postgres_data:/var/lib/postgresql/data

  api:
    build: .
    ports:
      - "5000:5000"
    environment:
      DATABASE_URL: postgresql://ft8user:your_secure_password@db/ft8_iot
    depends_on:
      - db

volumes:
  postgres_data:
```

## Future: RabbitMQ Message Broker Architecture

### Why RabbitMQ?

For production IoT deployments with multiple mobile stations, RabbitMQ provides enterprise-grade messaging:

**Advantages:**
- **Message Persistence**: Decodes queued during server downtime
- **Load Balancing**: Distribute processing across multiple consumers
- **Fan-out**: Send data to multiple services simultaneously
- **Priority Queuing**: Handle alerts or special events first
- **Dead Letter Exchanges**: Automatic retry and error handling
- **Monitoring**: Built-in management UI and metrics

**Use Cases:**
- Multi-station networks (ham radio club deployments)
- Real-time dashboards + database storage simultaneously
- Complex processing pipelines (ML analysis, propagation studies)
- Integration with existing RabbitMQ infrastructure

### Architecture Diagram

```
┌──────────────┐
│ Mobile       │
│ Tracker 1    │──┐
└──────────────┘  │
                  ├──→ [RabbitMQ] ──→ Exchanges ──┬──→ Queue 1 ──→ Database Writer
┌──────────────┐  │                               ├──→ Queue 2 ──→ Web Dashboard
│ Mobile       │  │                               ├──→ Queue 3 ──→ Analytics
│ Tracker 2    │──┘                               └──→ Queue 4 ──→ Alert System
└──────────────┘
```

### Docker Compose with RabbitMQ

```yaml
version: '3.8'

services:
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"   # AMQP
      - "15672:15672" # Management UI
    environment:
      RABBITMQ_DEFAULT_USER: ft8iot
      RABBITMQ_DEFAULT_PASS: secure_password
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq

  db:
    image: postgres:13
    environment:
      POSTGRES_DB: ft8_iot
      POSTGRES_USER: ft8user
      POSTGRES_PASSWORD: your_secure_password
    volumes:
      - postgres_data:/var/lib/postgresql/data

  # Producer: Receives HTTP uploads, publishes to RabbitMQ
  api:
    build: .
    ports:
      - "5000:5000"
    environment:
      RABBITMQ_URL: amqp://ft8iot:secure_password@rabbitmq:5672/
      DATABASE_URL: postgresql://ft8user:your_secure_password@db/ft8_iot
    depends_on:
      - rabbitmq
      - db

  # Consumer: Reads from RabbitMQ, writes to database
  consumer:
    build: ./consumer
    environment:
      RABBITMQ_URL: amqp://ft8iot:secure_password@rabbitmq:5672/
      DATABASE_URL: postgresql://ft8user:your_secure_password@db/ft8_iot
    depends_on:
      - rabbitmq
      - db
    deploy:
      replicas: 3  # Scale consumers for load balancing

volumes:
  rabbitmq_data:
  postgres_data:
```

### Mobile Tracker Changes

Minimal changes needed to tracker code:

```python
# Add to requirements.txt
pika==1.3.2  # RabbitMQ client

# In iot_uploader.py
import pika
import json

class RabbitMQUploader:
    def __init__(self, config):
        self.url = config['rabbitmq_url']
        self.exchange = 'ft8_decodes'
        
    def connect(self):
        params = pika.URLParameters(self.url)
        self.connection = pika.BlockingConnection(params)
        self.channel = self.connection.channel()
        self.channel.exchange_declare(exchange=self.exchange, 
                                     exchange_type='fanout',
                                     durable=True)
    
    def publish_decode(self, decode_data):
        message = json.dumps(decode_data)
        self.channel.basic_publish(
            exchange=self.exchange,
            routing_key='',
            body=message,
            properties=pika.BasicProperties(
                delivery_mode=2,  # Persistent
                content_type='application/json'
            )
        )
```

### Server Consumer

```python
# consumer.py
import pika
import json
from models import db, Decode

def callback(ch, method, properties, body):
    """Process incoming decode message."""
    data = json.loads(body)
    
    # Store in database
    decode = Decode(**data)
    db.session.add(decode)
    db.session.commit()
    
    # Acknowledge message
    ch.basic_ack(delivery_tag=method.delivery_tag)

def main():
    # Connect to RabbitMQ
    params = pika.URLParameters(os.getenv('RABBITMQ_URL'))
    connection = pika.BlockingConnection(params)
    channel = connection.channel()
    
    # Declare exchange and queue
    channel.exchange_declare(exchange='ft8_decodes', 
                           exchange_type='fanout',
                           durable=True)
    channel.queue_declare(queue='database_writer', durable=True)
    channel.queue_bind(exchange='ft8_decodes', 
                      queue='database_writer')
    
    # Set QoS - only 10 unacked messages per consumer
    channel.basic_qos(prefetch_count=10)
    
    # Start consuming
    channel.basic_consume(queue='database_writer',
                         on_message_callback=callback)
    
    print('Waiting for messages...')
    channel.start_consuming()

if __name__ == '__main__':
    main()
```

### When to Use RabbitMQ vs HTTP

**Use HTTP (current):**
- Single or few mobile stations
- Simple deployment
- Direct server communication
- Low message volume (<1000/hour)

**Use RabbitMQ:**
- Multiple mobile stations (>5)
- Need server maintenance without data loss
- Multiple data consumers
- High volume (>10,000/hour)
- Complex routing requirements
- Integration with existing message infrastructure

### Monitoring RabbitMQ

```bash
# Access management UI
http://dx.jxqz.org:15672

# Command line stats
rabbitmqctl list_queues name messages consumers
rabbitmqctl list_exchanges

# Monitor specific queue
watch -n 1 'rabbitmqctl list_queues | grep ft8'
```

### Migration Path

Hybrid approach during transition:

1. Deploy RabbitMQ alongside HTTP API
2. Update some trackers to use RabbitMQ
3. Keep HTTP API for backwards compatibility
4. Eventually deprecate HTTP once all migrated

This demonstrates modern IoT messaging patterns suitable for enterprise environments while maintaining operational simplicity for initial deployments.

## Web Dashboard (Optional)

Create a simple web interface to view decodes:

```html
<!-- /var/www/dx.jxqz.org/index.html -->
<!DOCTYPE html>
<html>
<head>
    <title>DX.JXQZ.ORG - FT8 Tracker</title>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css"/>
    <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
</head>
<body>
    <h1>FT8 Mobile Tracker Network</h1>
    <div id="map" style="height: 600px;"></div>
    <div id="recent-decodes"></div>
    
    <script>
        // Initialize map
        const map = L.map('map').setView([47.6, -122.3], 8);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);
        
        // Fetch and display recent decodes
        fetch('/api/decodes?limit=50')
            .then(r => r.json())
            .then(data => {
                // Display on map
                data.decodes.forEach(d => {
                    if (d.latitude && d.longitude) {
                        L.marker([d.latitude, d.longitude])
                            .bindPopup(`${d.callsign} - ${d.grid}`)
                            .addTo(map);
                    }
                });
            });
    </script>
</body>
</html>
```

## Testing

```bash
# Test API endpoint
curl -X POST https://dx.jxqz.org/api/ft8/upload \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -d '{
    "station": "TEST",
    "timestamp": 1703012345,
    "decodes": [{
      "timestamp": 1703012345,
      "callsign": "K1ABC",
      "grid": "FN42",
      "snr": -12,
      "frequency": 7074000,
      "band": "40m",
      "message": "CQ K1ABC FN42"
    }]
  }'
```

## Maintenance

```bash
# Check service status
sudo systemctl status ft8-iot

# Restart service
sudo systemctl restart ft8-iot

# View logs
journalctl -u ft8-iot --since today

# Database maintenance
sudo -u postgres psql ft8_iot -c "VACUUM ANALYZE;"
```

## See Also

- `beacon-server/app.py` - Flask application code
- `beacon-server/models.py` - Database models
- `beacon-server/api.py` - API endpoints
- `beacon-server/requirements.txt` - Python dependencies

For complete beacon server implementation code, see the `../beacon-server/` directory.

73!
