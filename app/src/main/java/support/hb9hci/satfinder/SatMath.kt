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

    // Corrected elevation calculation for satellites (without SGP4, but with 3D geometry)
    // Source: https://www.celestrak.com/columns/v02n02/
    fun calculateSatelliteElevation(
        obsLat: Double, obsLon: Double, obsAlt: Double,
        satLat: Double, satLon: Double, satAltMeters: Double
    ): Double {
        val earthRadius = 6378137.0 // Meter (WGS84)
        val obsLatRad = Math.toRadians(obsLat)
        val obsLonRad = Math.toRadians(obsLon)
        val satLatRad = Math.toRadians(satLat)
        val satLonRad = Math.toRadians(satLon)
        val obsR = earthRadius + obsAlt
        val satR = earthRadius + satAltMeters

        // Observer Cartesian coordinates (ECEF)
        val obsX = obsR * Math.cos(obsLatRad) * Math.cos(obsLonRad)
        val obsY = obsR * Math.cos(obsLatRad) * Math.sin(obsLonRad)
        val obsZ = obsR * Math.sin(obsLatRad)

        // Satellite Cartesian coordinates (ECEF)
        val satX = satR * Math.cos(satLatRad) * Math.cos(satLonRad)
        val satY = satR * Math.cos(satLatRad) * Math.sin(satLonRad)
        val satZ = satR * Math.sin(satLatRad)

        // Vector from observer to satellite
        val dx = satX - obsX
        val dy = satY - obsY
        val dz = satZ - obsZ

        // Observer-local: Up vector
        val upX = Math.cos(obsLatRad) * Math.cos(obsLonRad)
        val upY = Math.cos(obsLatRad) * Math.sin(obsLonRad)
        val upZ = Math.sin(obsLatRad)

        // Scalar product (direction to satellite and up)
        val range = Math.sqrt(dx * dx + dy * dy + dz * dz)
        val dot = (dx * upX + dy * upY + dz * upZ) / range
        val elevationRad = Math.asin(dot)
        return Math.toDegrees(elevationRad)
    }

    // Calculates azimuth and elevation from 3D coordinates (ECEF)
    // satX, satY, satZ: Satellite position in meters (ECEF)
    // obsLat, obsLon, obsAlt: Observer position (degrees, degrees, meters)
    fun calculateAzElFrom3D(satX: Double, satY: Double, satZ: Double, obsLat: Double, obsLon: Double, obsAlt: Double): Pair<Double, Double> {
        // WGS84 ellipsoid parameters
        val a = 6378137.0 // Equatorial radius in meters
        val f = 1.0 / 298.257223563
        val e2 = 2 * f - f * f
        val latRad = Math.toRadians(obsLat)
        val lonRad = Math.toRadians(obsLon)
        val N = a / sqrt(1 - e2 * sin(latRad) * sin(latRad))
        val obsX = (N + obsAlt) * cos(latRad) * cos(lonRad)
        val obsY = (N + obsAlt) * cos(latRad) * sin(lonRad)
        val obsZ = (N * (1 - e2) + obsAlt) * sin(latRad)
        // Vector from observer to satellite
        val dx = satX - obsX
        val dy = satY - obsY
        val dz = satZ - obsZ
        // Conversion to local horizon system (ENU)
        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val sinLon = sin(lonRad)
        val cosLon = cos(lonRad)
        val east = -sinLon * dx + cosLon * dy
        val north = -sinLat * cosLon * dx - sinLat * sinLon * dy + cosLat * dz
        val up = cosLat * cosLon * dx + cosLat * sinLon * dy + sinLat * dz
        val azimuth = (Math.toDegrees(atan2(east, north)) + 360) % 360
        val elevation = Math.toDegrees(asin(up / sqrt(dx * dx + dy * dy + dz * dz)))
        return Pair(azimuth, elevation)
    }

    // Calculates the 3D distance (in meters) between observer and satellite
    fun calculateDistance(
        obsLat: Double, obsLon: Double, obsAlt: Double,
        satLat: Double, satLon: Double, satAlt: Double
    ): Double {
        // Conversion to radians
        val obsLatRad = Math.toRadians(obsLat)
        val obsLonRad = Math.toRadians(obsLon)
        val satLatRad = Math.toRadians(satLat)
        val satLonRad = Math.toRadians(satLon)
        // Earth radius in meters
        val earthRadius = 6371000.0
        // Observer coordinates in ECEF
        val obsR = earthRadius + obsAlt
        val obsX = obsR * cos(obsLatRad) * cos(obsLonRad)
        val obsY = obsR * cos(obsLatRad) * sin(obsLonRad)
        val obsZ = obsR * sin(obsLatRad)
        // Satellite coordinates in ECEF
        val satR = earthRadius + satAlt
        val satX = satR * cos(satLatRad) * cos(satLonRad)
        val satY = satR * cos(satLatRad) * sin(satLonRad)
        val satZ = satR * sin(satLatRad)
        // 3D distance
        val dx = satX - obsX
        val dy = satY - obsY
        val dz = satZ - obsZ
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
