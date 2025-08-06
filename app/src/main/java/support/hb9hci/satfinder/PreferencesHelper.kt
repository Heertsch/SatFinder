package support.hb9hci.satfinder

import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log

object PreferencesHelper {
    fun saveLocation(context: Context, location: Location) {
        val prefs = context.getSharedPreferences("satfinder_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("fix_lat", location.latitude.toFloat())
            putFloat("fix_lon", location.longitude.toFloat())
            putFloat("fix_alt", location.altitude.toFloat())
            putLong("fix_time", location.time)
            apply()
        }
    }

    fun loadLocation(context: Context): Location? {
        val prefs = context.getSharedPreferences("satfinder_prefs", Context.MODE_PRIVATE)
        return try {
            if (prefs.contains("fix_lat")) {
                val lat = prefs.getFloat("fix_lat", 0f).toDouble()
                val lon = prefs.getFloat("fix_lon", 0f).toDouble()
                val alt = prefs.getFloat("fix_alt", 0f).toDouble()
                val time = prefs.getLong("fix_time", 0L)
                if (lat == 0.0 && lon == 0.0) {
                    Log.w("PreferencesHelper", "Location in Preferences ist 0/0 – wird ignoriert.")
                    return null
                }
                val loc = android.location.Location(android.location.LocationManager.GPS_PROVIDER)
                loc.latitude = lat
                loc.longitude = lon
                loc.altitude = alt
                loc.time = time
                loc
            } else null
        } catch (e: Exception) {
            Log.e("PreferencesHelper", "Fehler beim Laden der Location aus Preferences", e)
            null
        }
    }

    fun saveSatelliteData(context: Context, tle1: String?, tle2: String?, satName: String?, satLat: Double, satLon: Double, satAltMeters: Double) {
        val prefs = context.getSharedPreferences("satfinder_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("last_tle1", tle1)
            putString("last_tle2", tle2)
            putString("last_satName", satName)
            putFloat("last_satLat", satLat.toFloat())
            putFloat("last_satLon", satLon.toFloat())
            putFloat("last_satAltMeters", satAltMeters.toFloat())
            apply()
        }
    }

    fun loadSatelliteData(context: Context): Triple<String?, String?, String?> {
        val prefs = context.getSharedPreferences("satfinder_prefs", Context.MODE_PRIVATE)
        return try {
            val tle1 = prefs.getString("last_tle1", null)
            val tle2 = prefs.getString("last_tle2", null)
            val satName = prefs.getString("last_satName", null)
            Triple(tle1, tle2, satName)
        } catch (e: Exception) {
            Log.e("PreferencesHelper", "Fehler beim Laden der Satellitendaten aus Preferences", e)
            Triple(null, null, null)
        }
    }
}

object ActivityResultHelper {
    fun handleSatelliteSelectionResult(
        context: Context,
        data: Intent?,
        onUpdate: (tle1: String?, tle2: String?, satName: String?, satLat: Double, satLon: Double, satAltMeters: Double, periodMin: Double, inclination: Double, heightKm: Double, aosTime: Long, losTime: Long) -> Unit
    ) {
        if (data == null) return
        val satLat = data.getDoubleExtra("satLat", 0.0)
        val satLon = data.getDoubleExtra("satLon", 0.0)
        val satAltMeters = data.getDoubleExtra("satAlt", 0.0)
        val tle1 = data.getStringExtra("tle1")
        val tle2 = data.getStringExtra("tle2")
        val satName = data.getStringExtra("satName")
        val periodMin = data.getDoubleExtra("periodMin", 92.0)
        val inclination = data.getDoubleExtra("inclination", 51.6)
        val heightKm = data.getDoubleExtra("heightKm", 420.0)
        val aosTime = data.getLongExtra("aos", -1L)
        val losTime = data.getLongExtra("los", -1L)
        onUpdate(tle1, tle2, satName, satLat, satLon, satAltMeters, periodMin, inclination, heightKm, aosTime, losTime)
        PreferencesHelper.saveSatelliteData(context, tle1, tle2, satName, satLat, satLon, satAltMeters)
    }
}
