#!/bin/bash
#
# Steve Buer, N7MKO
# 
# This program is free
#
# Query GPSD and send output to grid square program
#

gpspipe -w | grep -m 1 TPV | jq -j '.lon, " ",.lat, "\n"'

# gps.sh | xargs grid to print grid
