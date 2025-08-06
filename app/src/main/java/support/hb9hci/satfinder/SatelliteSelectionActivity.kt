package support.hb9hci.satfinder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import java.net.URL
import java.io.File

class SatelliteSelectionActivity : AppCompatActivity() {

    // Example satellite data
    data class Satellite(val name: String, val lat: Double, val lon: Double, val alt: Double)

    private val satellites = listOf(
        Satellite("ISS", 0.0, 0.0, 408000.0),
        Satellite("NOAA-19", 10.0, 20.0, 850000.0),
        Satellite("AO-91", -5.0, 100.0, 500000.0)
    )

    data class TleSatellite(val name: String, val tle1: String, val tle2: String)
    private var tleSatellites: List<TleSatellite> = emptyList()

    private val TLE_FILENAME = "amateur_tle.txt"
    private fun saveTleLocally(tleList: List<TleSatellite>) {
        val file = File(filesDir, TLE_FILENAME)
        file.bufferedWriter().use { out ->
            tleList.forEach {
                out.write(it.name + "\n" + it.tle1 + "\n" + it.tle2 + "\n")
            }
        }
    }
    private fun loadTleLocally(): List<TleSatellite> {
        val file = File(filesDir, TLE_FILENAME)
        if (!file.exists()) return emptyList()
        val lines = file.readLines()
        val result = mutableListOf<TleSatellite>()
        var i = 0
        while (i < lines.size - 2) {
            val name = lines[i].trim()
            val tle1 = lines[i + 1].trim()
            val tle2 = lines[i + 2].trim()
            if (name.isNotEmpty() && tle1.startsWith("1 ") && tle2.startsWith("2 ")) {
                result.add(TleSatellite(name, tle1, tle2))
                i += 3
            } else {
                i++
            }
        }
        return result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_satellite_selection)
        val listView = findViewById<ListView>(R.id.satelliteListView)
        val refreshButton = findViewById<Button>(R.id.refreshButton)

        // Beim Start: Lade TLEs lokal, falls vorhanden, sonst leer
        tleSatellites = loadTleLocally()
        if (tleSatellites.isNotEmpty()) {
            val names = tleSatellites.map { it.name }
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
            listView.adapter = adapter
        } else {
            // Wenn keine lokalen TLEs, lade sie herunter
            downloadAndShowTLEs(listView)
        }
        refreshButton.setOnClickListener {
            downloadAndShowTLEs(listView)
        }
        listView.setOnItemClickListener { _, _, position, _ ->
            val selected = tleSatellites[position]
            val (periodMin, inclination, heightKm) = extractOrbitData(selected.tle1, selected.tle2)
            // Einfache Schätzung für AOS/LOS
            val aosMillis = System.currentTimeMillis()
            val losMillis = aosMillis + ((periodMin / 2) * 60 * 1000).toLong()
            val result = Intent().apply {
                putExtra("satName", selected.name)
                putExtra("tle1", selected.tle1)
                putExtra("tle2", selected.tle2)
                putExtra("periodMin", periodMin)
                putExtra("inclination", inclination)
                putExtra("heightKm", heightKm)
                // Sat-Position aus TLE berechnen (vereinfachte Annäherung)
                val tleParts = selected.tle2.split(Regex("\\s+")).filter { it.isNotEmpty() }
                val satLat = 0.0 // Dummy, echte Berechnung benötigt SGP4
                val satLon = 0.0 // Dummy, echte Berechnung benötigt SGP4
                val satAlt = heightKm * 1000
                putExtra("satLat", satLat)
                putExtra("satLon", satLon)
                putExtra("satAlt", satAlt)
                putExtra("aos", aosMillis)
                putExtra("los", losMillis)
            }
            setResult(Activity.RESULT_OK, result)
            // Debug: Log Intent-Daten
            android.util.Log.d("SatSel", "Intent: name=${selected.name}, aos=$aosMillis, los=$losMillis, lat=0.0, lon=0.0, alt=${heightKm * 1000}")
            finish()
        }
    }
    private fun downloadAndShowTLEs(listView: ListView) {
        object : AsyncTask<Void, Void, List<TleSatellite>>() {
            override fun doInBackground(vararg params: Void?): List<TleSatellite> {
                val url = "https://celestrak.com/NORAD/elements/amateur.txt"
                return try {
                    val lines = URL(url).readText().lines()
                    val result = mutableListOf<TleSatellite>()
                    var i = 0
                    while (i < lines.size - 2) {
                        val name = lines[i].trim()
                        val tle1 = lines[i + 1].trim()
                        val tle2 = lines[i + 2].trim()
                        if (name.isNotEmpty() && tle1.startsWith("1 ") && tle2.startsWith("2 ")) {
                            result.add(TleSatellite(name, tle1, tle2))
                            i += 3
                        } else {
                            i++
                        }
                    }
                    result
                } catch (e: Exception) {
                    emptyList()
                }
            }
            override fun onPostExecute(result: List<TleSatellite>) {
                tleSatellites = result
                saveTleLocally(result)
                val names = tleSatellites.map { it.name }
                val adapter = ArrayAdapter(this@SatelliteSelectionActivity, android.R.layout.simple_list_item_1, names)
                listView.adapter = adapter
            }
        }.execute()
    }

    private fun extractOrbitData(tle1: String, tle2: String): Triple<Double, Double, Double> {
        // TLE2: 2 NNNN incl RAAN ecc argPer meanAnom meanMotion revNum
        // Beispiel: 2 25544  51.6442  21.3932 0003887  80.2342  38.1234 15.48815328299929
        val parts = tle2.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val inclination = parts[2].toDoubleOrNull() ?: 0.0
        val meanMotion = parts[7].toDoubleOrNull() ?: 15.0 // revs per day
        val periodMin = 1440.0 / meanMotion // Minuten pro Umlauf
        // Bahnhöhe aus meanMotion (vereinfachte Formel):
        val mu = 398600.4418 // km^3/s^2
        val n = meanMotion * 2 * Math.PI / 86400.0 // rad/s
        val a = Math.cbrt(mu / (n * n)) // große Halbachse in km
        val rEarth = 6371.0
        val h = a - rEarth // Bahnhöhe in km
        return Triple(periodMin, inclination, h)
    }
}
