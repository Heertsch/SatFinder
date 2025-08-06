package support.hb9hci.satfinder

class Sgp4Native {
    companion object {
        // Loads the native library
        init {
            System.loadLibrary("sgp4native")
        }

        /**
         * Executes an SGP4 calculation.
         * @param tleLine1 First line of the TLE
         * @param tleLine2 Second line of the TLE
         * @param minutesSinceEpoch Minutes since the TLE epoch
         * @return Array with [lat, lon, alt] in degrees and meters
         */
        @JvmStatic
        external fun propagate(tleLine1: String, tleLine2: String, minutesSinceEpoch: Double): DoubleArray
    }
}
