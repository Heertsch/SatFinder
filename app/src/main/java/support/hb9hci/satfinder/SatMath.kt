package support.hb9hci.satfinder

import kotlin.math.*

object SatMath {
    fun calculateAzimuth(userLat: Double, userLon: Double, satLat: Double, satLon: Double): Double {
        val dLon = Math.toRadians(satLon - userLon)
        val lat1 = Math.toRadians(userLat)
        val lat2 = Math.toRadians(satLat)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val azimuth = Math.toDegrees(atan2(y, x))
        return (azimuth + 360) % 360
    }
}
