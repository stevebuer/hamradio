# Map Display Fix - Gray Grid Issue

## Problem

The map in the Android app was showing a gray grid instead of displaying the actual map tiles from OpenStreetMap. This is a common issue with osmdroid when it cannot fetch or display the tile images.

## Root Causes Identified

1. **Network Security Configuration** - The `network_security_config.xml` was too restrictive. It only allowed cleartext traffic to localhost and test IPs, but didn't explicitly allow HTTPS traffic to the OpenStreetMap tile servers. This was silently blocking tile downloads.

2. **Missing osmdroid Configuration** - The osmdroid configuration wasn't explicitly setting the cache directory, which could cause tile loading issues.

3. **Tile Network Access Not Enabled** - The MapView wasn't explicitly enabling data connection for tile downloads.

4. **Insufficient Error Logging** - Errors in tile loading and map initialization weren't being properly logged, making debugging difficult.

## Solutions Applied

### 1. Updated Network Security Configuration
**File**: `app/src/main/res/xml/network_security_config.xml`

Added explicit domain configurations to allow HTTPS traffic to map tile providers:
- OpenStreetMap tile servers (a/b/c variations)
- Geoapify maps
- Mapbox maps

This ensures that the Android Security Framework allows the app to download tiles from these trusted sources.

### 2. Enhanced osmdroid Configuration
**File**: `app/src/main/java/com/hamradio/ft8auto/MapFragment.kt`

Improved the `onCreate()` method to:
- Explicitly set the osmdroid cache directory to `app/cache/osmdroid`
- Configure tile download thread pool size (8 threads)
- Add logging to verify proper initialization

### 3. Enabled Tile Network Access
**File**: `app/src/main/java/com/hamradio/ft8auto/MapFragment.kt`

Modified `onCreateView()` to:
- Call `setUseDataConnection(true)` on the MapView
- Explicitly set the MAPNIK tile source
- Add error handling and logging for initialization
- Improved error messaging

### 4. Enhanced Error Handling
**File**: `app/src/main/java/com/hamradio/ft8auto/MapFragment.kt`

Updated `updateMap()` to:
- Re-enable data connection on each map update
- Use proper error logging with full exception stack traces
- Better debugging information in logs

## Testing Recommendations

1. **Check Logcat Output**: Look for these log messages confirming proper setup:
   ```
   MapFragment: osmdroid configured with cache: [path]/osmdroid
   MapFragment: Tile source set to: MAPNIK
   MapFragment: MapView initialized successfully
   ```

2. **Verify Network Connectivity**: Ensure the device has internet access:
   - Try ping or web browser test
   - Check WiFi/cellular connection

3. **Clear Cache if Needed**: If tiles still don't load:
   ```bash
   adb shell rm -rf /data/data/com.hamradio.ft8auto/cache/osmdroid
   ```

4. **Monitor Tile Downloads**: Check logcat for tile fetch attempts:
   ```bash
   adb logcat | grep -i "osmdroid\|mapnik\|tile"
   ```

## Debugging Commands

```bash
# View recent app logs
adb logcat | grep MapFragment

# Clear all cache
adb shell rm -rf /data/data/com.hamradio.ft8auto/

# Restart the app
adb shell am force-stop com.hamradio.ft8auto
adb shell am start -n com.hamradio.ft8auto/.MainActivity

# Monitor network access
adb shell nettop -p com.hamradio.ft8auto
```

## Expected Behavior After Fix

- Map should display OpenStreetMap tiles instead of gray grid
- Zoom and pan controls should work smoothly
- Markers for FT8 stations should appear on valid grid squares
- Device location should show as a blue dot (if GPS is enabled)
- Tiles should cache locally for faster subsequent loads

## Related Documentation

- [MapFragment Implementation](app/src/main/java/com/hamradio/ft8auto/MapFragment.kt)
- [Android Network Security](https://developer.android.com/training/articles/security-config)
- [osmdroid Documentation](https://github.com/osmdroid/osmdroid/wiki)
- [OpenStreetMap Tile Usage Policy](https://operations.osmfoundation.org/policies/tiles/)

## Notes

- The map uses OpenStreetMap tiles via osmdroid, which is free and open-source
- Tiles are cached locally to reduce bandwidth usage
- The app requires internet connectivity to initially fetch tiles
- After tiles are cached, offline viewing of cached areas is possible
