package support.hb9hci.satfinder

import android.location.Location
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

object MapHelper {
    /**
     * Centers the map on the given location and adds a marker.
     * @param googleMap The GoogleMap instance (nullable)
     * @param location The location to center and mark
     */
    fun updateMapPosition(googleMap: GoogleMap?, location: Location) {
        if (googleMap == null) return
        val pos = LatLng(location.latitude, location.longitude)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(pos).title("Present Position"))
    }
}

