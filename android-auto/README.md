# FT8 Auto Display

An Android Auto application that displays decoded FT8 messages on your car's screen. The app acts as a messaging app in Android Auto and receives FT8 decode data from an external source via network or serial connection.

## Features

- 📱 **Android Auto Integration**: Appears as a messaging app in Android Auto
- 🌐 **Network Support**: Receives FT8 decodes over TCP/IP connection
- 🔌 **Serial Support**: Can read from USB serial devices (FTDI, etc.)
- 📊 **Live Display**: Shows recent FT8 decodes with callsign, grid, SNR, and message
- 🚗 **Car-Optimized UI**: Designed for safe viewing while driving

## Architecture

The app consists of several components:

- **FT8DecodeManager**: Singleton that manages the list of decoded messages
- **FT8Parser**: Parses FT8 decode messages from various formats (WSJT-X, simple format)
- **FT8DataService**: Background service that receives data from network or serial port
- **FT8MessagingService**: Android Auto Car App Service that displays decodes
- **MainActivity**: Configuration UI for network/serial settings

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

1. Enable developer mode on Android Auto
2. Install Android Auto Desktop Head Unit
3. Connect your device via USB with debugging enabled
4. Run DHU to test the app without a car

```bash
# Start DHU
~/android-auto/desktop-head-unit.exe
```

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

- [ ] Add Bluetooth support for data reception
- [ ] Implement full USB serial communication
- [ ] Add filtering by callsign or grid square
- [ ] Add statistics and decode history
- [ ] Support for other digital modes (FT4, PSK31, etc.)
- [ ] Map view showing station locations

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
