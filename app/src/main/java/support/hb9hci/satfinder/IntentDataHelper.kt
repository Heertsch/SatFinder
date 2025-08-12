package support.hb9hci.satfinder

import android.content.Intent

object IntentDataHelper {

    fun loadSatelliteDataFromIntent(
        intent: Intent,
        callback: (String?, String?, String?, Long, Long) -> Unit
    ) {
        val tle1 = intent.getStringExtra("tle1")
        val tle2 = intent.getStringExtra("tle2")
        val satName = intent.getStringExtra("satName")
        val aosTime = intent.getLongExtra("aos", -1L)
        val losTime = intent.getLongExtra("los", -1L)

        callback(tle1, tle2, satName, aosTime, losTime)
    }
}
