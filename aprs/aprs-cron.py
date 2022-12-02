#!/usr/bin/python3

#
# Copyright (C) 2022 Steve Buer, N7MKO
# This Software is Free
#
# Send an APRS report from command line or cronjob, etc.
#

import socket, time

server = 'someserver.aprs2.net'
port = 15000

# the strings to send

login = b'user NOCALL pass 11111 vers aprs-cron 0.1\n'
report = b'NOCALL>APZ418:!4030.20N/12025.53WInocall.ampr.org my message here\n'

# connect and login

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((server, port))
s.sendall(login)
data = s.recv(1024)
print('aprs login successful:', repr(data))

# send report

s.sendall(report)
print('aprs report sent')

# wait and close

time.sleep(2)
s.close()
