package support.hb9hci.satfinder

object ValidationHelper {

    fun isValidSatelliteData(tle1: String?, tle2: String?, satName: String?): Boolean {
        return OverlayViewHelper.isValidTleLine(tle1) &&
               OverlayViewHelper.isValidTleLine(tle2) &&
               !satName.isNullOrBlank()
    }

    fun validateTleData(tle1: String?, tle2: String?): Boolean {
        return OverlayViewHelper.isValidTleLine(tle1) && OverlayViewHelper.isValidTleLine(tle2)
    }
}
