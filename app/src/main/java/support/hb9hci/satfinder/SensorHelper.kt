package support.hb9hci.satfinder

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorHelper(
    private val context: Context,
    private val listener: SensorEventListener
) {
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun registerSensorListener() {
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun unregisterSensorListener() {
        sensorManager.unregisterListener(listener)
    }

    companion object {
        fun calculateAzimuthAndPitch(event: SensorEvent): Pair<Double, Double> {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            var azimuth = Math.toDegrees(orientation[0].toDouble())
            if (azimuth < 0) azimuth += 360.0

            val pitch = Math.toDegrees(orientation[1].toDouble())

            return Pair(azimuth, pitch)
        }

        fun getDeviceLookElevation(currentAzimuth: Double, currentPitch: Double): Double {
            // Berechnet die Elevation der Geräte-Blickrichtung relativ zum Horizont
            // Das Vorzeichen muss umgekehrt werden: positiver Pitch = nach unten geneigt = negative Elevation
            return -currentPitch
        }
    }
}
