package support.hb9hci.satfinder

import android.view.View
import kotlin.math.cos
import kotlin.math.sin

object UIHelper {

    fun setupArrowPivots(azimuthArrow: View, elevationArrow: View) {
        azimuthArrow.viewTreeObserver.addOnGlobalLayoutListener {
            azimuthArrow.pivotX = azimuthArrow.width / 2f
            azimuthArrow.pivotY = azimuthArrow.height / 2f
        }
        elevationArrow.viewTreeObserver.addOnGlobalLayoutListener {
            elevationArrow.pivotX = elevationArrow.width / 2f
            elevationArrow.pivotY = elevationArrow.height / 2f
        }
    }

    fun updateArrowRotations(
        azimuthArrow: View,
        elevationArrow: View,
        currentAzimuth: Double,
        currentPitch: Double,
        targetAzimuth: Double,
        targetElevation: Double
    ) {
        // Azimuth arrow rotation (horizontal)
        val azimuthDiff = targetAzimuth - currentAzimuth
        val normalizedAzimuthDiff = when {
            azimuthDiff > 180 -> azimuthDiff - 360
            azimuthDiff < -180 -> azimuthDiff + 360
            else -> azimuthDiff
        }
        azimuthArrow.rotation = normalizedAzimuthDiff.toFloat()

        // Elevation arrow positioning and rotation
        val elevationDiff = targetElevation - currentPitch
        elevationArrow.rotation = elevationDiff.toFloat()

        // Position elevation arrow based on azimuth difference
        val screenWidth = azimuthArrow.context.resources.displayMetrics.widthPixels
        val screenHeight = azimuthArrow.context.resources.displayMetrics.heightPixels
        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f

        val radius = 100f // Radius for elevation arrow positioning
        val angleRad = Math.toRadians(normalizedAzimuthDiff)

        elevationArrow.x = centerX + radius * sin(angleRad).toFloat() - elevationArrow.width / 2f
        elevationArrow.y = centerY - radius * cos(angleRad).toFloat() - elevationArrow.height / 2f
    }
}
