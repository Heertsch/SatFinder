package support.hb9hci.satfinder

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

object PermissionHelper {

    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(activity: android.app.Activity, permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    }

    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        expectedRequestCode: Int,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (requestCode == expectedRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onGranted()
            } else {
                onDenied()
            }
        }
    }
}
