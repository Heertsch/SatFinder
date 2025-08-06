package support.hb9hci.satfinder

import kotlin.math.*

object SatMathUtil {
    fun calculateSatelliteElevation(observerLat: Double, observerLon: Double, observerAlt: Double, satLat: Double, satLon: Double, satAlt: Double): Double {
        // Berechnung der Elevation zwischen Beobachter und Satellit
        val obsLatRad = Math.toRadians(observerLat)
        val obsLonRad = Math.toRadians(observerLon)
        val satLatRad = Math.toRadians(satLat)
        val satLonRad = Math.toRadians(satLon)
        val earthRadius = 6371.0 * 1000.0
        val obsR = earthRadius + observerAlt
        val satR = earthRadius + satAlt
        val obsX = obsR * cos(obsLatRad) * cos(obsLonRad)
        val obsY = obsR * cos(obsLatRad) * sin(obsLonRad)
        val obsZ = obsR * sin(obsLatRad)
        val satX = satR * cos(satLatRad) * cos(satLonRad)
        val satY = satR * cos(satLatRad) * sin(satLonRad)
        val satZ = satR * sin(satLatRad)
        val dx = satX - obsX
        val dy = satY - obsY
        val dz = satZ - obsZ
        val range = sqrt(dx * dx + dy * dy + dz * dz)
        val upX = cos(obsLatRad) * cos(obsLonRad)
        val upY = cos(obsLatRad) * sin(obsLonRad)
        val upZ = sin(obsLatRad)
        val dot = dx * upX + dy * upY + dz * upZ
        val elevation = Math.toDegrees(asin(dot / range))
        return elevation
    }

    fun calculateAzimuth(observerLat: Double, observerLon: Double, satLat: Double, satLon: Double): Double {
        // Berechnung des Azimuts zwischen Beobachter und Satellit
        val obsLatRad = Math.toRadians(observerLat)
        val obsLonRad = Math.toRadians(observerLon)
        val satLatRad = Math.toRadians(satLat)
        val satLonRad = Math.toRadians(satLon)
        val dLon = satLonRad - obsLonRad
        val y = sin(dLon) * cos(satLatRad)
        val x = cos(obsLatRad) * sin(satLatRad) - sin(obsLatRad) * cos(satLatRad) * cos(dLon)
        val azimuth = (Math.toDegrees(atan2(y, x)) + 360) % 360
        return azimuth
    }

    fun calculateDistance(observerLat: Double, observerLon: Double, observerAlt: Double, satLat: Double, satLon: Double, satAlt: Double): Double {
        // Berechnung der Distanz zwischen Beobachter und Satellit (in Metern)
        val obsLatRad = Math.toRadians(observerLat)
        val obsLonRad = Math.toRadians(observerLon)
        val satLatRad = Math.toRadians(satLat)
        val satLonRad = Math.toRadians(satLon)
        val earthRadius = 6371.0 * 1000.0
        val obsR = earthRadius + observerAlt
        val satR = earthRadius + satAlt
        val obsX = obsR * cos(obsLatRad) * cos(obsLonRad)
        val obsY = obsR * cos(obsLatRad) * sin(obsLonRad)
        val obsZ = obsR * sin(obsLatRad)
        val satX = satR * cos(satLatRad) * cos(satLonRad)
        val satY = satR * cos(satLatRad) * sin(satLonRad)
        val satZ = satR * sin(satLatRad)
        val dx = satX - obsX
        val dy = satY - obsY
        val dz = satZ - obsZ
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun calculateDistanceKm(observerLat: Double, observerLon: Double, satLat: Double, satLon: Double): Double {
        // Berechnung der Distanz auf der Erdoberfläche (in km)
        val earthRadius = 6371.0
        val dLat = Math.toRadians(satLat - observerLat)
        val dLon = Math.toRadians(satLon - observerLon)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(observerLat)) * cos(Math.toRadians(satLat)) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
