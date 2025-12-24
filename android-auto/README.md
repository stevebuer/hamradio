# FT8 Auto Display

An Android Auto application that displays decoded FT8 messages on your car's screen. The app acts as a messaging app in Android Auto and receives FT8 decode data from an external source via network connection.

## Features

- 📱 **Android Auto Integration**: Appears as a messaging app in Android Auto
- 🌐 **Network Support**: Receives FT8 decodes over TCP/IP connection
- 📊 **Live Display**: Shows recent FT8 decodes with callsign, grid, SNR, and message
- 🚗 **Car-Optimized UI**: Designed for safe viewing while driving

## Architecture

The app consists of several components:

- **FT8DecodeManager**: Singleton that manages the list of decoded messages
- **FT8Parser**: Parses FT8 decode messages from various formats (WSJT-X, simple format)
- **FT8DataService**: Background service that receives data from network
- **FT8MessagingService**: Android Auto Car App Service that displays decodes
- **MainActivity**: Configuration UI for network settings

## Data Format

The app can parse multiple FT8 decode formats:

### WSJT-X Format
```
134500 -12  0.3 1234 ~ CQ K1ABC FN42
```
- Time: HHMMSS
- SNR: Signal-to-noise ratio in dB
- DT: Time offset
- Freq: Frequency offset in Hz
- Message: The decoded FT8 message

### Simple Format
```
K1ABC FN42 -12
```
- Callsign Grid SNR

### Raw Format
Any line that doesn't match a known format will be displayed as-is.

## Setup

### Prerequisites

- Android Studio Iguana or newer
- Android SDK 23 or higher
- Android Auto app installed on device
- A car with Android Auto support or Android Auto Developer Head Unit (DHU)

### Building

1. Clone the repository
2. Open the project in Android Studio
3. Let Gradle sync and download dependencies
4. Build and run on your Android device

```bash
./gradlew assembleDebug
```

The compiled APK will be located at: `app/build/outputs/apk/debug/app-debug.apk`

### Installation

#### Method 1: Direct Install via ADB (Recommended for Development)

1. **Enable Developer Options on your Android phone:**
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - You'll see a message that Developer Options are now enabled

2. **Enable USB Debugging:**
   - Go to Settings → System → Developer Options
   - Enable "USB Debugging"
   - (Optional) Enable "Stay awake" to keep screen on while charging

3. **Connect your phone to your computer:**
   - Use a USB cable to connect your Android phone
   - On your phone, approve the "Allow USB debugging?" prompt
   - Check "Always allow from this computer" for convenience

4. **Install ADB if not already installed:**
   ```bash
   # Debian/Ubuntu
   sudo apt install adb
   
   # Arch Linux
   sudo pacman -S android-tools
   
   # Fedora/RHEL
   sudo dnf install android-tools
   ```

5. **Verify your device is connected:**
   ```bash
   adb devices
   ```
   You should see your device listed. If you see "unauthorized", check your phone for the authorization prompt.

6. **Install the app:**
   ```bash
   ./gradlew installDebug
   ```
   Or manually:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

#### Method 2: Manual Installation

1. Copy the APK file (`app/build/outputs/apk/debug/app-debug.apk`) to your phone
2. On your phone, go to Settings → Security
3. Enable "Install unknown apps" for your file manager
4. Use a file manager to locate the APK and tap it to install

#### Post-Installation Setup

1. Open the app on your phone to configure settings
2. Enable Android Auto on your device (if not already enabled)
3. Connect your phone to your car's Android Auto system
4. The app will appear in the messaging section

## Configuration

### Network Mode

1. Open the app on your phone
2. Enter the IP address and port of your FT8 decode source
3. Tap "Connect"
4. The app will start receiving decodes and display them

Example server that sends FT8 decodes:
```bash
# Send decodes via TCP
tail -f wsjtx.log | nc -l 8080
```

### Serial Mode

1. Connect a USB serial device (FTDI, etc.) using an OTG adapter
2. The app will automatically detect the device
3. Configure the USB device filter in AndroidManifest.xml for your specific device

You may need to update [usb_device_filter.xml](app/src/main/res/xml/usb_device_filter.xml) with your device's vendor and product IDs.

## External FT8 Decoder Setup

This app does **not** decode FT8 signals. You need a separate system to:

1. Receive and decode FT8 audio (e.g., WSJT-X, FT8Call, etc.)
2. Send the decoded messages to this app via:
   - TCP/IP network connection
   - USB serial port

**Future transmission feature:** The app will support sending FT8 beacons to test propagation. Your backend system would receive transmission requests from the app and queue them for transmission via your FT8 transmitter.

### Example: Using WSJT-X

WSJT-X can log decodes to a file. You can tail this file and send it to the app:

```bash
# On your FT8 decoding computer
tail -f ~/.local/share/WSJT-X/ALL.TXT | nc <android-ip> 8080
```

### Example: Using a Raspberry Pi

Set up a Raspberry Pi with:
- RTL-SDR or other radio receiver
- FT8 decoding software
- Network connection to send decodes to your phone

## Android Auto Testing

