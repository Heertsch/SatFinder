package support.hb9hci.satfinder

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class SatelliteSelectionActivity : AppCompatActivity() {

    // Example satellite data
    data class Satellite(val name: String, val lat: Double, val lon: Double, val alt: Double)

    private val satellites = listOf(
        Satellite("ISS", 0.0, 0.0, 408000.0),
        Satellite("NOAA-19", 10.0, 20.0, 850000.0),
        Satellite("AO-91", -5.0, 100.0, 500000.0)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_satellite_selection)

        val listView = findViewById<ListView>(R.id.satelliteListView)
        val names = satellites.map { it.name }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val selected = satellites[position]
            val result = Intent().apply {
                putExtra("satLat", selected.lat)
                putExtra("satLon", selected.lon)
                putExtra("satAlt", selected.alt)
            }
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }
}
