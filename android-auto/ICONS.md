# Android Auto FT8 Display - Icon Setup

The app needs launcher icons. You can:

1. **Use Android Studio's Asset Studio**:
   - Right-click on `res` folder
   - New > Image Asset
   - Choose Icon Type: Launcher Icons
   - Use a ham radio or FT8-related icon

2. **Placeholder Icons**:
   For now, the app will use Android's default icons. The AndroidManifest references:
   - `@mipmap/ic_launcher` (default)
   - `@mipmap/ic_launcher_round` (default)

3. **Create Custom Icons**:
   Place PNG files in these directories:
   ```
   app/src/main/res/
   ├── mipmap-mdpi/ic_launcher.png (48x48)
   ├── mipmap-hdpi/ic_launcher.png (72x72)
   ├── mipmap-xhdpi/ic_launcher.png (96x96)
   ├── mipmap-xxhdpi/ic_launcher.png (144x144)
   └── mipmap-xxxhdpi/ic_launcher.png (192x192)
   ```

## Icon Suggestions

Good icon ideas for this app:
- Radio wave symbol
- "FT8" text
- Antenna/radio tower
- Spectrum analyzer display
- Ham radio callsign format

You can generate icons from SVG using Android Studio or online tools like:
- https://romannurik.github.io/AndroidAssetStudio/
- https://appicon.co/
