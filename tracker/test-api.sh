#!/bin/bash

# FT8 Tracker API Test Script
# Tests GPS position updates and band changes

set -e

# Configuration
DEFAULT_HOST="127.0.0.1"
DEFAULT_PORT="8080"  # Single REST API port for all endpoints

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Parse command line arguments
HOST="${1:-$DEFAULT_HOST}"
PORT="${2:-$DEFAULT_PORT}"

# Valid bands
VALID_BANDS=("80m" "60m" "40m" "30m" "20m" "17m" "15m" "12m" "10m" "6m")

# Function to print banner
print_banner() {
    echo -e "${BLUE}"
    echo "================================"
    echo "FT8 Tracker API Test Tool"
    echo "================================"
    echo -e "${NC}"
    echo "Target: http://${HOST}:${PORT}"
    echo ""
}

# Function to print menu
print_menu() {
    echo -e "${YELLOW}Options:${NC}"
    echo "1) Send GPS position update"
    echo "2) Send band change notification"
    echo "3) Check server health"
    echo "4) Test connection"
    echo "5) Exit"
    echo ""
}

# Function to send GPS update
send_gps_update() {
    echo -e "${YELLOW}GPS Position Update${NC}"
    read -p "Enter latitude (default: 47.606): " LAT
    LAT="${LAT:-47.606}"
    
    read -p "Enter longitude (default: -122.332): " LON
    LON="${LON:--122.332}"
    
    read -p "Enter altitude in meters (optional, press Enter to skip): " ALT
    
    read -p "Enter accuracy in meters (optional, press Enter to skip): " ACC
    
    # Build JSON payload
    if [ -z "$ALT" ] && [ -z "$ACC" ]; then
        PAYLOAD=$(cat <<EOF
{
  "latitude": $LAT,
  "longitude": $LON,
  "timestamp": $(date +%s),
  "source": "test_script"
}
EOF
)
    elif [ -z "$ACC" ]; then
        PAYLOAD=$(cat <<EOF
{
  "latitude": $LAT,
  "longitude": $LON,
  "altitude": $ALT,
  "timestamp": $(date +%s),
  "source": "test_script"
}
EOF
)
    elif [ -z "$ALT" ]; then
        PAYLOAD=$(cat <<EOF
{
  "latitude": $LAT,
  "longitude": $LON,
  "accuracy": $ACC,
  "timestamp": $(date +%s),
  "source": "test_script"
}
EOF
)
    else
        PAYLOAD=$(cat <<EOF
{
  "latitude": $LAT,
  "longitude": $LON,
  "altitude": $ALT,
  "accuracy": $ACC,
  "timestamp": $(date +%s),
  "source": "test_script"
}
EOF
)
    fi
    
    echo -e "\n${BLUE}Sending GPS update:${NC}"
    echo "$PAYLOAD"
    echo ""
    
    RESPONSE=$(curl -s -X POST "http://${HOST}:${PORT}/gps" \
      -H "Content-Type: application/json" \
      -d "$PAYLOAD")
    
    echo -e "${GREEN}Response:${NC}"
    echo "$RESPONSE" | jq . 2>/dev/null || echo "$RESPONSE"
    echo ""
}

# Function to send band update
send_band_update() {
    echo -e "${YELLOW}Band Change Notification${NC}"
    echo "Valid bands: ${VALID_BANDS[*]}"
    read -p "Enter band (default: 20m): " BAND
    BAND="${BAND:-20m}"
    
    # Validate band
    if [[ ! " ${VALID_BANDS[@]} " =~ " ${BAND} " ]]; then
        echo -e "${RED}Error: Invalid band '${BAND}'${NC}"
        return 1
    fi
    
    PAYLOAD=$(cat <<EOF
{
  "band": "$BAND"
}
EOF
)
    
    echo -e "\n${BLUE}Sending band update:${NC}"
    echo "$PAYLOAD"
    echo ""
    
    RESPONSE=$(curl -s -X POST "http://${HOST}:${PORT}/band" \
      -H "Content-Type: application/json" \
      -d "$PAYLOAD")
    
    echo -e "${GREEN}Response:${NC}"
    echo "$RESPONSE" | jq . 2>/dev/null || echo "$RESPONSE"
    echo ""
}

# Function to check server health
check_health() {
    echo -e "${BLUE}Checking server health...${NC}"
    echo ""
    
    RESPONSE=$(curl -s -X GET "http://${HOST}:${PORT}/health")
    
    echo -e "${GREEN}Response:${NC}"
    echo "$RESPONSE" | jq . 2>/dev/null || echo "$RESPONSE"
    echo ""
}

# Function to test connection
test_connection() {
    echo -e "${BLUE}Testing connection to ${HOST}:${PORT}...${NC}"
    echo ""
    
    if timeout 3 bash -c "echo > /dev/tcp/${HOST}/${PORT}" 2>/dev/null; then
        echo -e "${GREEN}✓ Connection successful${NC}"
        echo ""
    else
        echo -e "${RED}✗ Connection failed${NC}"
        echo "Make sure the tracker server is running on ${HOST}:${PORT}"
        echo ""
    fi
}

# Main loop
print_banner

if [ $# -ge 1 ]; then
    echo -e "${GREEN}Using host: $HOST${NC}"
fi
if [ $# -ge 2 ]; then
    echo -e "${GREEN}Using port: $PORT${NC}"
fi
echo ""

while true; do
    print_menu
    read -p "Select option (1-5): " CHOICE
    
    case $CHOICE in
        1)
            send_gps_update
            ;;
        2)
            send_band_update
            ;;
        3)
            check_health
            ;;
        4)
            test_connection
            ;;
        5)
            echo -e "${GREEN}Exiting...${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}Invalid option. Please try again.${NC}"
            echo ""
            ;;
    esac
done
