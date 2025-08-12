package support.hb9hci.satfinder

import android.location.Location
import java.util.Date
import java.util.Locale
import kotlin.math.*

object SatelliteMathHelper {
    fun calculateAzimuth(observerLat: Double, observerLon: Double, satLat: Double, satLon: Double): Double {
        return SatMath.calculateAzimuth(observerLat, observerLon, satLat, satLon)
    }

    fun calculateElevation(observerLat: Double, observerLon: Double, observerAlt: Double, satLat: Double, satLon: Double, satAlt: Double): Double {
        return SatMath.calculateSatelliteElevation(observerLat, observerLon, observerAlt, satLat, satLon, satAlt)
    }

    fun calculateDistance(observerLat: Double, observerLon: Double, observerAlt: Double, satLat: Double, satLon: Double, satAlt: Double): Double {
        return SatMath.calculateDistanceKm(observerLat, observerLon, satLat, satLon) * 1000.0 // Convert to meters
    }

    fun calculateRadialSpeed(
        observer: Location,
        satPosNow: Sgp4Util.SatPos,
        satPosFuture: Sgp4Util.SatPos,
        dtSec: Double
    ): Double {
        val rad = 6371.0 * 1000.0 + observer.altitude
        val latRad = Math.toRadians(observer.latitude)
        val lonRad = Math.toRadians(observer.longitude)
        val obsX = rad * cos(latRad) * cos(lonRad)
        val obsY = rad * cos(latRad) * sin(lonRad)
        val obsZ = rad * sin(latRad)
        val satRadNow = 6371.0 * 1000.0 + satPosNow.alt
        val satLatRadNow = Math.toRadians(satPosNow.lat)
        val satLonRadNow = Math.toRadians(satPosNow.lon)
        val satXNow = satRadNow * cos(satLatRadNow) * cos(satLonRadNow)
        val satYNow = satRadNow * cos(satLatRadNow) * sin(satLonRadNow)
        val satZNow = satRadNow * sin(satLatRadNow)
        val satRadFut = 6371.0 * 1000.0 + satPosFuture.alt
        val satLatRadFut = Math.toRadians(satPosFuture.lat)
        val satLonRadFut = Math.toRadians(satPosFuture.lon)
        val satXFut = satRadFut * cos(satLatRadFut) * cos(satLonRadFut)
        val satYFut = satRadFut * cos(satLatRadFut) * sin(satLonRadFut)
        val satZFut = satRadFut * sin(satLatRadFut)
        val vxNow = satXNow - obsX
        val vyNow = satYNow - obsY
        val vzNow = satZNow - obsZ
        val distNow = sqrt(vxNow*vxNow + vyNow*vyNow + vzNow*vzNow)
        val vxFut = satXFut - obsX
        val vyFut = satYFut - obsY
        val vzFut = satZFut - obsZ
        val distFut = sqrt(vxFut*vxFut + vyFut*vyFut + vzFut*vzFut)
        return (distFut - distNow) / dtSec
    }

    /**
     * Berechnet die Radialgeschwindigkeit und gibt sie als formatierten String in km/s zurück
     */
    fun calculateAndFormatRadialSpeed(
        observer: Location,
        tle1: String,
        tle2: String,
        satPosNow: Sgp4Util.SatPos,
        epoch: Date,
        dtSec: Double = 10.0
    ): String {
        return try {
            val now = Date()
            val futureDate = Date(now.time + (dtSec * 1000).toLong())
            val satPosFuture = getSatellitePosition(tle1, tle2, futureDate, epoch)

            if (satPosFuture != null) {
                val radialSpeedMs = calculateRadialSpeed(observer, satPosNow, satPosFuture, dtSec)
                val radialSpeedKms = radialSpeedMs / 1000.0 // Convert m/s to km/s
                String.format(Locale.US, "%+.3f km/s", radialSpeedKms)
            } else {
                "--"
            }
        } catch (e: Exception) {
            "--"
        }
    }

    fun getSatellitePosition(tle1: String, tle2: String, date: Date, epoch: Date): Sgp4Util.SatPos? {
        if (!OverlayViewHelper.isValidTleLine(tle1) || !OverlayViewHelper.isValidTleLine(tle2)) {
            return null
        }
        return Sgp4Util.getSatPos(tle1, tle2, date, epoch)
    }
}
