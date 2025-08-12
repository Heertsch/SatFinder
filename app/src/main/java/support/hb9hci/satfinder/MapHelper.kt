package support.hb9hci.satfinder

import android.graphics.Color
import android.location.Location
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

object MapHelper {

    private var currentCircle: Circle? = null

    fun setupMap(googleMap: GoogleMap) {
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = false
    }

    fun updateMapLocation(googleMap: GoogleMap?, location: Location?) {
        if (googleMap != null && location != null) {
            val userLatLng = LatLng(location.latitude, location.longitude)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 10f))
        }
    }

    fun addSatelliteMarker(googleMap: GoogleMap?, satPos: Sgp4Util.SatPos?, satName: String?) {
        if (googleMap != null && satPos != null) {
            val satLatLng = LatLng(satPos.lat, satPos.lon)
            googleMap.clear()
            googleMap.addMarker(
                MarkerOptions()
                    .position(satLatLng)
                    .title(satName ?: "Satellite")
            )
        }
    }

    fun addSatelliteReceptionCircle(googleMap: GoogleMap?, userLocation: Location?, satPos: Sgp4Util.SatPos?) {
        if (googleMap != null && userLocation != null && satPos != null) {
            // Entferne vorherigen Kreis
            currentCircle?.remove()

            // Berechne Empfangsradius des Satelliten (Footprint)
            val satelliteHeightKm = satPos.alt / 1000.0
            val receptionRadiusKm = calculateSatelliteFootprintRadius(satelliteHeightKm)

            // Erstelle roten Empfangskreis um die SATELLITEN-Position
            val satLatLng = LatLng(satPos.lat, satPos.lon)
            val circleOptions = CircleOptions()
                .center(satLatLng) // Kreis um Satellit, nicht um Benutzer
                .radius(receptionRadiusKm * 1000.0) // Radius in Metern
                .strokeColor(Color.RED)
                .strokeWidth(3f)
                .fillColor(Color.TRANSPARENT) // Keine Füllung, nur Rand
                .visible(true)

            currentCircle = googleMap.addCircle(circleOptions)
        }
    }

    private fun calculateSatelliteFootprintRadius(satelliteHeightKm: Double): Double {
        // Berechnet den Footprint-Radius des Satelliten (Sichtbereich auf der Erde)
        // Formel für den Horizont-Radius: R = √(h² + 2*R_earth*h)
        val earthRadiusKm = 6371.0
        return kotlin.math.sqrt(satelliteHeightKm * satelliteHeightKm + 2 * earthRadiusKm * satelliteHeightKm)
    }
}
