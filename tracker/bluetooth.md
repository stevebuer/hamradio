# Linux <-> Android Bluetooth Configuration

Connecting a phone (or laptop) to a single board computer over bluetooth serial to access a modem.

Linux uses the <a href="https://www.bluez.org/">BlueZ</a> protocol stack.

Interface is via <a href="https://manpages.debian.org/unstable/bluez/bluetoothctl.1.en.html">bluetoothctl</a>.

APRSdroid is the test program for this setup.

DireWolf is the intended AX.25 modem.

## Server config

The server program is <a href="https://linux.die.net/man/8/bluetoothd">bluetoothd</a>.

### Bluetooth Service Discovery Protocol (SDP)

<a href="https://linux.die.net/man/1/sdptool">sdptool</a> administers the local database and performs queries.

### rfcomm program

<a href="https://linux.die.net/man/1/rfcomm">rfcomm</a> man page.

<pre>

Steve Test:

sudo rfcomm listen hci0

</pre>

Are these even needed?

```
sdptool browse local
```

```
sdptool add --channel=22 SP
```

```
rfcomm listen /dev/rfcomm0 22
```

## Client config

Make the tracker visible and pair the Android phone as is typical.
