package support.hb9hci.satfinder

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.text.Html
import android.util.Log
import android.widget.TextView
import java.util.*
import kotlin.math.abs

object OverlayViewHelper {
    fun updateOverlay(
        context: Context,
        overlayText: TextView,
        tle1: String?,
        tle2: String?,
        satName: String?,
        lastGoodLocation: Location?,
        aosTime: Long,
        losTime: Long
    ) {
        val satDisplay = satName ?: "(no satellite selected)"
        val loc = lastGoodLocation ?: run {
            val prefs = context.getSharedPreferences("satfinder_prefs", Context.MODE_PRIVATE)
            if (prefs.contains("fix_lat")) {
                val lat = prefs.getFloat("fix_lat", 0f).toDouble()
                val lon = prefs.getFloat("fix_lon", 0f).toDouble()
                val alt = prefs.getFloat("fix_alt", 0f).toDouble()
                val time = prefs.getLong("fix_time", 0L)
                val locTmp = Location(LocationManager.GPS_PROVIDER)
                locTmp.latitude = lat
                locTmp.longitude = lon
                locTmp.altitude = alt
                locTmp.time = time
                locTmp
            } else null
        }
        if (!isValidTleLine(tle1) || !isValidTleLine(tle2)) {
            overlayText.text = Html.fromHtml("<b>Keine oder ungültige TLE-Daten. Bitte wählen Sie einen Satelliten.</b>", Html.FROM_HTML_MODE_LEGACY)
            return
        }
        // ab hier sind tle1 und tle2 garantiert nicht null und gültig
        val tle1Str = tle1!!
        val tle2Str = tle2!!
        var aosLos = "--"
        var satPos: Sgp4Util.SatPos? = null
        var epoch: Date? = null
        var distance: Double?
        var speedStr = "--"
        val satPosInfo = "Sat-Pos: --"
        try {
            epoch = TleHelper.extractEpochFromTle(tle1Str)
            val utcNow = Date() // aktuelle Zeit verwenden
            if (loc != null && epoch != null) {
                satPos = SatelliteMathHelper.getSatellitePosition(tle1Str, tle2Str, utcNow, epoch)
                // Debug: Prüfe, ob satPos sich bei Satellitenwechsel ändert
                Log.d("SatFinder1", "Name: $satName satPos: $satPos, tle1: $tle1Str, tle2: $tle2Str, utcNow: $utcNow, epoch: $epoch")
                val elev = satPos?.let { SatelliteMathHelper.calculateElevation(loc.latitude, loc.longitude, loc.altitude, it.lat, it.lon, it.alt) }
                val az = satPos?.let { SatelliteMathHelper.calculateAzimuth(loc.latitude, loc.longitude, it.lat, it.lon) }
                // Debug: Prüfe, ob elev/az sich bei Satellitenwechsel ändern
                Log.d("SatFinder1", "elev: $elev, az: $az")
                distance = satPos?.let { SatelliteMathHelper.calculateDistance(loc.latitude, loc.longitude, loc.altitude, it.lat, it.lon, it.alt) }
                aosLos = if (elev != null && elev > 0.0) "AOS" else "LOS"
                // Always calculate speed
                val nowDate = utcNow
                val dtSec = 10.0
                val futureDate = Date(nowDate.time + (dtSec * 1000).toLong())
                val satPosFuture = SatelliteMathHelper.getSatellitePosition(tle1Str, tle2Str, futureDate, epoch)
                if (satPos != null && satPosFuture != null) {
                    val radialSpeed = SatelliteMathHelper.calculateRadialSpeed(loc, satPos, satPosFuture, dtSec)
                    Log.d("SatFinder3", "Speed-Berechnung: now=(${satPos.lat},${satPos.lon},${satPos.alt}), future=(${satPosFuture.lat},${satPosFuture.lon},${satPosFuture.alt}), dtSec=$dtSec, speed=$radialSpeed")
                    speedStr = String.format(Locale.US, "%+.1f m/s", radialSpeed)
                }
                // Round values and adjust units
                val azStr = az?.let { String.format(Locale.US, "%.1f°", it) } ?: "--"
                val elStr = elev?.let { String.format(Locale.US, "%.1f°", it) } ?: "--"
                val distStr = distance?.let { String.format(Locale.US, "%.1f km", it/1000.0) } ?: "--"
                val speedStrRounded = speedStr
                // Calculate next AOS time if LOS
                var nextAosStr = "--"
                if (aosLos == "LOS" && tle1 != null && tle2 != null && loc != null && epoch != null) {
                    try {
                        val utcNow = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).time
                        var testTime: Date
                        val maxSearch = 24 * 3600 // max 24h search
                        for (i in 1..maxSearch step 10) { // 10s steps
                            testTime = Date(utcNow.time + i * 1000L)
                            val pos = SatelliteMathHelper.getSatellitePosition(tle1!!, tle2!!, testTime, epoch)
                            val elevTest = pos?.let { SatelliteMathHelper.calculateElevation(loc.latitude, loc.longitude, loc.altitude, it.lat, it.lon, it.alt) } ?: 0.0
                            if (elevTest > 0) {
                                nextAosStr = java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(testTime)
                                break
                            }
                        }
                    } catch (_: Exception) {}
                }
                // AOS: Show time only when LOS
                val fixStatus = if (loc != null && loc.hasAccuracy() && loc.accuracy < 100 && System.currentTimeMillis() - loc.time < 60_000) "Fix" else "no Fix"
                val aosLosStr = if (aosLos == "LOS" && nextAosStr != "--") "LOS (next rise: $nextAosStr)" else aosLos

                // Satelliten-Position formatieren - sichere Zugriffe auf satPos
                val latAbs = abs(satPos?.lat ?: 0.0)
                val lonAbs = abs(satPos?.lon ?: 0.0)
                val latDir = if ((satPos?.lat ?: 0.0) >= 0) "N" else "S"
                val lonDir = if ((satPos?.lon ?: 0.0) >= 0) "E" else "W"
                val satPosFormatted = "Sat_pos %.1f° %s, %.1f° %s".format(latAbs, latDir, lonAbs, lonDir)
                val heightFormatted = "Height %.0f km".format((satPos?.alt ?: 0.0)/1000.0)

                val text = """
                    <big><b>$satDisplay</b></big><br>
                    <small>$satPosFormatted</small><br>
                    <small>$heightFormatted</small><br>
                    Azimuth: $azStr, Elevation: $elStr<br>
                    Distance: $distStr, Speed: $speedStrRounded<br>
                    Fix: $fixStatus<br>
                    Status: $aosLosStr
                """.trimIndent()
                overlayText.text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
            }
        } catch (e: Exception) {
            overlayText.text = Html.fromHtml("<b>Ungültige TLE-Daten: ${e.message}</b>", Html.FROM_HTML_MODE_LEGACY)
            android.widget.Toast.makeText(context, "Ungültige TLE-Daten: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            val prefs = context.getSharedPreferences("satfinder_prefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            val intent = android.content.Intent(context, support.hb9hci.satfinder.SatelliteSelectionActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            return
        }
    }

    fun updateFixStatus(
        context: Context,
        overlayText: TextView,
        lastGoodLocation: Location?,
        tle1: String?,
        tle2: String?,
        getFixColor: (String) -> Int,
        getGpsStatus: () -> String
    ) {
        // FixStatus-Logik ausgelagert aus MainActivity
        val fixStatus = when {
            lastGoodLocation == null -> "no GPS fix"
            System.currentTimeMillis() - lastGoodLocation.time > 60_000 -> "GPS fix old"
            else -> "GPS fix"
        }
        val gpsStatus = getGpsStatus()
        val neutralColor = "#FFFFFF"
        val fixColor = String.format("#%06X", 0xFFFFFF and getFixColor(fixStatus))
        // AOS/LOS calculation
        val aosLos = if (lastGoodLocation != null && tle1 != null && tle2 != null) {
            val utcNow = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).time
            val epoch = TleHelper.extractEpochFromTle(tle1!!)
            if (epoch != null) {
                val pos = SatelliteMathHelper.getSatellitePosition(tle1!!, tle2!!, utcNow, epoch)
                val elev = pos?.let { SatelliteMathHelper.calculateElevation(
                    lastGoodLocation.latitude,
                    lastGoodLocation.longitude,
                    lastGoodLocation.altitude,
                    it.lat, it.lon, it.alt
                ) } ?: 0.0
                if (elev > 0) "AOS" else "LOS"
            } else {
                "--"
            }
        } else {
            "--"
        }
        val text = """
            <font color='$neutralColor'>AOS/LOS: $aosLos<br>Distance: -- km<br>Speed: -- km/h<br></font>
            <font color='$fixColor'>$fixStatus</font> <font color='$neutralColor'>$gpsStatus</font>
        """.trimIndent()
        overlayText.text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
    }

