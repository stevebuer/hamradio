#!/bin/bash
# Quick local testing script for FT8 Beacon Server
# This runs the Flask server locally without needing to build/install dpkg

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/../beacon-server"

echo "=== FT8 Beacon Server - Local Testing ==="
echo

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
fi

# Activate virtual environment
echo "Activating virtual environment..."
source venv/bin/activate

# Install/update dependencies
echo "Installing dependencies..."
pip install -q --upgrade pip
pip install -q -r requirements.txt

# Check if .env exists
if [ ! -f ".env" ]; then
    echo
    echo "WARNING: No .env file found!"
    echo "Creating default .env file..."
    cat > .env <<EOF
# Database Configuration
DATABASE_URL=postgresql://ft8user:password@localhost/ft8_iot

# Flask Configuration
FLASK_APP=app.py
FLASK_ENV=development
SECRET_KEY=$(python3 -c 'import secrets; print(secrets.token_hex(32))')

# Server Configuration
HOST=0.0.0.0
PORT=5000
EOF
    echo "Created .env - you may want to edit database settings"
    echo
fi

# Check if database exists
echo
echo "Checking database connection..."
if ! python3 -c "from app import app, db; app.app_context().push(); db.engine.connect()" 2>/dev/null; then
    echo
    echo "WARNING: Cannot connect to database!"
    echo "You may need to:"
    echo "  1. Install PostgreSQL: sudo apt-get install postgresql"
    echo "  2. Create database and user (see server/README.md)"
    echo "  3. Initialize database: flask init-db"
    echo
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo
echo "=== Starting Flask Server ==="
echo "Server will be available at: http://localhost:5000"
echo "Press Ctrl+C to stop"
echo

# Run Flask in development mode
export FLASK_APP=app.py
export FLASK_ENV=development
flask run --host=0.0.0.0 --port=5000
