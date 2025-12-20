package com.hamradio.ft8auto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.hamradio.ft8auto.model.FT8Decode
import com.hamradio.ft8auto.model.FT8DecodeManager

/**
 * Fragment displaying list of FT8 decodes
 */
class DecodeListFragment : Fragment() {
    
    private lateinit var decodeTextView: TextView
    private val decodeManager = FT8DecodeManager.getInstance()
    
    private val decodeListener = object : FT8DecodeManager.DecodeListener {
        override fun onNewDecode(decode: FT8Decode) {
            activity?.runOnUiThread {
                updateDecodeDisplay()
            }
        }
        
        override fun onDecodesCleared() {
            activity?.runOnUiThread {
                updateDecodeDisplay()
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_decode_list, container, false)
        decodeTextView = view.findViewById(R.id.decodeTextView)
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        decodeManager.addListener(decodeListener)
        updateDecodeDisplay()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        decodeManager.removeListener(decodeListener)
    }
    
    private fun updateDecodeDisplay() {
        val decodes = decodeManager.getDecodes()
        val activity = activity as? MainActivity
        
        if (decodes.isEmpty()) {
            decodeTextView.text = "No FT8 decodes yet.\n\nConnect to a data source to start receiving decodes."
        } else {
            val sb = StringBuilder()
            sb.append("Received ${decodes.size} decodes:\n\n")
            
            val lat = activity?.getCurrentLatitude()
            val lon = activity?.getCurrentLongitude()
            val useMiles = activity?.useMiles ?: true
            
            decodes.forEach { decode ->
                sb.append(decode.getDisplayText(lat, lon, useMiles))
                sb.append("\n")
            }
            decodeTextView.text = sb.toString()
        }
    }
}
