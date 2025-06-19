package support.hb9hci.satfinder
import android.view.View
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlin.math.*

class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private lateinit var compassView: ImageView
    private lateinit var overlayText: TextView
    private lateinit var selectButton: Button
    private lateinit var azimuthArrow: View
    private lateinit var elevationArrow: View

    private var currentAzimuth = 0.0
    private var currentPitch = 0.0
    private var targetAzimuth = 0.0
    private var targetElevation = 0.0

    private var satLat = 45.0
    private var satLon = 8.0
    private var satAltMeters = 500000.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        compassView = findViewById(R.id.compassImage)
        overlayText = findViewById(R.id.overlayText)
        selectButton = findViewById(R.id.selectSatelliteButton)
        azimuthArrow = findViewById(R.id.azimuthArrow)
        elevationArrow = findViewById(R.id.elevationArrow)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            startLocationUpdates()
        }

        selectButton.setOnClickListener {
            val intent = Intent(this, SatelliteSelectionActivity::class.java)
            startActivityForResult(intent, 100)
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, this)
        }
    }

    override fun onResume() {
        super.onResume()
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            currentAzimuth = Math.toDegrees(orientation[0].toDouble())
            currentPitch = Math.toDegrees(orientation[1].toDouble())

            updateDisplay()
        }
    }

    override fun onLocationChanged(location: Location) {
        targetAzimuth = SatMath.calculateAzimuth(location.latitude, location.longitude, satLat, satLon)
        targetElevation = SatMath.calculateElevation(location.latitude, location.longitude, location.altitude, satLat, satLon, satAltMeters)

        val distance = SatMath.calculateDistanceKm(location.latitude, location.longitude, satLat, satLon)
        val speed = location.speed * 3.6
        val isAos = targetElevation > 0
        val isLos = !isAos

        overlayText.text = "AOS/LOS: %s / %s\nDistance: %.1f km\nSpeed: %.1f km/h".format(
            if (isAos) "AOS" else "--",
            if (isLos) "LOS" else "--",
            distance,
            speed
        )

        updateDisplay()
    }

    private fun updateDisplay() {
        compassView.rotation = -currentAzimuth.toFloat()

        val azError = (targetAzimuth - currentAzimuth + 540) % 360 - 180
        val elError = (targetElevation - currentPitch).coerceIn(-90.0, 90.0)

        updateAzimuthBar(azError)
        updateElevationBar(elError)
    }

    private fun updateAzimuthBar(error: Double) {
        val maxWidthPx = 300 * resources.displayMetrics.density
        val width = (abs(error) / 180.0 * maxWidthPx).toInt().coerceAtLeast(10)

        val layout = azimuthArrow.layoutParams
        layout.width = width
        layout.height = 4
        azimuthArrow.layoutParams = layout

        azimuthArrow.rotation = if (error >= 0) 0f else 180f
    }

    private fun updateElevationBar(error: Double) {
        val maxHeightPx = 300 * resources.displayMetrics.density
        val height = (abs(error) / 90.0 * maxHeightPx).toInt().coerceAtLeast(10)

        val layout = elevationArrow.layoutParams
        layout.width = 4
        layout.height = height
        elevationArrow.layoutParams = layout

        elevationArrow.rotation = if (error >= 0) 270f else 90f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            Toast.makeText(this, "GPS permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            data?.let {
                satLat = it.getDoubleExtra("satLat", satLat)
                satLon = it.getDoubleExtra("satLon", satLon)
                satAltMeters = it.getDoubleExtra("satAlt", satAltMeters)
                Toast.makeText(this, "Satellite selected!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
