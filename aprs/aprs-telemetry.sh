#!/bin/bash
#
# Send APRS Telemetry Report
# 
# Steve Buer, N7MKO
#
# This program is free
#

MYCALL=N0CALL
MYPASS=12345
DEST=APRS

SERVER=aserver.aprs2.net
PORT=14580

SEQUENCE_FILE=~/.aprs_sequence

# todo: getopt

APRSIS=false
METADATA=false

# Option: APRS-IS
# Option: RF

if [ $APRSIS == true ]
then

	exec 3<> /dev/tcp/$SERVER/$PORT

	read -u 3 LINE # read banner

	echo $LINE

	echo "user $MYCALL pass $MYPASS vers aprs-telemetry 0.1\n" >&3

	read -u 3 LINE # verify login

	echo $LINE

	sleep 2
fi

if [ METADATA = true ]
then

	echo "$MYCALL>$DEST::$MYCALL    :PARM.VBAT" >&3
	echo "$MYCALL>$DEST::$MYCALL    :UNIT.VDC" >&3
	echo "$MYCALL>$DEST::$MYCALL    :EQNS.0,1,0" >&3
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

echo "$MYCALL>$DEST:T#$SQFORMAT,13.6" >&3

sleep 2

exec 3>&-

let SEQUENCE=$SEQUENCE+1

if [ $SEQUENCE -gt 255 ]
then
	SEQUENCE=1
fi

echo $SEQUENCE > $SEQUENCE_FILE
