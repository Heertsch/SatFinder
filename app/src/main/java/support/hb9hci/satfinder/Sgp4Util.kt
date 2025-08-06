package support.hb9hci.satfinder

import java.util.Date

object Sgp4Util {
    data class SatPos(val lat: Double, val lon: Double, val alt: Double)

    init {
        System.loadLibrary("sgp4native")
    }

    fun getSatPos(tle1: String, tle2: String, date: Date, epoch: Date): SatPos {
        val minutesSinceEpoch = (date.time - epoch.time) / 60000.0
        try {
            //android.util.Log.d("Sgp4Debug", "getSatPos: Start propagate mit tle1=$tle1, tle2=$tle2, minutesSinceEpoch=$minutesSinceEpoch")
            val result = Sgp4Native.propagate(tle1, tle2, minutesSinceEpoch)
            val satPos = SatPos(result[0], result[1], result[2])
            /*val latDir = if (satPos.lat >= 0) "N" else "S"
            val lonDir = if (satPos.lon >= 0) "E" else "W"
            val latAbs = kotlin.math.abs(satPos.lat)
            val lonAbs = kotlin.math.abs(satPos.lon)
            android.util.Log.d(
                "Sgp4Debug",
                "getSatPos: SatPosition lat=%.4f° %s, lon=%.4f° %s, alt=%.1f m".format(latAbs, latDir, lonAbs, lonDir, satPos.alt)
            )*/
            return satPos
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("Sgp4Debug", "UnsatisfiedLinkError: ${e.message}", e)
            throw e
        } catch (e: Throwable) {
            android.util.Log.e("Sgp4Debug", "Error calling Sgp4Native.propagate: ${e.message}", e)
            throw e
        }
    }
}
