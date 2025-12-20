.PHONY: help build clean install test run

help:
	@echo "FT8 Auto Display - Android Build Makefile"
	@echo ""
	@echo "Available targets:"
	@echo "  make build    - Build debug APK"
	@echo "  make clean    - Clean build artifacts"
	@echo "  make install  - Install APK to connected device"
	@echo "  make test     - Run test data sender"
	@echo "  make run      - Build, install, and launch app"
	@echo "  make logcat   - Show app logs"

build:
	@echo "Building FT8 Auto Display..."
	./gradlew assembleDebug

clean:
	@echo "Cleaning build artifacts..."
	./gradlew clean

install: build
	@echo "Installing APK to device..."
	adb install -r app/build/outputs/apk/debug/app-debug.apk

test:
	@echo "Starting test data sender..."
	@echo "Enter Android device IP:"
	@read ip; ./test-sender.sh $$ip 8080

run: install
	@echo "Launching app..."
	adb shell am start -n com.hamradio.ft8auto/.MainActivity

logcat:
	@echo "Showing app logs (Ctrl+C to stop)..."
	adb logcat | grep -E "(FT8|hamradio)"

uninstall:
	@echo "Uninstalling app..."
	adb uninstall com.hamradio.ft8auto

devices:
	@echo "Connected devices:"
	adb devices -l
