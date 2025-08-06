package support.hb9hci.satfinder.sgp4

import kotlin.math.*

/**
 * Minimalistische TLE-Klasse für LEO-Satelliten
 */
data class Tle(val line1: String, val line2: String) {
    val inclination: Double
    val raan: Double
    val eccentricity: Double
    val argPerigee: Double
    val meanAnomaly: Double
    val meanMotion: Double
    val epoch: Double
    val satelliteNumber: Int

    init {
        // Parsing according to Celestrak format
        satelliteNumber = line1.substring(2, 7).trim().toInt()
        epoch = line1.substring(18, 32).trim().toDouble()
        inclination = line2.substring(8, 16).trim().toDouble()
        raan = line2.substring(17, 25).trim().toDouble()
        eccentricity = ("0." + line2.substring(26, 33).trim()).toDouble()
        argPerigee = line2.substring(34, 42).trim().toDouble()
        meanAnomaly = line2.substring(43, 51).trim().toDouble()
        meanMotion = line2.substring(52, 63).trim().toDouble()
    }
}

/**
 * Minimalistische SGP4-Propagator-Klasse für LEO (nur grobe Position, keine Deep-Space)
 */
object Sgp4 {
    // WGS-72 Earth radius in km
    private const val R_EARTH = 6378.135
    private const val MINUTES_PER_DAY = 1440.0
    private const val TWOPI = 2.0 * Math.PI

    /**
     * Berechnet die mittlere Umlaufzeit in Minuten aus der meanMotion
     */
    fun periodMinutes(meanMotion: Double): Double = MINUTES_PER_DAY / meanMotion

    /**
     * Berechnet die Position (nur grob, LEO, ECI) für einen Zeitpunkt (in Minuten seit TLE-Epoch)
     */
    fun propagate(tle: Tle, minutesSinceEpoch: Double): Triple<Double, Double, Double> {
        // Orbital parameters
        val n = tle.meanMotion * TWOPI / MINUTES_PER_DAY // rad/min
        val a = Math.pow(R_EARTH, 2.0 / 3.0) * Math.pow(MINUTES_PER_DAY / (2 * Math.PI * tle.meanMotion), 2.0 / 3.0) // semi-major axis in km
        val e = tle.eccentricity
        val i_rad = Math.toRadians(tle.inclination)
        val raan_rad = Math.toRadians(tle.raan)
        val argp_rad = Math.toRadians(tle.argPerigee)
        // Mean anomaly
        val M = (tle.meanAnomaly + n * minutesSinceEpoch) % 360.0
        val M_rad = Math.toRadians(M)
        // Solve Kepler's equation: M = E - e*sin(E)
        var E = M_rad
        for (j in 0..4) {
            E = M_rad + e * sin(E)
        }
        // True anomaly
        val v = atan2(sqrt(1 - e * e) * sin(E), cos(E) - e)
        // Distance to Earth's center
        val r = a * (1 - e * cos(E))
        // Perifocal coordinates
        val x_p = r * cos(v)
        val y_p = r * sin(v)
        val z_p = 0.0
        // Rotation: Perifocal -> ECI (NO GMST rotation here!)
        val x = (cos(raan_rad) * cos(argp_rad) - sin(raan_rad) * sin(argp_rad) * cos(i_rad)) * x_p +
                (-cos(raan_rad) * sin(argp_rad) - sin(raan_rad) * cos(argp_rad) * cos(i_rad)) * y_p
        val y = (sin(raan_rad) * cos(argp_rad) + cos(raan_rad) * sin(argp_rad) * cos(i_rad)) * x_p +
                (-sin(raan_rad) * sin(argp_rad) + cos(raan_rad) * cos(argp_rad) * cos(i_rad)) * y_p
        val z = (sin(argp_rad) * sin(i_rad)) * x_p + (cos(argp_rad) * sin(i_rad)) * y_p
        return Triple(x, y, z) // ECI coordinates in km (NO ECEF rotation!)
    }

    // Hilfsfunktion: Berechnet die Millisekunden seit Unix-Epoch für die TLE-Epoch
    private fun tleEpochMillis(tle: Tle): Long {
        val epochInt = tle.epoch.toInt()
        val yearShort = epochInt / 1000
        val year = if (yearShort < 57) 2000 + yearShort else 1900 + yearShort
        val dayOfYear = tle.epoch - yearShort * 1000.0
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.set(java.util.Calendar.YEAR, year)
        cal.set(java.util.Calendar.DAY_OF_YEAR, dayOfYear.toInt())
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis + ((dayOfYear - dayOfYear.toInt()) * 24.0 * 60.0 * 60.0 * 1000.0).toLong()
    }

    /**
     * Converts ECI coordinates (km) and time (millis since Unix epoch) to geographic coordinates (lat, lon, altitude in km)
     * Source: https://celestrak.org/columns/v02n03/ (simplified approximation, no precession)
     */
    fun eciToGeodetic(x: Double, y: Double, z: Double, timeMillis: Long): Triple<Double, Double, Double> {
        val rEarth = 6378.137 // WGS-84
        val f = 1.0 / 298.257223563
        val e2 = 2 * f - f * f
        val gmst = calcGmst(timeMillis)
        val lon = Math.atan2(y, x) - gmst
        val r = Math.sqrt(x * x + y * y)
        val lat0 = Math.atan2(z, r)
        var lat = lat0
        var h: Double
        for (i in 0..4) {
            val sinLat = Math.sin(lat)
            val c = 1.0 / Math.sqrt(1 - e2 * sinLat * sinLat)
            h = r / Math.cos(lat) - rEarth * c
            lat = Math.atan2(z, r * (1 - e2 * c / (c + h / rEarth)))
        }
        h = r / Math.cos(lat) - rEarth / Math.sqrt(1 - e2 * Math.sin(lat) * Math.sin(lat))
        val lonDeg = Math.toDegrees(lon)
        // Normalization to -180 to +180 degrees
        val lonNorm = ((lonDeg + 180) % 360 + 360) % 360 - 180
        return Triple(Math.toDegrees(lat), lonNorm, h)
    }

    /**
     * Berechnet den Greenwich Mean Sidereal Time (GMST) Winkel in Radiant für einen Zeitpunkt
     */
    fun calcGmst(timeMillis: Long): Double {
        val jd = timeMillis / 86400000.0 + 2440587.5
        val d = jd - 2451545.0
        val gmst = 280.46061837 + 360.98564736629 * d
        return Math.toRadians(gmst % 360)
    }
}
