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

    /**
     * Universelle Overlay-Update-Funktion
     * @param context Android Context (optional für erweiterte Features)
     * @param overlayText TextView zum Aktualisieren
     * @param tle1 TLE Zeile 1
     * @param tle2 TLE Zeile 2
     * @param satName Satellitenname
     * @param location Beobachterposition
     * @param aosTime AOS Zeit (legacy parameter, wird nicht verwendet)
     * @param losTime LOS Zeit (legacy parameter, wird nicht verwendet)
     */
    fun updateOverlay(
        context: Context?,
        overlayText: TextView,
        tle1: String?,
        tle2: String?,
        satName: String?,
        location: Location?,
        aosTime: Long = 0,
        losTime: Long = 0
    ) {
        val satDisplay = satName ?: "(no satellite selected)"

        // Fallback für Location falls null
        val loc = location ?: context?.let { ctx ->
            val prefs = ctx.getSharedPreferences("satfinder_prefs", Context.MODE_PRIVATE)
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

        val tle1Str = tle1!!
        val tle2Str = tle2!!

        try {
            val epoch = TleHelper.extractEpochFromTle(tle1Str)
            val utcNow = Date()

            if (loc == null || epoch == null) {
                overlayText.text = Html.fromHtml("<b>Keine GPS-Position oder ungültige TLE-Epoch</b>", Html.FROM_HTML_MODE_LEGACY)
                return
            }

            val satPos = SatelliteMathHelper.getSatellitePosition(tle1Str, tle2Str, utcNow, epoch)
            if (satPos == null) {
                overlayText.text = Html.fromHtml("<b>Fehler bei der Satellitenpositionsberechnung</b>", Html.FROM_HTML_MODE_LEGACY)
                return
            }

            // Alle Berechnungen
            val azimuth = SatelliteMathHelper.calculateAzimuth(loc.latitude, loc.longitude, satPos.lat, satPos.lon)
            val elevation = SatelliteMathHelper.calculateElevation(loc.latitude, loc.longitude, loc.altitude, satPos.lat, satPos.lon, satPos.alt)
            val distance = SatelliteMathHelper.calculateDistance(loc.latitude, loc.longitude, loc.altitude, satPos.lat, satPos.lon, satPos.alt)
            val speedStr = SatelliteMathHelper.calculateAndFormatRadialSpeed(loc, tle1Str, tle2Str, satPos, epoch)

            // AOS/LOS Status mit Zeitberechnung
            val isVisible = elevation > 0.0
            val statusStr = SatelliteCalculationHelper.calculateNextAosLosTime(tle1Str, tle2Str, loc, isVisible)
            val finalStatus = if (statusStr != "--") statusStr else (if (isVisible) "AOS" else "LOS")

            // Formatierung
            val azStr = String.format(Locale.US, "%.1f°", azimuth)
            val elStr = String.format(Locale.US, "%.1f°", elevation)
            val distStr = String.format(Locale.US, "%.1f km", distance/1000.0)

            // Position formatieren
            val latAbs = abs(satPos.lat)
            val lonAbs = abs(satPos.lon)
            val latDir = if (satPos.lat >= 0) "N" else "S"
            val lonDir = if (satPos.lon >= 0) "E" else "W"
            val satPosFormatted = "Pos: %.1f° %s, %.1f° %s, Height %.0f km".format(latAbs, latDir, lonAbs, lonDir, satPos.alt/1000.0)

            val text = """
                <big><b>$satDisplay</b></big><br>
                <small>$satPosFormatted<br>
                Azimuth: $azStr, Elevation: $elStr<br>
                Distance: $distStr, Speed: $speedStr<br>
                Status: $finalStatus</small>
            """.trimIndent()

            overlayText.text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)

        } catch (e: Exception) {
            overlayText.text = Html.fromHtml("<b>Ungültige TLE-Daten: ${e.message}</b>", Html.FROM_HTML_MODE_LEGACY)
            context?.let { ctx ->
                android.widget.Toast.makeText(ctx, "Ungültige TLE-Daten: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                val prefs = ctx.getSharedPreferences("satfinder_prefs", Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
                val intent = android.content.Intent(ctx, support.hb9hci.satfinder.SatelliteSelectionActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                ctx.startActivity(intent)
            }
        }
    }

    // Legacy-Wrapper für die alte updateOverlay Funktion (umbenannt um Signatur-Konflikt zu vermeiden)
    @JvmName("updateOverlayLegacy")
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
        updateOverlay(context, overlayText, tle1, tle2, satName, lastGoodLocation, aosTime, losTime)
    }

    // Legacy-Wrapper für die alte updateSatelliteDirectionOverlay Funktion
    fun updateSatelliteDirectionOverlay(
        overlayText: TextView,
        location: Location,
        satName: String?,
        currentAzimuth: Double,
        currentPitch: Double,
        tle1: String,
        tle2: String
    ) {
        updateOverlay(null, overlayText, tle1, tle2, satName, location)
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
            <font color='$neutralColor'>AOS/LOS: $aosLos<br>Distance: -- km<br>Speed: -- km/s<br></font>
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

    /**
     * Prüft, ob ein TLE-String gültig ist (mind. 69 Zeichen, nicht nur Leerzeichen, nicht null).
     */
    fun isValidTleLine(line: String?): Boolean {
        return !line.isNullOrBlank() && line.trim().length >= 69
    }

    /**
     * Einfache, sichere Overlay-Update-Funktion
     */
    fun updateOverlaySafe(
        context: Context?,
        overlayText: TextView?,
        tle1: String?,
        tle2: String?,
        satName: String?,
        location: Location?
    ) {
        try {
            // Null-Checks für alle kritischen Parameter
            if (overlayText == null) {
                Log.w("OverlayViewHelper", "overlayText is null")
                return
            }

            if (!isValidTleLine(tle1) || !isValidTleLine(tle2)) {
                overlayText.text = "No TLE-data. Select satellite."
                return
            }

            if (location == null) {
                overlayText.text = "No GPS position available."
                return
            }

            val satDisplay = satName ?: "(no satellite selected)"
            val tle1Str = tle1!!
            val tle2Str = tle2!!

            val epoch = TleHelper.extractEpochFromTle(tle1Str)
            if (epoch == null) {
                overlayText.text = "Invalid TLE epoch data."
                return
            }

            val utcNow = Date()
            val satPos = SatelliteMathHelper.getSatellitePosition(tle1Str, tle2Str, utcNow, epoch)
            if (satPos == null) {
                overlayText.text = "Satellite position calculation failed."
                return
            }

            // Einfache Berechnungen ohne komplexe Formatierung
            val azimuth = SatelliteMathHelper.calculateAzimuth(location.latitude, location.longitude, satPos.lat, satPos.lon)
            val elevation = SatelliteMathHelper.calculateElevation(location.latitude, location.longitude, location.altitude, satPos.lat, satPos.lon, satPos.alt)
            val distance = SatelliteMathHelper.calculateDistance(location.latitude, location.longitude, location.altitude, satPos.lat, satPos.lon, satPos.alt)

            // Einfache Anzeige ohne komplexe HTML-Formatierung
            val isVisible = elevation > 0.0
            val status = if (isVisible) "AOS" else "LOS"

            val text = """
                $satDisplay
                Azimuth: ${String.format("%.1f°", azimuth)}
                Elevation: ${String.format("%.1f°", elevation)}
                Distance: ${String.format("%.1f km", distance/1000.0)}
                Status: $status
            """.trimIndent()

            overlayText.text = text

        } catch (e: Exception) {
            Log.e("OverlayViewHelper", "Error in updateOverlaySafe: ${e.message}", e)
            overlayText?.text = "Error: ${e.message}"
        }
    }
}
