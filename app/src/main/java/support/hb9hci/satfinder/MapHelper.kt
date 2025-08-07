package support.hb9hci.satfinder

import android.graphics.Color
import android.location.Location
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.CircleOptions
import kotlin.math.*

object MapHelper {
    /**
     * Centers the map on the given location and adds a marker.
     * @param googleMap The GoogleMap instance (nullable)
     * @param location The location to center and mark
     */
    fun updateMapPosition(googleMap: GoogleMap?, location: Location) {
        if (googleMap == null) return
        val pos = LatLng(location.latitude, location.longitude)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 0.5f))  // Noch kleinerer Zoom für bessere Übersicht
        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(pos).title("Present Position"))
    }

    /**
     * Berechnet den Empfangsradius eines Satelliten basierend auf Höhe und min. Elevationswinkel
     * @param satelliteAltitudeKm Satellitenhöhe in km
     * @param minElevationDegrees Minimaler Elevationswinkel in Grad (normalerweise 5-10°)
     * @return Empfangsradius in Kilometern
     */
    fun calculateCoverageRadius(satelliteAltitudeKm: Double, minElevationDegrees: Double = 5.0): Double {
        val earthRadiusKm = 6371.0 // Erdradius in km
        val elevationRad = Math.toRadians(minElevationDegrees)

        // Geometrische Berechnung des Sichtradius
        val satelliteDistanceFromCenter = earthRadiusKm + satelliteAltitudeKm
        val horizonAngle = acos(earthRadiusKm / satelliteDistanceFromCenter)
        val maxAngle = horizonAngle - elevationRad

        // Radius auf der Erdoberfläche
        val coverageRadiusKm = earthRadiusKm * sin(maxAngle)

        return max(0.0, coverageRadiusKm)
    }

    /**
     * Adds a satellite coverage circle to the map
     * @param googleMap The GoogleMap instance
     * @param satelliteLat Satellite latitude
     * @param satelliteLon Satellite longitude
     * @param satelliteAltitudeKm Satellite altitude in km
     * @param minElevationDegrees Minimum elevation angle (default 5°)
     */
    fun addSatelliteCoverage(
        googleMap: GoogleMap?,
        satelliteLat: Double,
        satelliteLon: Double,
        satelliteAltitudeKm: Double,
        minElevationDegrees: Double = 5.0
    ) {
        if (googleMap == null) return

        val satellitePos = LatLng(satelliteLat, satelliteLon)
        val radiusKm = calculateCoverageRadius(satelliteAltitudeKm, minElevationDegrees)
        val radiusMeters = radiusKm * 1000 // Convert to meters for Google Maps

        // Add coverage circle - Google Maps handles Mercator distortion automatically
        googleMap.addCircle(CircleOptions()
            .center(satellitePos)
            .radius(radiusMeters)
            .strokeColor(Color.RED)
            .strokeWidth(3f)
            .fillColor(Color.TRANSPARENT) // Keine Füllung - nur Rand sichtbar
        )

        // Add satellite marker
        googleMap.addMarker(MarkerOptions()
            .position(satellitePos)
            .title("Satellite (${satelliteAltitudeKm.toInt()}km)")
            .snippet("Coverage: ${radiusKm.toInt()}km radius"))
    }

    /**
     * Adds coverage for current satellite position from TLE data
     * @param googleMap The GoogleMap instance
     * @param satPos Satellite position (lat, lon, alt in meters)
     */
    fun addCurrentSatelliteCoverage(googleMap: GoogleMap?, satPos: Any) {
        if (googleMap == null) return

        // Extract lat, lon, alt from satPos object (assuming it has these properties)
        try {
            val latField = satPos::class.java.getDeclaredField("lat")
            val lonField = satPos::class.java.getDeclaredField("lon")
            val altField = satPos::class.java.getDeclaredField("alt")

            latField.isAccessible = true
            lonField.isAccessible = true
            altField.isAccessible = true

            val lat = latField.get(satPos) as Double
            val lon = lonField.get(satPos) as Double
            val altMeters = altField.get(satPos) as Double
            val altKm = altMeters / 1000.0

            addSatelliteCoverage(googleMap, lat, lon, altKm)

        } catch (e: Exception) {
            android.util.Log.e("MapHelper", "Error extracting satellite position: ${e.message}")
        }
    }
}