    fun getFixColor(fixStatus: String): Int {
        return when (fixStatus) {
            "GPS fix" -> 0x00FF00 // Grün
            "GPS fix old" -> 0xFFFF00 // Gelb
            "no GPS fix", "no Fix" -> 0xFF0000 // Rot
            else -> 0xFFFFFF // Weiß
        }
    }

    fun getGpsStatus(): String {
        // Hier kann die Logik für den GPS-Status angepasst werden
        // Beispiel: Einfacher Dummy-Status
        return "Status unbekannt"
    }

    fun updateSatelliteDirectionOverlay(
        overlayText: TextView,
        location: Location,
        satPos: Sgp4Util.SatPos?,
        satName: String?,
        currentAzimuth: Double,
        currentPitch: Double,
        tle1: String,
        tle2: String
    ) {
        if (!isValidTleLine(tle1) || !isValidTleLine(tle2)) {
            overlayText.text = Html.fromHtml("<b>Keine oder ungültige TLE-Daten. Bitte wählen Sie einen Satelliten.</b>", Html.FROM_HTML_MODE_LEGACY)
            return
        }
        if (satPos == null) {
            overlayText.text = Html.fromHtml("<b>No valid satellite data. Please select a satellite.</b>", Html.FROM_HTML_MODE_LEGACY)
            return
        }
        try {
            val azimuth = SatelliteMathHelper.calculateAzimuth(location.latitude, location.longitude, satPos.lat, satPos.lon)
            val elevation = SatelliteMathHelper.calculateElevation(location.latitude, location.longitude, location.altitude, satPos.lat, satPos.lon, satPos.alt)
            val distance = SatelliteMathHelper.calculateDistance(location.latitude, location.longitude, location.altitude, satPos.lat, satPos.lon, satPos.alt)
            var speed = 0.0
            val dtSec = 10.0
            val now = Date()
            val futureDate = Date(now.time + (dtSec * 1000).toLong())
            val posFut = SatelliteMathHelper.getSatellitePosition(satName ?: "", "", futureDate, now)
            if (posFut != null) {
                val radialSpeed = SatelliteMathHelper.calculateRadialSpeed(location, satPos, posFut, dtSec)
                speed = radialSpeed / 1000.0 // km/s
            }
            val fixStatus = if (location.hasAccuracy() && location.accuracy < 100 && System.currentTimeMillis() - location.time < 60_000) "Fix" else "no Fix"
            val aosLos = if (elevation > 0) "AOS" else "LOS"
            val azStr = String.format(Locale.US, "%.1f°", azimuth)
            val elStr = String.format(Locale.US, "%.1f°", elevation)
            val distStr = String.format(Locale.US, "%.1f km", distance/1000.0) // Meter zu km konvertieren
            val speedStr = String.format(Locale.US, "%+.1f m/s", speed*1000.0)
            val latDir = if (satPos.lat >= 0) "N" else "S"
            val lonDir = if (satPos.lon >= 0) "E" else "W"
            val latAbs = abs(satPos.lat)
            val lonAbs = abs(satPos.lon)
            val satPosFormatted = "Sat_pos %.1f° %s, %.1f° %s".format(latAbs, latDir, lonAbs, lonDir)
            val heightFormatted = "Height %.0f km".format(satPos.alt/1000.0)
            val text = """
                <big><b>${satName ?: "(no name)"}</b></big><br>
                <small>$satPosFormatted</small><br>
                <small>$heightFormatted</small><br>
                Azimuth: $azStr, Elevation: $elStr<br>
                Distance: $distStr, Speed: $speedStr<br>
                Fix: $fixStatus<br>
                Status: $aosLos
            """.trimIndent()
            overlayText.text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
        } catch (e: Exception) {
            overlayText.text = Html.fromHtml("<b>Ungültige TLE-Daten: ${e.message}</b>", Html.FROM_HTML_MODE_LEGACY)
            android.widget.Toast.makeText(overlayText.context, "Ungültige TLE-Daten: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            val prefs = overlayText.context.getSharedPreferences("satfinder_prefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            val intent = android.content.Intent(overlayText.context, support.hb9hci.satfinder.SatelliteSelectionActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            overlayText.context.startActivity(intent)
            return
        }
    }

    /**
     * Prüft, ob ein TLE-String gültig ist (mind. 69 Zeichen, nicht nur Leerzeichen, nicht null).
     */
    fun isValidTleLine(line: String?): Boolean {
        return !line.isNullOrBlank() && line.trim().length >= 69
    }
}
