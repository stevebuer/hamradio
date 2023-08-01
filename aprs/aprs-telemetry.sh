#!/bin/bash
#
# Send APRS Telemetry Report
# 
# Steve Buer, N7MKO
#
# This program is free
#

MYCALL=N0CALL
PASSCODE=12345
DEST=APRS
SERVER=server.aprs2.net
PORT=14580

APRSIS=false
METADATA=false

SEQUENCE_FILE=~/.aprs_sequence
CONFIG_FILE=~/aprs.config

read_apc_ups()
{
        echo 'reading APC UPS'
}

while getopts 'him' OPT
do
        case $OPT in

                i)
                        APRSIS=true
                        ;;

                m)
                        METADATA=true
                        ;;

                *)
                        echo "usage: $0 [ -i ] [ -m ]"
                        exit 1
                        ;;
        esac
done

if [ -f $CONFIG_FILE ]
then
	source $CONFIG_FILE
fi

if [ $APRSIS == true ]
then

	exec 3<> /dev/tcp/$SERVER/$PORT

	read -u 3 LINE # read banner

	echo $LINE

	echo "user $MYCALL pass $PASSCODE vers aprs-telemetry 0.1\n" >&3

	read -u 3 LINE # verify login

	echo $LINE

	sleep 2
fi

if [ $METADATA = true ]
then

	echo "$MYCALL>$DEST::$MYCALL    :PARM.BATV,LINEV,BATPCT" >&3
	echo "$MYCALL>$DEST::$MYCALL    :UNIT.VDC,VAC,PCT" >&3
#	echo "$MYCALL>$DEST::$MYCALL    :EQNS.0,1,0" >&3 ## defaults assumed
	exit 0
fi

# data

if [ -f $SEQUENCE_FILE ]
then
	SEQUENCE=$(cat $SEQUENCE_FILE)
else
	SEQUENCE=1
	echo $SEQUENCE > $SEQUENCE_FILE
fi

SQFORMAT=$(printf "%03d" $SEQUENCE)

if [ $APRSIS == true ]
then
	echo "$MYCALL>$DEST:T#$SQFORMAT,13.$(($RANDOM % 100 / 10)),$((118 + $RANDOM % 10)),$(bc <<< "100.0 - 0.$(($RANDOM % 10))")" >&3
	sleep 2
	exec 3>&-
fi

let SEQUENCE=$SEQUENCE+1

if [ $SEQUENCE -gt 255 ]
then
	SEQUENCE=1
fi

echo $SEQUENCE > $SEQUENCE_FILE
