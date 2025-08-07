package support.hb9hci.satfinder

import android.content.Context
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.content.pm.PackageManager
import kotlin.math.*

object SensorHelper {
    /**
     * Extracts azimuth and pitch from a rotation vector event.
     * @param event The SensorEvent (rotation vector)
     * @return Pair(azimuth, pitch) in degrees
     */
    fun getAzimuthAndPitch(event: SensorEvent): Pair<Double, Double> {
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)
        val azimuthRaw = (Math.toDegrees(orientation[0].toDouble()) + 360) % 360
        val azimuth = (azimuthRaw - 90 + 360) % 360  // Korrektur um -90° für Portrait-Modus
        val pitch = Math.toDegrees(orientation[1].toDouble())
        return Pair(azimuth, pitch)
    }

    /**
     * Calculates the device's look elevation (line of sight relative to horizon).
     * @param azimuth Current azimuth in degrees
     * @param pitch Current pitch in degrees
     * @return Elevation in degrees
     */
    fun getDeviceLookElevation(azimuth: Double, pitch: Double): Double {
        val azRad = Math.toRadians(azimuth)
        val pitchRad = Math.toRadians(-pitch)  // Vorzeichen umkehren!
        val x = cos(pitchRad) * sin(azRad)
        val y = cos(pitchRad) * cos(azRad)
        val z = sin(pitchRad)
        val norm = sqrt(x * x + y * y + z * z)
        return Math.toDegrees(asin(z / norm))
    }
}

object PermissionHelper {
    /**
     * Checks if the given permission is granted.
     * @param context The context
     * @param permission The permission string (e.g. Manifest.permission.ACCESS_FINE_LOCATION)
     * @return true if granted, false otherwise
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Handles the result of a permission request.
     * @param requestCode The request code
     * @param permissions The permissions array
     * @param grantResults The grant results array
     * @param onGranted Callback if granted
     * @param onDenied Callback if denied
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        expectedRequestCode: Int,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (requestCode == expectedRequestCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onGranted()
        } else {
            onDenied()
        }
    }
}
