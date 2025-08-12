package support.hb9hci.satfinder

import android.view.View
import kotlin.math.abs
import kotlin.math.min

object ArrowViewHelper {

    private const val MAX_ERROR_DEGREES = 90.0 // Maximaler Fehler für Skalierung

    fun updateAzimuthBar(azimuthArrow: View, error: Double) {
        // Horizontaler Azimuth-Pfeil (blau)
        val errorAbs = abs(error)

        // Länge basierend auf Abweichung berechnen (Skalierungsfaktor zwischen 0.1 und 3.0)
        val lengthFactor = min(errorAbs / MAX_ERROR_DEGREES, 1.0)
        val scaleFactor = 0.1f + lengthFactor.toFloat() * 2.9f // 0.1 bis 3.0

        // Horizontale Skalierung für Azimuth-Abweichung (X-Achse)
        azimuthArrow.scaleX = scaleFactor
        azimuthArrow.scaleY = 1.0f // Höhe bleibt konstant

        // Position und Rotation so dass PFEILENDE im Zentrum ist
        val screenWidth = azimuthArrow.context.resources.displayMetrics.widthPixels
        val screenHeight = azimuthArrow.context.resources.displayMetrics.heightPixels
        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f

        // Setze Pivot-Punkt für Skalierung
        if (error > 0) {
            // Handy muss nach links - Pfeil zeigt nach rechts (gespiegelt)
            azimuthArrow.pivotX = azimuthArrow.width.toFloat() // Pivot am rechten Ende
            azimuthArrow.pivotY = azimuthArrow.height / 2f
            azimuthArrow.x = centerX - azimuthArrow.width // Rechtes Ende (Pivot) im Zentrum
            azimuthArrow.y = centerY - azimuthArrow.height / 2f
            azimuthArrow.rotation = 180f // Gedreht, Spitze zeigt nach rechts (gespiegelt)
        } else {
            // Handy muss nach rechts - Pfeil zeigt nach links (gespiegelt)
            azimuthArrow.pivotX = azimuthArrow.width.toFloat() // Pivot am rechten Ende
            azimuthArrow.pivotY = azimuthArrow.height / 2f
            azimuthArrow.x = centerX - azimuthArrow.width // Rechtes Ende (Pivot) im Zentrum
            azimuthArrow.y = centerY - azimuthArrow.height / 2f
            azimuthArrow.rotation = 0f // Normal, Spitze zeigt nach links (gespiegelt)
        }

        // Farb-Feedback durch Transparenz
        azimuthArrow.alpha = when {
            errorAbs < 5.0 -> 1.0f   // Sehr genau - vollständig sichtbar
            errorAbs < 15.0 -> 0.8f  // Mäßig genau - leicht transparent
            else -> 0.6f             // Ungenau - deutlich transparent
        }
    }

    fun updateElevationBar(elevationArrow: View, error: Double) {
        // Vertikaler Elevation-Pfeil (grün)
        val errorAbs = abs(error)

        // Länge basierend auf Abweichung berechnen (Skalierungsfaktor zwischen 0.1 und 3.0)
        val lengthFactor = min(errorAbs / MAX_ERROR_DEGREES, 1.0)
        val scaleFactor = 0.1f + lengthFactor.toFloat() * 2.9f // 0.1 bis 3.0

        // Vertikale Skalierung für Elevation-Abweichung (Y-Achse)
        elevationArrow.scaleX = 1.0f // Breite bleibt konstant
        elevationArrow.scaleY = scaleFactor

        // Position und Rotation so dass PFEILENDE im Zentrum ist
        val screenWidth = elevationArrow.context.resources.displayMetrics.widthPixels
        val screenHeight = elevationArrow.context.resources.displayMetrics.heightPixels
        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f

        // Setze Pivot-Punkt für Skalierung
        if (error > 0) {
            // Handy muss nach unten - Pfeil zeigt nach oben (gespiegelt)
            elevationArrow.pivotX = elevationArrow.width / 2f
            elevationArrow.pivotY = elevationArrow.height.toFloat() // Pivot am unteren Ende
            elevationArrow.x = centerX - elevationArrow.width / 2f
            elevationArrow.y = centerY - elevationArrow.height // Unteres Ende (Pivot) im Zentrum
            elevationArrow.rotation = 180f // Gedreht, Spitze zeigt nach oben (gespiegelt)
        } else {
            // Handy muss nach oben - Pfeil zeigt nach unten (gespiegelt)
            elevationArrow.pivotX = elevationArrow.width / 2f
            elevationArrow.pivotY = elevationArrow.height.toFloat() // Pivot am unteren Ende
            elevationArrow.x = centerX - elevationArrow.width / 2f
            elevationArrow.y = centerY - elevationArrow.height // Unteres Ende (Pivot) im Zentrum
            elevationArrow.rotation = 0f // Normal, Spitze zeigt nach unten (gespiegelt)
        }

        // Farb-Feedback durch Transparenz
        elevationArrow.alpha = when {
            errorAbs < 5.0 -> 1.0f   // Sehr genau - vollständig sichtbar
            errorAbs < 15.0 -> 0.8f  // Mäßig genau - leicht transparent
            else -> 0.6f             // Ungenau - deutlich transparent
        }
    }
}
