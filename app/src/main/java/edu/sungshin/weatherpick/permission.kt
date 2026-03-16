package edu.sungshin.weatherpick

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionDialog {

    private const val REQ_LOCATION = 100

    fun checkAndRequestLocation(activity: Activity) {
        val granted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            // 권한 없으면 시스템 권한 팝업 띄움
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQ_LOCATION
            )
        }
        // 이미 권한 있으면 여기서 바로 위치 사용 로직 실행하면 됨
    }

    fun show(activity: MainActivity, function: () -> Unit) {}
}
