package support.hb9hci.satfinder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.CameraUpdateFactory

class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener, OnMapReadyCallback {

    private lateinit var sensorManager: SensorManager
    private lateinit var locationHelper: LocationHelper
    private lateinit var overlayText: TextView
    private lateinit var selectButton: Button
    private lateinit var ft4ft8Button: Button
    private lateinit var azimuthArrow: View
    private lateinit var elevationArrow: View

    private var googleMap: GoogleMap? = null

    private var currentAzimuth = 0.0
    private var currentPitch = 0.0
    private var targetAzimuth = 0.0
    private var targetElevation = 0.0

    private var satLat = 45.0
    private var satLon = 8.0
    private var satAltMeters = 500000.0

    private var lastGoodLocation: Location? = null

    private var tle1: String? = null
    private var tle2: String? = null
    private var satName: String? = null

    private var periodMin: Double = 92.0
    private var inclination: Double = 51.6
    private var heightKm: Double = 420.0

    private var aosTime: Long = -1
    private var losTime: Long = -1

    // Remove deprecated startActivityForResult usage and use Activity Result API
    private val selectSatelliteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d("SatFinder2", "selectSatelliteLauncher called: result=$result")
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            ActivityResultHelper.handleSatelliteSelectionResult(
                this,
                data
            ) { tle1New, tle2New, satNameNew, satLatNew, satLonNew, satAltMetersNew, periodMinNew, inclinationNew, heightKmNew, aosTimeNew, losTimeNew ->
                Log.d("SatFinder2", "selectSatelliteLauncher: tle1New=$tle1New, tle2New=$tle2New, satNameNew=$satNameNew")
                tle1 = tle1New
                tle2 = tle2New
                satName = satNameNew
                satLat = satLatNew
                satLon = satLonNew
                satAltMeters = satAltMetersNew
                periodMin = periodMinNew
                inclination = inclinationNew
                heightKm = heightKmNew
                aosTime = aosTimeNew
                losTime = losTimeNew
                updateOverlay()
                updateSatelliteDirection()
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Set screen orientation to portrait
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("Sgp4Debug", "Test: MainActivity started")

        overlayText = findViewById(R.id.overlayText)
        selectButton = findViewById(R.id.selectSatelliteButton)
        ft4ft8Button = findViewById(R.id.ft4ft8Button)
        azimuthArrow = findViewById(R.id.azimuthArrow)
        elevationArrow = findViewById(R.id.elevationArrow)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        locationHelper = LocationHelper(this, this)

        if (!PermissionHelper.isPermissionGranted(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            locationHelper.startLocationUpdates()
        }

        selectButton.setOnClickListener {
            val intent = Intent(this, SatelliteSelectionActivity::class.java)
            selectSatelliteLauncher.launch(intent)
        }

        ft4ft8Button.setOnClickListener {
            val intent = Intent(this, FT4FT8Activity::class.java)
            startActivity(intent)
        }

        // Set pivot points after layout to allow rotation around center
        azimuthArrow.viewTreeObserver.addOnGlobalLayoutListener {
            azimuthArrow.pivotX = azimuthArrow.width / 2f
            azimuthArrow.pivotY = azimuthArrow.height / 2f
        }
        elevationArrow.viewTreeObserver.addOnGlobalLayoutListener {
            elevationArrow.pivotX = elevationArrow.width / 2f
            elevationArrow.pivotY = elevationArrow.height / 2f
        }

        // Initialize map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Load last fix from preferences
        lastGoodLocation = PreferencesHelper.loadLocation(this)
        // Get TLE data from intent
        tle1 = intent.getStringExtra("tle1")
        tle2 = intent.getStringExtra("tle2")
        satName = intent.getStringExtra("satName")
        periodMin = intent.getDoubleExtra("periodMin", periodMin)
        inclination = intent.getDoubleExtra("inclination", inclination)
        heightKm = intent.getDoubleExtra("heightKm", heightKm)
        val aosMillis = intent.getLongExtra("aos", -1L)
        val losMillis = intent.getLongExtra("los", -1L)
        if (aosMillis > 0 && losMillis > 0) {
            aosTime = aosMillis
            losTime = losMillis
        }
        // Lade TLE-Daten aus Preferences nur, wenn sie im Intent oder Speicher nicht gültig sind
        if (!OverlayViewHelper.isValidTleLine(tle1) || !OverlayViewHelper.isValidTleLine(tle2) || satName == null) {
            val (savedTle1, savedTle2, savedSatName) = PreferencesHelper.loadSatelliteData(this)
            tle1 = savedTle1
            tle2 = savedTle2
            satName = savedSatName
        }
        // Wenn immer noch keine gültigen TLE-Daten vorhanden sind, Preferences löschen und Auswahl erzwingen
        if (!OverlayViewHelper.isValidTleLine(tle1) || !OverlayViewHelper.isValidTleLine(tle2)) {
            // Preferences löschen
            val prefs = getSharedPreferences("satfinder_prefs", MODE_PRIVATE)
            prefs.edit().clear().apply()
            overlayText.text = "Keine oder ungültige TLE-Daten. Bitte wählen Sie einen Satelliten."
            // Auswahl-Dialog starten
            val intent = Intent(this, SatelliteSelectionActivity::class.java)
            selectSatelliteLauncher.launch(intent)
        } else {
            updateOverlay()
        }
    }

    // Overlay-logic outsourced
    @SuppressLint("SetTextI18n")
    private fun updateOverlay() {
        if (!OverlayViewHelper.isValidTleLine(tle1) || !OverlayViewHelper.isValidTleLine(tle2)) {
            overlayText.text = "No TLE-data. Select satellite."
            return
        }
        OverlayViewHelper.updateOverlay(
            context = this,
            overlayText = overlayText,
            tle1 = tle1,
            tle2 = tle2,
            satName = satName,
            lastGoodLocation = lastGoodLocation,
            aosTime = aosTime,
            losTime = losTime
        )
    }

    // FixStatus-Logik ausgelagert
    private fun updateFixStatus() {
        if (!OverlayViewHelper.isValidTleLine(tle1) || !OverlayViewHelper.isValidTleLine(tle2)) return
        OverlayViewHelper.updateFixStatus(
            context = this,
            overlayText = overlayText,
            lastGoodLocation = lastGoodLocation,
            tle1 = tle1,
            tle2 = tle2,
            getFixColor = OverlayViewHelper::getFixColor,
            getGpsStatus = OverlayViewHelper::getGpsStatus
        )
    }

    override fun onResume() {
        super.onResume()
        LifecycleHelper.registerSensorListener(sensorManager, this, Sensor.TYPE_ROTATION_VECTOR)
        updateFixStatus()
        updateOverlay()

        // Position aktualisieren OHNE Zoom zu ändern
        lastGoodLocation?.let { loc ->
            val pos = com.google.android.gms.maps.model.LatLng(loc.latitude, loc.longitude)
            googleMap?.clear()
            googleMap?.addMarker(com.google.android.gms.maps.model.MarkerOptions()
                .position(pos)
                .title("Present Position"))
            googleMap?.moveCamera(CameraUpdateFactory.newLatLng(pos))
        }
    }

    override fun onPause() {
        super.onPause()
        LifecycleHelper.unregisterSensorListener(sensorManager, this)
    }

    private fun startLocationUpdates() {
        locationHelper.startLocationUpdates()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val (azimuth, pitch) = SensorHelper.getAzimuthAndPitch(event)
            currentAzimuth = azimuth
            currentPitch = pitch
            updateSatelliteDirection()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Standard-Einstellungen für Satelliten-Empfangsbereiche
        googleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
        googleMap?.uiSettings?.isMyLocationButtonEnabled = true
        googleMap?.uiSettings?.isRotateGesturesEnabled = true
        googleMap?.uiSettings?.isTiltGesturesEnabled = true

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap?.isMyLocationEnabled = true
        }

        // Vernünftiger Zoom für grosse Gebiete/Kontinente (Zoom 3-4)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(
            com.google.android.gms.maps.model.LatLng(0.0, 0.0), 3f
        ))

        // Map-Logik ausgelagert
        lastGoodLocation?.let {
            // Position zentrieren aber Zoom beibehalten
            val pos = com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude)
            googleMap?.moveCamera(CameraUpdateFactory.newLatLng(pos))
            googleMap?.addMarker(com.google.android.gms.maps.model.MarkerOptions()
                .position(pos)
                .title("Present Position"))
        }
    }

    override fun onLocationChanged(location: Location) {
        val hasFix = location.hasAccuracy() && location.accuracy < 1000
        if (hasFix) {
            location.time = System.currentTimeMillis()
            lastGoodLocation = location
            PreferencesHelper.saveLocation(this, location)
        }
        OverlayViewHelper.updateOverlay(
            context = this,
            overlayText = overlayText,
            tle1 = tle1,
            tle2 = tle2,
            satName = satName,
            lastGoodLocation = lastGoodLocation,
            aosTime = aosTime,
            losTime = losTime
        )
        updateSatelliteDirection()

        // Position aktualisieren OHNE Zoom zu ändern
        if (hasFix) {
            val pos = com.google.android.gms.maps.model.LatLng(location.latitude, location.longitude)
            googleMap?.clear()
            googleMap?.addMarker(com.google.android.gms.maps.model.MarkerOptions()
                .position(pos)
                .title("Present Position"))
            // Nur Position ändern, Zoom beibehalten
            googleMap?.moveCamera(CameraUpdateFactory.newLatLng(pos))
        }
    }

    // Example method to calculate and display the direction to the satellite
    private fun updateSatelliteDirection() {
        val tle1 = this.tle1
        val tle2 = this.tle2
        val location = lastGoodLocation
        if (!OverlayViewHelper.isValidTleLine(tle1) || !OverlayViewHelper.isValidTleLine(tle2) || location == null) {
            return
        }
        val now = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).time
        val epoch = TleHelper.extractEpochFromTle(tle1!!)
        if (epoch == null) {
            Log.e("Sgp4Debug", "updateSatelliteDirection: Epoch not actualized")
            return
        }
        val satPos = SatelliteMathHelper.getSatellitePosition(tle1!!, tle2!!, now, epoch)
        if (satPos != null) {
            // UI-Update ausgelagert
            OverlayViewHelper.updateSatelliteDirectionOverlay(
                overlayText = overlayText,
                location = location,
                satPos = satPos,
                satName = satName,
                currentAzimuth = currentAzimuth,
                currentPitch = currentPitch,
                tle1 = tle1!!,
                tle2 = tle2!!
            )
            val azimuth = SatelliteMathHelper.calculateAzimuth(location.latitude, location.longitude, satPos.lat, satPos.lon)
            val elevation = SatelliteMathHelper.calculateElevation(location.latitude, location.longitude, location.altitude, satPos.lat, satPos.lon, satPos.alt)
            val azimuthGeo = (450 - azimuth) % 360
            val azimuthFixed = if (azimuthGeo < 0) azimuthGeo + 360 else azimuthGeo
            val azError = ((azimuthFixed - currentAzimuth + 540) % 360) - 180
            val elError = elevation - getDeviceLookElevation()

            // Karte leeren (entfernt alte Kreise und Marker) und dann neu befüllen
            googleMap?.clear()

            // Position-Marker wieder hinzufügen
            val pos = com.google.android.gms.maps.model.LatLng(location.latitude, location.longitude)
            googleMap?.addMarker(com.google.android.gms.maps.model.MarkerOptions()
                .position(pos)
                .title("Present Position"))

            // Empfangskreis auf der Karte anzeigen
            MapHelper.addCurrentSatelliteCoverage(googleMap, satPos)

            Log.d("Sgp4Debug", "elevation=$elevation, deviceLookElevation=${getDeviceLookElevation()}, elError=$elError, azError=$azError, currentAzimuth=$currentAzimuth, currentPitch=$currentPitch")
            updateAzimuthBar(azError)
            updateElevationBar(elError)
        }
    }

    // Helper function: Calculate the elevation of the device's line of sight relative to the horizon
    private fun getDeviceLookElevation(): Double {
        return SensorHelper.getDeviceLookElevation(currentAzimuth, currentPitch)
    }

    private fun updateAzimuthBar(error: Double) {
        ArrowViewHelper.updateAzimuthBar(azimuthArrow, error)
    }

    private fun updateElevationBar(error: Double) {
        ArrowViewHelper.updateElevationBar(elevationArrow, error)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.handlePermissionResult(
            requestCode,
            permissions,
            grantResults,
            1,
            onGranted = {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                locationHelper.startLocationUpdates()
            },
            onDenied = {
                Toast.makeText(this, "GPS permission is required", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroy() {
        locationHelper.stopLocationUpdates()
        LifecycleHelper.unlockScreenOrientation(this)
        super.onDestroy()
    }
}
