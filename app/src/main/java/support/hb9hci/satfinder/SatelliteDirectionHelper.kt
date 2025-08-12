package support.hb9hci.satfinder

import android.location.Location
import android.util.Log
import java.util.*

object SatelliteDirectionHelper {

    fun calculateSatelliteDirection(
        location: Location?,
        tle1: String?,
        tle2: String?,
        satName: String?
    ): SatelliteDirection? {

        if (location == null || !OverlayViewHelper.isValidTleLine(tle1) || !OverlayViewHelper.isValidTleLine(tle2)) {
            return null
        }

        return try {
            val epoch = TleHelper.extractEpochFromTle(tle1!!)
            val utcNow = Date()

            if (epoch != null) {
                val satPos = SatelliteMathHelper.getSatellitePosition(tle1, tle2!!, utcNow, epoch)

                if (satPos != null) {
                    val azimuth = SatelliteMathHelper.calculateAzimuth(
                        location.latitude, location.longitude,
                        satPos.lat, satPos.lon
                    )
                    val elevation = SatelliteMathHelper.calculateElevation(
                        location.latitude, location.longitude, location.altitude,
                        satPos.lat, satPos.lon, satPos.alt
                    )
                    val distance = SatelliteMathHelper.calculateDistance(
                        location.latitude, location.longitude, location.altitude,
                        satPos.lat, satPos.lon, satPos.alt
                    )

                    Log.d("SatDirection", "Calculated - Azimuth: $azimuth, Elevation: $elevation")

                    SatelliteDirection(
                        azimuth = azimuth,
                        elevation = elevation,
                        distance = distance,
                        satPos = satPos,
                        isVisible = elevation > 0.0
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("SatDirection", "Error calculating satellite direction: ${e.message}")
            null
        }
    }

    data class SatelliteDirection(
        val azimuth: Double,
        val elevation: Double,
        val distance: Double,
        val satPos: Sgp4Util.SatPos,
        val isVisible: Boolean
    )
}
