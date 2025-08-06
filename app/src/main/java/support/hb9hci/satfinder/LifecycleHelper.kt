package support.hb9hci.satfinder

import android.app.Activity
import android.hardware.SensorEventListener
import android.hardware.SensorManager

object LifecycleHelper {
    /**
     * Registers the sensor listener for the given sensor type.
     */
    fun registerSensorListener(sensorManager: SensorManager, listener: SensorEventListener, sensorType: Int) {
        val sensor = sensorManager.getDefaultSensor(sensorType)
        sensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /**
     * Unregisters the sensor listener.
     */
    fun unregisterSensorListener(sensorManager: SensorManager, listener: SensorEventListener) {
        sensorManager.unregisterListener(listener)
    }

    /**
     * Unlocks the screen orientation.
     */
    fun unlockScreenOrientation(activity: Activity) {
        activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}

