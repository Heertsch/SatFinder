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
        layout.height = (26 * azimuthArrow.resources.displayMetrics.density).toInt() // +10px dicker
        azimuthArrow.layoutParams = layout

        // Pivot at the tip (where it should point to center)
        azimuthArrow.pivotX = length.toFloat() // Pivot an der Spitze
        azimuthArrow.pivotY = azimuthArrow.height / 2f
        azimuthArrow.y = (parent.height / 2f) - (azimuthArrow.height / 2f)

        // Pfeilspitze soll zum Zentrum zeigen
        if (error > 0) {
            // Handy zu weit links -> Pfeil von links zum Zentrum (Spitze nach rechts)
            azimuthArrow.x = (parent.width / 2f) - length
            azimuthArrow.rotation = 0f // Original-Richtung nach rechts
        } else {
            // Handy zu weit rechts -> Pfeil von rechts zum Zentrum (Spitze nach links)
            azimuthArrow.x = (parent.width / 2f)
            azimuthArrow.rotation = 180f // Gedreht nach links
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
        layout.width = (26 * elevationArrow.resources.displayMetrics.density).toInt() // +10px dicker
        layout.height = length
        elevationArrow.layoutParams = layout

        // Pivot at the tip (where it should point to center)
        elevationArrow.pivotX = elevationArrow.width / 2f
        elevationArrow.pivotY = length.toFloat() // Pivot an der Spitze
        elevationArrow.x = (parent.width / 2f) - (elevationArrow.width / 2f)

        if (length == 0) {
            elevationArrow.visibility = View.INVISIBLE
        } else {
            elevationArrow.visibility = View.VISIBLE
            // Pfeilspitze soll zum Zentrum zeigen - Position and Rotation anpassen
            if (error > 0) {
                // Satellit ist höher -> Pfeil von unten zum Zentrum (Spitze nach oben)
                elevationArrow.y = (parent.height / 2f) - length
                elevationArrow.rotation = 0f // Original-Richtung nach unten, aber von unten positioniert
            } else {
                // Satellit ist niedriger -> Pfeil von oben zum Zentrum (Spitze nach unten)
                elevationArrow.y = (parent.height / 2f)
                elevationArrow.rotation = 180f // Gedreht nach oben, aber von oben positioniert
            }
            elevationArrow.bringToFront()
        }
    }
}
