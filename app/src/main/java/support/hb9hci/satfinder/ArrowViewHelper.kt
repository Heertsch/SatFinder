package support.hb9hci.satfinder

import android.view.View
import kotlin.math.abs

object ArrowViewHelper {
    fun updateAzimuthBar(azimuthArrow: View, error: Double) {
        val parent = azimuthArrow.parent as View
        val maxWidthPx = parent.width * 0.4 // Maximum 40% of parent width
        val length = (abs(error) / 180.0 * maxWidthPx).toInt().coerceAtLeast((20 * azimuthArrow.resources.displayMetrics.density).toInt())

        val layout = azimuthArrow.layoutParams
        layout.width = length
        layout.height = (16 * azimuthArrow.resources.displayMetrics.density).toInt()
        azimuthArrow.layoutParams = layout

        // Pivot at the blunt end (center of screen)
        azimuthArrow.pivotX = 0f
        azimuthArrow.pivotY = azimuthArrow.height / 2f
        azimuthArrow.y = (parent.height / 2f) - (azimuthArrow.height / 2f)

        // Error logic: Arrow points right if the smartphone is too far left (error > 0), left if too far right (error < 0)
        if (error > 0) {
            // Arrow to the right
            azimuthArrow.x = (parent.width / 2f)
            azimuthArrow.rotation = 0f
        } else {
            // Arrow to the left
            azimuthArrow.x = (parent.width / 2f) - length
            azimuthArrow.rotation = 180f
        }
        azimuthArrow.bringToFront()
    }

    fun updateElevationBar(elevationArrow: View, error: Double) {
        val parent = elevationArrow.parent as View
        val minLengthPx = (20 * elevationArrow.resources.displayMetrics.density).toInt()
        val maxHeightPx = if (parent.height > 0) (parent.height.toDouble() * 0.3) else (minLengthPx * 2).toDouble()
        // Arrow length: 0 if error=0, grows with |error|, but at least minLengthPx if error!=0
        val length = if (abs(error) < 1.0) 0 else (abs(error) / 90.0 * maxHeightPx).toInt().coerceAtLeast(minLengthPx)

        val layout = elevationArrow.layoutParams
        layout.width = (16 * elevationArrow.resources.displayMetrics.density).toInt()
        layout.height = length
        elevationArrow.layoutParams = layout

        // Pivot at the blunt end (center of screen)
        elevationArrow.pivotX = elevationArrow.width / 2f
        elevationArrow.pivotY = 0f // Pivot at the blunt end
        elevationArrow.x = (parent.width / 2f) - (elevationArrow.width / 2f)

        if (length == 0) {
            elevationArrow.visibility = View.INVISIBLE
        } else {
            elevationArrow.visibility = View.VISIBLE
            // Arrow points up if the phone is held too low (error > 0), down if too high (error < 0)
            if (error > 0) {
                elevationArrow.y = (parent.height / 2f) - length
                elevationArrow.rotation = 0f
            } else {
                elevationArrow.y = (parent.height / 2f)
                elevationArrow.rotation = 180f
            }
            elevationArrow.bringToFront()
        }
    }
}
