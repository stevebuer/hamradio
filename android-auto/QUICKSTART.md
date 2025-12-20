# Quick Start Guide

## 1. Build the App

### Using Android Studio (Recommended)
1. Open Android Studio
2. File > Open > Select the `android-auto` folder
3. Wait for Gradle sync
4. Build > Make Project
5. Run > Run 'app' (or click green play button)

### Using Command Line
```bash
cd /home/steve/GITHUB/hamradio/android-auto
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 2. Configure Data Source

### On your FT8 Decoding Computer
```bash
# Find your Android device IP
# Example: 192.168.1.100

# Forward WSJT-X decodes (Linux/Mac)
tail -f ~/.local/share/WSJT-X/ALL.TXT | nc 192.168.1.100 8080

# Or use the test sender
./test-sender.sh 192.168.1.100 8080
```

## 3. Start the App

1. Open "FT8 Auto Display" on your Android device
2. Enter your FT8 decoder's IP and port (or keep default for reverse mode)
3. Tap "Connect"
4. You should see decodes appearing!

## 4. Use in Android Auto

1. Connect phone to car (USB or wireless)
2. Launch Android Auto on car display
3. Navigate to messaging apps
4. Open "FT8 Auto Display"
5. View FT8 decodes on your car screen!

## Common Scenarios

### Scenario 1: Testing at Home
```bash
# Terminal 1: Start test sender
cd /home/steve/GITHUB/hamradio/android-auto
./test-sender.sh <your-android-ip> 8080

# Phone: Open app, see test decodes appear
```

### Scenario 2: Using WSJT-X
```bash
# On PC running WSJT-X
tail -f ~/.local/share/WSJT-X/ALL.TXT | nc <android-ip> 8080

# Phone: Decodes appear as WSJT-X receives them
```

### Scenario 3: In the Car
1. Set up decoder at home (PC or Raspberry Pi)
2. Configure to send to your phone's IP
3. Phone connects via cellular or VPN
4. Android Auto displays decodes while driving

## Troubleshooting Quick Fixes

### "No decodes yet"
- Check network connection
- Verify IP address is correct
- Try test sender first
- Check if service is running (notification should show)

### App doesn't show in Android Auto
- Reinstall the app
- Check Android Auto app is updated
- Try disconnecting and reconnecting to car
- Enable developer mode in Android Auto settings

### Builds fail
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

### Connection issues
```bash
# Test connectivity
ping <android-ip>

# Check if port is reachable
nc -zv <android-ip> 8080
```

## Tips

- **WiFi Direct**: Use Android's WiFi hotspot for field operations
- **Battery**: Service may drain battery, keep phone charged in car
- **Permissions**: Grant all requested permissions for full functionality
- **Display**: Works best with newer cars supporting Android Auto messaging
- **Safety**: Never interact with the app while driving!

## Next Steps

- Customize the UI colors in [colors.xml](app/src/main/res/values/colors.xml)
- Add custom icon (see [ICONS.md](ICONS.md))
- Adjust decode limit in `FT8DecodeManager.kt` (default: 100)
- Modify parser for different data formats in `FT8Parser.kt`

## Support

Check out:
- [README.md](README.md) - Full documentation
- [SETUP.md](SETUP.md) - Detailed setup guide
- [CONFIG_EXAMPLES.md](CONFIG_EXAMPLES.md) - Data source examples

## 73!
