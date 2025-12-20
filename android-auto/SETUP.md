# Project Setup Summary

## What Was Created

A complete Android Auto application in Kotlin for displaying FT8 decodes on your car screen.

### Project Structure

```
android-auto/
├── build.gradle                    # Root build configuration
├── settings.gradle                 # Project settings
├── gradle.properties               # Gradle properties
├── .gitignore                      # Git ignore rules
├── README.md                       # Full documentation
├── ICONS.md                        # Icon setup guide
├── build.sh                        # Build helper script
├── test-sender.sh                  # Test data sender script
├── gradle/wrapper/
│   └── gradle-wrapper.properties   # Gradle wrapper config
└── app/
    ├── build.gradle                # App build configuration
    ├── proguard-rules.pro          # ProGuard rules
    └── src/main/
        ├── AndroidManifest.xml     # App manifest with Android Auto config
        ├── java/com/hamradio/ft8auto/
        │   ├── auto/
        │   │   ├── FT8MessagingService.kt    # Android Auto service
        │   │   └── FT8MainScreen.kt          # Android Auto UI screen
        │   ├── model/
        │   │   ├── FT8Decode.kt              # FT8 decode data class
        │   │   └── FT8DecodeManager.kt       # Manages decode list
        │   ├── parser/
        │   │   └── FT8Parser.kt              # Parses FT8 messages
        │   ├── service/
        │   │   └── FT8DataService.kt         # Background data receiver
        │   ├── MainActivity.kt               # Main configuration UI
        │   └── UsbDeviceActivity.kt          # USB device handler
        └── res/
            ├── layout/
            │   └── activity_main.xml         # Main UI layout
            ├── values/
            │   ├── colors.xml                # Color definitions
            │   ├── strings.xml               # String resources
            │   └── themes.xml                # Material 3 theme
            └── xml/
                └── usb_device_filter.xml     # USB device filter
```

## Key Features Implemented

1. **Android Auto Integration**
   - Messaging category service (required to appear in Android Auto)
   - MessageTemplate-based UI for car display
   - Foreground service for data reception

2. **Data Reception**
   - Network TCP/IP receiver with auto-reconnect
   - USB serial port support (placeholder for implementation)
   - Background service that runs independently

3. **FT8 Parsing**
   - WSJT-X format: `134500 -12  0.3 1234 ~ CQ K1ABC FN42`
   - Simple format: `K1ABC FN42 -12`
   - Raw message fallback

4. **Display Features**
   - Shows recent 100 decodes
   - Displays time, callsign, SNR, grid, and full message
   - Auto-refresh on new decodes
   - Clear all functionality

5. **Phone UI**
   - Network connection configuration
   - Real-time decode display
   - Connect/Disconnect controls
   - Material 3 design

## Next Steps

### 1. Build the App

```bash
cd /home/steve/GITHUB/hamradio/android-auto

# If you have Android SDK and Gradle:
./build.sh

# Or use Android Studio:
# Open the android-auto folder as a project
```

### 2. Test with Sample Data

```bash
# On your development machine, send test data:
./test-sender.sh <android-device-ip> 8080
```

### 3. Setup External FT8 Decoder

On your FT8 decoding computer (PC/Raspberry Pi):

```bash
# Example: Forward WSJT-X decodes to your Android device
tail -f ~/.local/share/WSJT-X/ALL.TXT | nc <android-ip> 8080

# Or create a simple server
nc -l 8080 < ft8_decodes.txt
```

### 4. Android Auto Testing

**Option A: Using a Car**
1. Install the app on your phone
2. Connect phone to car via USB or wirelessly
3. Launch Android Auto
4. Find "FT8 Auto Display" in messaging apps

**Option B: Using Desktop Head Unit (DHU)**
1. Enable Developer Mode in Android Auto
2. Install Android Auto DHU
3. Connect phone via USB with debugging
4. Run DHU to simulate car display

### 5. Icon Setup

The app currently uses default Android icons. See [ICONS.md](ICONS.md) for how to add custom icons.

## Dependencies

All dependencies are managed by Gradle:
- Kotlin 1.9.20
- Android SDK 23+ (Android 6.0+)
- AndroidX libraries
- Material Design 3
- Android Auto Car App Library 1.3.0
- Kotlin Coroutines

## Configuration

### Network Setup

1. Make sure your Android device and FT8 decoder are on the same network
2. Find your Android device's IP address
3. On the decoder, send FT8 decodes to that IP on port 8080
4. In the app, enter the decoder's IP if receiving from network

### Serial Setup

For USB serial (FTDI, etc.):
1. Update [usb_device_filter.xml](app/src/main/res/xml/usb_device_filter.xml)
2. Add your device's vendor and product IDs
3. Connect via OTG adapter

## Troubleshooting

### App doesn't appear in Android Auto
- Check that the service is declared in AndroidManifest.xml
- Verify category is set to `androidx.car.app.category.MESSAGING`
- Ensure app is installed and permissions granted

### No decodes showing
- Check network connectivity
- Verify the data service is running (check notifications)
- Test with `test-sender.sh` script
- Check format of incoming data

### Build errors
- Ensure Android SDK is installed
- Run `./gradlew clean build`
- Check that Kotlin plugin is enabled
- Sync Gradle files in Android Studio

## Android Auto Safety

This app follows Android Auto's distraction guidelines:
- Limited text per screen
- Large touch targets
- Minimal interaction required
- No video or animations
- Text displayed in car-appropriate format

**Always prioritize safe driving!**

## Development Notes

- The app uses Kotlin coroutines for asynchronous operations
- FT8DecodeManager is a thread-safe singleton
- The data service runs as a foreground service with notification
- Android Auto UI uses the Car App Library templates
- Network reconnection is automatic with 5-second delay

## License

Open source for ham radio community use.

## 73!

Enjoy monitoring FT8 from your car!
