package support.hb9hci.satfinder

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat

class LocationHelper(
    private val context: Context,
    private val locationListener: LocationListener
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun startLocationUpdates() {
        locationManager.removeUpdates(locationListener)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            var providerCount = 0
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, locationListener)
                Toast.makeText(context, "Listener registered for GPS", Toast.LENGTH_SHORT).show()
                providerCount++
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 1f, locationListener)
                Toast.makeText(context, "Listener registered for Network", Toast.LENGTH_SHORT).show()
                providerCount++
            }
            if (providerCount == 0) {
                Toast.makeText(context, "No active location provider!", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "startLocationUpdates: No permission", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopLocationUpdates() {
        locationManager.removeUpdates(locationListener)
    }
}

