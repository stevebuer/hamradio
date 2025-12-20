package com.hamradio.ft8auto

import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    
    private val decodeListener = object : FT8DecodeManager.DecodeListener {
        override fun onNewDecode(decode: FT8Decode) {
            updateMap()
        }
        
        override fun onDecodesCleared() {
            clearMarkers()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure osmdroid
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mapView = MapView(requireContext()).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            
            // Set default zoom and center (will update with user location)
            controller.setZoom(4.0)
            controller.setCenter(GeoPoint(39.8283, -98.5795)) // Center of USA
            
            // Add user location overlay
            myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), this)
            myLocationOverlay?.enableMyLocation()
            myLocationOverlay?.enableFollowLocation()
            overlays.add(myLocationOverlay)
        }
        
        return mapView
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Add decode listener
        decodeManager.addListener(decodeListener)
        
        // Initial map update
        updateMap()
    }
    
    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        decodeManager.removeListener(decodeListener)
        mapView = null
        myLocationOverlay = null
    }
    
    private fun updateMap() {
        val map = mapView ?: return
        
        val decodes = decodeManager.getDecodes()
        val activity = activity as? MainActivity ?: return
        val myLat = activity.getCurrentLatitude()
        val myLon = activity.getCurrentLongitude()
        
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
                    } else {
                        // Update existing marker
                        existingMarker.position = GeoPoint(lat, lon)
                        
                        val distance = if (myLat != null && myLon != null) {
                            decode.calculateDistance(myLat, myLon)?.let { dist ->
                                " | ${GridSquare.formatDistance(dist, useMiles = true)}"
                            } ?: ""
                        } else ""
                        
                        existingMarker.snippet = "${decode.grid} | SNR: ${decode.snr}dB$distance\n${decode.message}"
                    }
                }
            }
        }
        
        map.invalidate()
    }
    
    private fun clearMarkers() {
        val map = mapView ?: return
        
        stationMarkers.values.forEach { marker ->
            map.overlays.remove(marker)
        }
        stationMarkers.clear()
        
        map.invalidate()
    }
}
