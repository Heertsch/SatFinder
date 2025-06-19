package support.hb9hci.satfinder

import kotlin.math.*

object SatMath {

    // Calculates azimuth in degrees from observer (lat1, lon1) to satellite (lat2, lon2)
    fun calculateAzimuth(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLon = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLon) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLon)
        var azimuth = Math.toDegrees(atan2(y, x))
        azimuth = (azimuth + 360) % 360
        return azimuth
    }

    // Calculates distance in kilometers between two points on the Earth's surface
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2.0) + cos(phi1) * cos(phi2) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    // Calculates elevation angle in degrees from observer to satellite
    fun calculateElevation(
        obsLat: Double, obsLon: Double, obsAlt: Double,
        satLat: Double, satLon: Double, satAltMeters: Double
    ): Double {
        val distanceKm = calculateDistanceKm(obsLat, obsLon, satLat, satLon)
        val satAltKm = satAltMeters / 1000.0
        val deltaAltKm = satAltKm - obsAlt / 1000.0

        val elevationRad = atan2(deltaAltKm, distanceKm)
        return Math.toDegrees(elevationRad)
    }
}
