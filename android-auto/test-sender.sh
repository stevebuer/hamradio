#!/bin/bash
# Test FT8 data sender - sends sample FT8 decodes to the app

HOST=${1:-localhost}
PORT=${2:-8080}

echo "Sending test FT8 decodes to $HOST:$PORT"
echo "Press Ctrl+C to stop"
echo ""

# Sample FT8 decodes in WSJT-X format
while true; do
    # Generate random values for testing
    TIME=$(date +%H%M%S)
    SNR=$((RANDOM % 40 - 20))
    DT=$(echo "scale=1; ($RANDOM % 20 - 10) / 10" | bc)
    FREQ=$((RANDOM % 3000 + 500))
    
    # Sample callsigns and grids
    CALLS=("K1ABC" "W2XYZ" "N7MKO" "AA0AA" "VE3XYZ" "G4ABC" "JA1ABC")
    GRIDS=("FN42" "CN87" "DN06" "EM12" "FN03" "IO91" "PM95")
    
    CALL=${CALLS[$RANDOM % ${#CALLS[@]}]}
    GRID=${GRIDS[$RANDOM % ${#GRIDS[@]}]}
    
    # Generate different message types
    MSGS=(
        "CQ $CALL $GRID"
        "$CALL W1AW FN31"
        "W1AW $CALL R-12"
        "$CALL W1AW RRR"
        "W1AW $CALL 73"
    )
    
    MSG=${MSGS[$RANDOM % ${#MSGS[@]}]}
    
    # Format and send
    DECODE="$TIME $SNR  $DT $FREQ ~ $MSG"
    echo "$DECODE"
    echo "$DECODE" | nc -w 1 $HOST $PORT
    
    # Random delay between 1-5 seconds
    sleep $((RANDOM % 4 + 1))
done
