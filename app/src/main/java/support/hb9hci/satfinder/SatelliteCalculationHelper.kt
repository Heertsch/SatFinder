package support.hb9hci.satfinder

import android.location.Location
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import java.util.*

object SatelliteCalculationHelper {

    data class SatelliteCalculationResult(
        val azError: Double,
        val elError: Double,
        val satPos: Sgp4Util.SatPos,
        val elevation: Double,
        val azimuth: Double
    )

    fun calculateSatelliteDirection(
        tle1: String,
        tle2: String,
        location: Location,
        currentAzimuth: Double,
        currentPitch: Double
    ): SatelliteCalculationResult? {

        if (!OverlayViewHelper.isValidTleLine(tle1) || !OverlayViewHelper.isValidTleLine(tle2)) {
            return null
        }

        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time
        val epoch = TleHelper.extractEpochFromTle(tle1)

        if (epoch == null) {
            Log.e("Sgp4Debug", "updateSatelliteDirection: Epoch not actualized")
            return null
        }

        val satPos = SatelliteMathHelper.getSatellitePosition(tle1, tle2, now, epoch)
            ?: return null

        val azimuth = SatelliteMathHelper.calculateAzimuth(
            location.latitude, location.longitude, satPos.lat, satPos.lon
        )
        val elevation = SatelliteMathHelper.calculateElevation(
            location.latitude, location.longitude, location.altitude,
            satPos.lat, satPos.lon, satPos.alt
        )

        // Azimuth-Fehlerberechnung
        val azimuthGeo = (450 - azimuth) % 360
        val azimuthFixed = if (azimuthGeo < 0) azimuthGeo + 360 else azimuthGeo
        val azError = ((azimuthFixed - currentAzimuth + 540) % 360) - 180

        // Elevation-Fehlerberechnung
        val deviceLookElevation = SensorHelper.getDeviceLookElevation(currentAzimuth, currentPitch)
        val elError = deviceLookElevation - elevation

        Log.d("Sgp4Debug", "elevation=$elevation, deviceLookElevation=$deviceLookElevation, elError=$elError, azError=$azError, currentAzimuth=$currentAzimuth, currentPitch=$currentPitch")

        return SatelliteCalculationResult(azError, elError, satPos, elevation, azimuth)
    }

    fun updateMapWithResults(
        googleMap: GoogleMap?,
        location: Location,
        satPos: Sgp4Util.SatPos,
        satName: String?
    ) {
        // Karte leeren und neu befüllen
        googleMap?.clear()

        // Position-Marker wieder hinzufügen
        val pos = com.google.android.gms.maps.model.LatLng(location.latitude, location.longitude)
        googleMap?.addMarker(com.google.android.gms.maps.model.MarkerOptions()
            .position(pos)
            .title("Present Position"))

        // Satelliten-Marker und Empfangskreis hinzufügen
        MapHelper.addSatelliteMarker(googleMap, satPos, satName)
        MapHelper.addSatelliteReceptionCircle(googleMap, location, satPos)
    }

    /**
     * Berechnet die Zeit bis zum nächsten AOS oder LOS
     * @param tle1 TLE Zeile 1
     * @param tle2 TLE Zeile 2
     * @param location Beobachterposition
     * @param isCurrentlyVisible true wenn Satellit aktuell sichtbar ist (AOS), false wenn nicht sichtbar (LOS)
     * @return String mit "AOS in HH:MM:SS" oder "LOS in HH:MM:SS" oder "--" bei Fehlern
     */
    fun calculateNextAosLosTime(
        tle1: String,
        tle2: String,
        location: Location,
        isCurrentlyVisible: Boolean
    ): String {
        if (!OverlayViewHelper.isValidTleLine(tle1) || !OverlayViewHelper.isValidTleLine(tle2)) {
            return "--"
        }

        return try {
            val epoch = TleHelper.extractEpochFromTle(tle1) ?: return "--"
            val utcNow = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time
            var testTime: Date
            val maxSearch = 24 * 3600 // max 24h search

            if (!isCurrentlyVisible) {
                // Satellite is below horizon, find next AOS
                for (i in 1..maxSearch step 10) { // 10s steps
                    testTime = Date(utcNow.time + i * 1000L)
                    val pos = SatelliteMathHelper.getSatellitePosition(tle1, tle2, testTime, epoch)
                    val elevTest = pos?.let {
                        SatelliteMathHelper.calculateElevation(
                            location.latitude, location.longitude, location.altitude,
                            it.lat, it.lon, it.alt
                        )
                    } ?: 0.0
                    if (elevTest > 0) {
                        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(testTime)
                        return "AOS in $timeStr"
                    }
                }
            } else {
                // Satellite is above horizon, find next LOS
                for (i in 1..maxSearch step 10) { // 10s steps
                    testTime = Date(utcNow.time + i * 1000L)
                    val pos = SatelliteMathHelper.getSatellitePosition(tle1, tle2, testTime, epoch)
                    val elevTest = pos?.let {
                        SatelliteMathHelper.calculateElevation(
                            location.latitude, location.longitude, location.altitude,
                            it.lat, it.lon, it.alt
                        )
                    } ?: 0.0
                    if (elevTest <= 0) {
                        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(testTime)
                        return "LOS in $timeStr"
                    }
                }
            }
            "--" // No AOS/LOS found within 24h
        } catch (e: Exception) {
            Log.e("SatelliteCalculationHelper", "Error calculating AOS/LOS time: ${e.message}")
            "--"
        }
    }
}
