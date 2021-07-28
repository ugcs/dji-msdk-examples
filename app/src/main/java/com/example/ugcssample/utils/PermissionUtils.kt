package com.example.ugcssample.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.util.*

object PermissionUtils {
    const val REQUEST_PERMISSION_CODE = 2358
    val REQUIRED_PERMISSION_LIST: Array<String?>? = arrayOf(
        Manifest.permission.VIBRATE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.READ_PHONE_STATE,  // only in DJI SDK Demo app
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.RECORD_AUDIO
                                                           )

    /**
     * Checks if there is any missing permissions
     */
    @JvmOverloads
    fun checkForMissingPermission(
        ctx: Context?,
        requiredPermissionList: Array<String?>? = REQUIRED_PERMISSION_LIST
                                 ): MutableList<String?>? {
        val missingPermission: MutableList<String?> = ArrayList()
        // Check for permissions
        if (requiredPermissionList != null && ctx != null) {
            for (one in requiredPermissionList) {
                if (ContextCompat.checkSelfPermission(ctx, one.toString()) != PackageManager.PERMISSION_GRANTED) {
                    missingPermission.add(one)
                }
            }
        }
        return missingPermission
    }
}