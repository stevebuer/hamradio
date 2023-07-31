#!/bin/bash
#
# Send APRS Weather Report
# 
# Steve Buer, N7MKO
#
# This program is free
#

MYCALL=N0CALL
PASSCODE=12345
DEST=APRS
APRSIS=true
SERVER=rotate.aprs2.net
PORT=14580

CONFIG_FILE=~/aprs.config

while getopts 'him' OPT
do
	case $OPT in

		c)
			CONFIG_FILE=$OPTARG
			;;

		i)
			APRSIS=true
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

	echo "user $MYCALL pass $PASSCODE vers aprs-weather 0.1\n" >&3

	read -u 3 LINE # verify login

	echo $LINE

	sleep 2
fi

# todo MDHM=$(date --format todo)

MDHM=07302010

if [ $APRSIS == true ]
then
	echo "$MYCALL>$DEST:_${MDHM}c...s...g...t074" >&3
	sleep 2
	exec 3>&-
fi
