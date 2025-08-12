package support.hb9hci.satfinder

import android.annotation.SuppressLint
import android.content.Intent
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment

class SimplifiedMainActivity : AppCompatActivity(), SensorEventListener, LocationListener, OnMapReadyCallback {

    // UI Components
    private lateinit var overlayText: TextView
    private lateinit var selectButton: Button
    private lateinit var azimuthArrow: View
    private lateinit var elevationArrow: View

    // Helpers
    private lateinit var sensorHelper: SensorHelper
    private lateinit var locationHelper: LocationHelper

    // State
    private var googleMap: GoogleMap? = null
    private var currentAzimuth = 0.0
    private var currentPitch = 0.0
    private var lastGoodLocation: Location? = null
    private var tle1: String? = null
    private var tle2: String? = null
    private var satName: String? = null
    private var aosTime: Long = -1
    private var losTime: Long = -1

    // Activity Result Launcher
    private val selectSatelliteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            ActivityResultHelper.handleSatelliteSelectionResult(this, result.data) {
                tle1New, tle2New, satNameNew, _, _, _, _, _, _, aosTimeNew, losTimeNew ->
                updateSatelliteData(tle1New, tle2New, satNameNew, aosTimeNew, losTimeNew)
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeHelpers()
        setupUI()
        setupPermissions()
        loadData()
    }

    private fun initializeViews() {
        overlayText = findViewById(R.id.overlayText)
        selectButton = findViewById(R.id.selectSatelliteButton)
        azimuthArrow = findViewById(R.id.azimuthArrow)
        elevationArrow = findViewById(R.id.elevationArrow)
    }

    private fun initializeHelpers() {
        sensorHelper = SensorHelper(this, this)
        locationHelper = LocationHelper(this, this)
    }

    private fun setupUI() {
        UIHelper.setupArrowPivots(azimuthArrow, elevationArrow)

        selectButton.setOnClickListener {
            val intent = Intent(this, SatelliteSelectionActivity::class.java)
            selectSatelliteLauncher.launch(intent)
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupPermissions() {
        if (!PermissionHelper.isPermissionGranted(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            PermissionHelper.requestPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION, 1)
        }
    }

    private fun loadData() {
        lastGoodLocation = PreferencesHelper.loadLocation(this)

        // Load from intent or preferences
        IntentDataHelper.loadSatelliteDataFromIntent(intent) { tle1New, tle2New, satNameNew, aosTimeNew, losTimeNew ->
            updateSatelliteData(tle1New, tle2New, satNameNew, aosTimeNew, losTimeNew)
        }

        if (!ValidationHelper.isValidSatelliteData(tle1, tle2, satName)) {
            startSatelliteSelection()
        } else {
            updateOverlay()
        }
    }

    private fun updateSatelliteData(tle1: String?, tle2: String?, satName: String?, aosTime: Long, losTime: Long) {
        this.tle1 = tle1
        this.tle2 = tle2
        this.satName = satName
        this.aosTime = aosTime
        this.losTime = losTime
        updateOverlay()
        updateSatelliteDirection()
    }

    private fun startSatelliteSelection() {
        val intent = Intent(this, SatelliteSelectionActivity::class.java)
        selectSatelliteLauncher.launch(intent)
    }

    override fun onResume() {
        super.onResume()
        sensorHelper.registerSensorListener()
        if (PermissionHelper.isPermissionGranted(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            locationHelper.startLocationUpdates()
        }
        updateOverlay()
    }

    override fun onPause() {
        super.onPause()
        sensorHelper.unregisterSensorListener()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == android.hardware.Sensor.TYPE_ROTATION_VECTOR) {
            val (azimuth, pitch) = SensorHelper.calculateAzimuthAndPitch(event)
            currentAzimuth = azimuth
            currentPitch = pitch
            updateSatelliteDirection()
        }
    }

    override fun onLocationChanged(location: Location) {
        lastGoodLocation = location
        PreferencesHelper.saveLocation(this, location)
        updateOverlay()
        updateSatelliteDirection()
        MapHelper.updateMapLocation(googleMap, location)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        MapHelper.setupMap(map)
        MapHelper.updateMapLocation(map, lastGoodLocation)
    }

    private fun updateOverlay() {
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

    private fun updateSatelliteDirection() {
        val direction = SatelliteDirectionHelper.calculateSatelliteDirection(
            lastGoodLocation, tle1, tle2, satName
        )

        direction?.let {
            UIHelper.updateArrowRotations(
                azimuthArrow, elevationArrow,
                currentAzimuth, currentPitch,
                it.azimuth, it.elevation
            )
            MapHelper.addSatelliteMarker(googleMap, it.satPos, satName)
        }
    }

    // Minimal interface implementations
    override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
}
