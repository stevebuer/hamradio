# Ham Radio Projects TODO List

## Android Auto App

### High Priority
- [ ] Add band detection for FT8 decodes
  - Implement WSJT-X UDP protocol listener to get dial frequency
  - Calculate actual RF frequency (dial + audio offset)
  - Determine band from RF frequency
  - Update FT8Decode model to include band information
  - Display band in Android Auto interface

### Medium Priority
- [ ] Add support for other digital modes (FT4, WSPR)
- [ ] Implement decode filtering (by callsign, grid, SNR threshold)
- [ ] Add statistics view (decodes per band, unique callsigns, etc.)
- [ ] Implement USB serial support for direct radio connection

### Low Priority
- [ ] Add dark mode support
- [ ] Implement decode export (CSV, ADIF)
- [ ] Add audio alerts for specific callsigns or grids
- [ ] Implement Auto-Reply functionality for FT8

## Tracker (Pine64)

### High Priority
- [ ] Implement WSJT-X UDP protocol for real-time status
  - Get dial frequency
  - Get TX/RX status
  - Get current operating mode
- [ ] Add CAT control support (Hamlib) as fallback for frequency detection

### Medium Priority
- [ ] Implement web dashboard for tracker status
- [ ] Add automatic band switching detection
- [ ] Improve GPS fallback logic and position interpolation
- [ ] Add support for multiple radio configurations

### Low Priority
- [ ] Implement APRS position beaconing with FT8 activity
- [ ] Add Prometheus metrics export
- [ ] Implement data visualization (maps, statistics)

## General / Infrastructure

### Medium Priority
- [ ] Add automated tests for Android app
- [ ] Add automated tests for tracker Python code
- [ ] Improve error handling and recovery
- [ ] Add configuration validation

### Low Priority
- [ ] Create Docker containers for easy deployment
- [ ] Add CI/CD pipeline
- [ ] Improve documentation with architecture diagrams

## Completed
- [x] GPS location support in Android Auto app
- [x] Distance calculation to decoded stations (km/miles)
- [x] GPS sharing between Android Auto and tracker
- [x] Database storage for external GPS positions
- [x] HTTP endpoint for GPS updates on tracker
