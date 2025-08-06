package support.hb9hci.satfinder

import java.util.*

object TleHelper {
    /**
     * Extracts the epoch date from TLE line 1 (columns 19-32, format YYDDD.DDDDDDDD)
     */
    fun extractEpochFromTle(tle1: String): Date? {
        return try {
            val epochStr = tle1.substring(18, 32).trim()
            val yearShort = epochStr.substring(0, 2).toInt()
            val year = if (yearShort < 57) 2000 + yearShort else 1900 + yearShort
            val dayOfYear = epochStr.substring(2).toDouble()
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val millis = ((dayOfYear - 1) * 24 * 60 * 60 * 1000).toLong()
            Date(cal.timeInMillis + millis)
        } catch (e: Exception) {
            null
        }
    }
}