### Using Android Auto DHU (Desktop Head Unit)

The Android Auto Desktop Head Unit (DHU) lets you test the app without a physical car. You can run it independently without Android Studio.

#### Option 1: Standalone DHU (Fastest for Development)

If you've already installed Android Studio, you can run DHU directly:

```bash
# Find and run the DHU executable
~/Android/Sdk/extras/google/auto/desktop-head-unit
```

#### Option 2: Download DHU Separately (Lightweight Alternative)

If you don't have Android Studio installed, you can download just the DHU:

1. **Download Android SDK Command-line Tools:**
   ```bash
   # Go to https://developer.android.com/studio and download "Command line tools only"
   mkdir ~/android-sdk
   cd ~/android-sdk
   unzip ~/Downloads/cmdline-tools-linux-*.zip
   ```

2. **Install the Android Auto extra:**
   ```bash
   ./cmdline-tools/bin/sdkmanager "extras;google;auto"
   ```

3. **Run DHU:**
   ```bash
   ./extras/google/auto/desktop-head-unit
   ```

#### Setup and Testing Steps

1. **Start DHU** using one of the methods above
2. **Enable USB Debugging** on your Android device:
   - Settings → System → Developer Options → USB Debugging
3. **Connect your device** via USB cable
4. **Build and install the app:**
   ```bash
   ./gradlew installDebug
   ```
5. **Launch the app** on your device from the application launcher
6. **View it in DHU** - The app will appear in the messaging section of the DHU interface

#### Troubleshooting DHU Connection

- If your device doesn't appear in DHU, try: `adb devices` to verify the connection
- On Linux, you may need to set up USB permissions: `echo 'SUBSYSTEM=="usb", ATTR{idVendor}=="<vendor_id>", MODE="0666"' | sudo tee /etc/udev/rules.d/51-android.rules`
- Restart the ADB daemon: `adb kill-server && adb start-server`

#### FT8 Auto App Not Appearing in DHU Messaging List

If the FT8 Auto app doesn't appear in the DHU messaging app list, try:

1. **Restart the DHU**: The DHU caches app discovery at startup
   ```bash
   pkill -f desktop-head-unit
   ./start-dhu.sh
   ```

2. **Make sure the app is installed and running**:
   ```bash
   adb shell pm list packages | grep ft8auto
   adb shell am start -n com.hamradio.ft8auto/.MainActivity
   ```

3. **Check app is properly declared**: The app should be in the phone's application list. If not, reinstall:
   ```bash
   ./gradlew installDebug
   ```

4. **Verify Car App compatibility**: Some older Android versions or DHU versions may have issues with Car App Service discovery. The app is properly configured but DHU app discovery can be inconsistent.

Note: The FT8 Auto Display is designed to work with both physical cars and the DHU, but Car App discovery is not always reliable on all Android versions.

## Project Structure

```
app/src/main/
├── java/com/hamradio/ft8auto/
│   ├── auto/
│   │   ├── FT8MessagingService.kt    # Android Auto service
│   │   └── FT8MainScreen.kt          # Android Auto UI
│   ├── model/
│   │   ├── FT8Decode.kt              # Data model
│   │   └── FT8DecodeManager.kt       # Decode manager
│   ├── parser/
│   │   └── FT8Parser.kt              # Message parser
│   ├── service/
│   │   └── FT8DataService.kt         # Data receiver service
│   ├── MainActivity.kt               # Main configuration UI
│   └── UsbDeviceActivity.kt          # USB device handler
└── res/
    ├── layout/
    │   └── activity_main.xml         # Main UI layout
    ├── values/
    │   ├── colors.xml
    │   ├── strings.xml
    │   └── themes.xml
    └── xml/
        └── usb_device_filter.xml     # USB device filter
```

## Dependencies

- Kotlin 1.9.20
- AndroidX Core KTX
- AndroidX Car App 1.3.0 (Android Auto support)
- Material Design 3
- Kotlin Coroutines
- usb-serial-for-android (USB serial support)

## License

This project is provided as-is for ham radio operators. Feel free to modify and distribute.

## Contributing

Contributions are welcome! Some ideas for improvements:

- [x] ~~Add Bluetooth support for data reception~~ (Network-based instead)
- [ ] **Beacon transmission** - Send FT8 beacons from the car to test propagation
- [ ] Add filtering by callsign or grid square
- [ ] Add statistics and decode history
- [ ] Support for other digital modes (FT4, PSK31, etc.)
- [ ] Map view showing station locations
- [ ] Message composition and transmission (future enhancement)

## Safety Note

This app is designed for safe use while driving:
- Large, readable text
- Minimal interaction required
- Optimized for Android Auto's distraction-free interface
- **Always prioritize safe driving over viewing FT8 decodes!**

## 73!

Enjoy monitoring FT8 activity from your car! This is particularly useful for:
- Monitoring band conditions during road trips
- Checking propagation on your commute
- Field Day mobile operations
- Emergency communications monitoring

---

**Note**: This app requires an external FT8 decoder. The actual audio processing and decoding must be done by another system (PC, Raspberry Pi, etc.).
