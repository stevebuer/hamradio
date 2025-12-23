package com.hamradio.ft8auto

import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.hamradio.ft8auto.model.FT8Decode
import com.hamradio.ft8auto.model.FT8DecodeManager
import com.hamradio.ft8auto.util.GridSquare
import com.hamradio.ft8auto.util.LocationTracker
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Fragment displaying a map with decoded FT8 stations plotted
 */
class MapFragment : Fragment() {
    
    private var mapView: MapView? = null
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private val decodeManager = FT8DecodeManager.getInstance()
    private val stationMarkers = mutableMapOf<String, Marker>() // callsign -> marker
    private var isMapReady = false
    
    private val decodeListener = object : FT8DecodeManager.DecodeListener {
        override fun onNewDecode(decode: FT8Decode) {
            if (isMapReady) {
                updateMap()
            }
        }
        
        override fun onDecodesCleared() {
            if (isMapReady) {
                clearMarkers()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.i("MapFragment", "onCreate called")
        Toast.makeText(requireContext(), "MapFragment.onCreate", Toast.LENGTH_SHORT).show()
        
        // Configure osmdroid BEFORE anything else
        val config = Configuration.getInstance()
        
        // Load from preferences
        config.load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        
        // Set cache directory
        val cacheDir = requireContext().cacheDir
        config.osmdroidBasePath = cacheDir
        config.osmdroidTileCache = java.io.File(cacheDir, "osmdroid").also { it.mkdirs() }
        
        // Tile loading configuration
        config.tileDownloadThreads = 8
        config.userAgentValue = "FT8AutoDisplay/1.0 (Android) osmdroid"
        
        // Network configuration for tile loading
        config.isDebugMode = true  // Enable debug to see tile loading info
        
        android.util.Log.i("MapFragment", "osmdroid configured with cache: ${config.osmdroidTileCache}")
        android.util.Log.i("MapFragment", "Debug mode: ${config.isDebugMode}")
        android.util.Log.i("MapFragment", "User agent: ${config.userAgentValue}")
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        android.util.Log.i("MapFragment", "onViewCreated called")
        Toast.makeText(requireContext(), "MapFragment.onViewCreated", Toast.LENGTH_SHORT).show()
        
        try {
            // Get the MapView from the layout
            mapView = view.findViewById(R.id.mapView)
            android.util.Log.i("MapFragment", "MapView found from layout: ${mapView != null}")
            
            mapView?.let { map ->
                Toast.makeText(requireContext(), "Initializing map...", Toast.LENGTH_SHORT).show()
                
                // Set tile source - MAPNIK is the default OpenStreetMap tiles
                val tileSource = TileSourceFactory.MAPNIK
                map.setTileSource(tileSource)
                android.util.Log.i("MapFragment", "Tile source set to: ${tileSource.name()}")
                
                map.setMultiTouchControls(true)
                map.setUseDataConnection(true) // Important: allow network tile downloads
                map.isVerticalMapRepetitionEnabled = false
                android.util.Log.i("MapFragment", "Data connection enabled for tiles")
                
                // Set default zoom and center (will update with user location)
                map.controller.setZoom(4.0)
                map.controller.setCenter(GeoPoint(39.8283, -98.5795)) // Center of USA
                
                // Add user location overlay
                myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), map)
                myLocationOverlay?.enableMyLocation()
                myLocationOverlay?.enableFollowLocation()
                map.overlays.add(myLocationOverlay)
                
                android.util.Log.i("MapFragment", "MapView initialized successfully from layout")
                Toast.makeText(requireContext(), "Map initialized!", Toast.LENGTH_SHORT).show()
                
                // Mark map as ready
                isMapReady = true
                
                // Add decode listener
                decodeManager.addListener(decodeListener)
                
                // Initial map update
                updateMap()
                
                // Force a refresh after a short delay to ensure tiles have started loading
                view.postDelayed({
                    android.util.Log.i("MapFragment", "Triggering deferred map refresh")
                    map.invalidate()
                    updateMap()
                }, 500)
                
                android.util.Log.i("MapFragment", "Map setup complete, isMapReady=$isMapReady")
            } ?: run {
                android.util.Log.e("MapFragment", "Failed to find MapView in layout with ID R.id.mapView")
                Toast.makeText(requireContext(), "ERROR: MapView not found!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("MapFragment", "Error initializing MapView: ${e.message}", e)
            e.printStackTrace()
            Toast.makeText(requireContext(), "ERROR: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        
        // Restart tile loading when coming back to this fragment
        mapView?.let { map ->
            try {
                android.util.Log.i("MapFragment", "onResume called: attempting tile refresh")
                Toast.makeText(requireContext(), "Map resumed - reloading tiles", Toast.LENGTH_SHORT).show()
                
                // Ensure data connection is enabled
                map.setUseDataConnection(true)
                android.util.Log.i("MapFragment", "Data connection enabled")
                
                // Clear the tile cache to force fresh tiles
                try {
                    android.util.Log.i("MapFragment", "Clearing tile cache")
                    map.tileProvider.clearTileCache()
                    android.util.Log.i("MapFragment", "Tile cache cleared")
                } catch (e: Exception) {
                    android.util.Log.w("MapFragment", "Could not clear tile cache: ${e.message}")
                }
                
                // Reset tile source (don't detach, just re-set)
                val tileSource = TileSourceFactory.MAPNIK
                map.setTileSource(tileSource)
                android.util.Log.i("MapFragment", "Tile source reset to: ${tileSource.name()}")
                
                // Trigger map refresh with multiple invalidations
                android.util.Log.i("MapFragment", "Starting tile refresh")
                map.invalidate()
                
                // Multiple staggered invalidations
                map.postDelayed({
                    android.util.Log.i("MapFragment", "Refresh step 1")
                    map.invalidate()
                }, 100)
                
                map.postDelayed({
                    android.util.Log.i("MapFragment", "Refresh step 2")
                    map.invalidate()
                }, 300)
                
                map.postDelayed({
                    android.util.Log.i("MapFragment", "Refresh step 3 - calling updateMap")
                    updateMap()
                    map.invalidate()
                }, 500)
                
                android.util.Log.i("MapFragment", "onResume refresh complete")
            } catch (e: Exception) {
                android.util.Log.e("MapFragment", "Error in onResume: ${e.message}", e)
                e.printStackTrace()
                Toast.makeText(requireContext(), "ERROR: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        isMapReady = false
        decodeManager.removeListener(decodeListener)
        mapView = null
        myLocationOverlay = null
    }
    
    private fun updateMap() {
        val map = mapView ?: return
        
        if (!isMapReady) return
        
        try {
            val decodes = decodeManager.getDecodes()
            val activity = activity as? MainActivity ?: return
            val myLat = activity.getCurrentLatitude()
            val myLon = activity.getCurrentLongitude()
            
            // Ensure tiles are being requested
            map.setUseDataConnection(true)
            
            // Remove markers for decodes that are no longer in the list
            val currentCallsigns = decodes.map { it.callsign }.toSet()
            val markersToRemove = stationMarkers.keys.filter { it !in currentCallsigns }
            markersToRemove.forEach { callsign ->
                stationMarkers[callsign]?.let { marker ->
                    map.overlays.remove(marker)
                }
                stationMarkers.remove(callsign)
            }
            
            // Add or update markers for current decodes
            decodes.forEach { decode ->
                if (decode.grid.isNotEmpty() && decode.callsign.isNotEmpty()) {
                    // Get station location from grid square
                    val stationLatLon = GridSquare.toLatLon(decode.grid)
                    
                    if (stationLatLon != null) {
                        val (lat, lon) = stationLatLon
                        
                        // Check if marker already exists
                        val existingMarker = stationMarkers[decode.callsign]
                        
                        if (existingMarker == null) {
                            // Create new marker
                            try {
                                val marker = Marker(map).apply {
                                    position = GeoPoint(lat, lon)
                                    title = decode.callsign
                                    
                                    // Build snippet with details
                                    val distance = if (myLat != null && myLon != null) {
                                        decode.calculateDistance(myLat, myLon)?.let { dist ->
                                            " | ${GridSquare.formatDistance(dist, useMiles = true)}"
                                        } ?: ""
                                    } else ""
                                    
                                    snippet = "${decode.grid} | SNR: ${decode.snr}dB$distance\n${decode.message}"
                                    
                                    // Color code by SNR
                                    icon = when {
                                        decode.snr >= 0 -> null // Default (green)
                                        decode.snr >= -10 -> null // Yellow would need custom drawable
                                        else -> null // Red would need custom drawable
                                    }
                                    
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                }
                                
                                map.overlays.add(marker)
                                stationMarkers[decode.callsign] = marker
                            } catch (e: Exception) {
                                android.util.Log.e("MapFragment", "Failed to create marker: ${e.message}", e)
                            }
                        } else {
                            // Update existing marker
                            try {
                                existingMarker.position = GeoPoint(lat, lon)
                                
                                val distance = if (myLat != null && myLon != null) {
                                    decode.calculateDistance(myLat, myLon)?.let { dist ->
                                        " | ${GridSquare.formatDistance(dist, useMiles = true)}"
                                    } ?: ""
                                } else ""
                                
                                existingMarker.snippet = "${decode.grid} | SNR: ${decode.snr}dB$distance\n${decode.message}"
                            } catch (e: Exception) {
                                android.util.Log.e("MapFragment", "Failed to update marker: ${e.message}", e)
                            }
                        }
                    }
                }
            }
            
            // Force map redraw with tile refresh
            map.invalidate()
        } catch (e: Exception) {
            android.util.Log.e("MapFragment", "Error updating map: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    private fun clearMarkers() {
        val map = mapView ?: return
        
        try {
            stationMarkers.values.forEach { marker ->
                map.overlays.remove(marker)
            }
            stationMarkers.clear()
            map.invalidate()
        } catch (e: Exception) {
            android.util.Log.d("MapFragment", "Error clearing markers: ${e.message}")
        }
    }
}
