package support.hb9hci.satfinder

class Sgp4Native {
    companion object {
        // Lädt die native Bibliothek
        init {
            System.loadLibrary("sgp4native")
        }

        /**
         * Führt eine SGP4-Berechnung aus.
         * @param tleLine1 Erste Zeile des TLE
         * @param tleLine2 Zweite Zeile des TLE
         * @param minutesSinceEpoch Minuten seit dem TLE-Epoch
         * @return Array mit [lat, lon, alt] in Grad und Meter
         */
        @JvmStatic
        external fun propagate(tleLine1: String, tleLine2: String, minutesSinceEpoch: Double): DoubleArray
    }
}

