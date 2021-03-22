package com.example.ugcssample.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public final class PermissionUtils {

    public static final int REQUEST_PERMISSION_CODE = 2358;

    public static final String[] REQUIRED_PERMISSION_LIST = new String[]{
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
        Manifest.permission.READ_PHONE_STATE,
        // only in DJI SDK Demo app
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.RECORD_AUDIO,
        // my
        //Manifest.permission.SYSTEM_ALERT_WINDOW,
        //Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS
    };

    /**
     * Checks if there is any missing permissions
     */
    public static List<String> checkForMissingPermission(Context ctx) {
        return checkForMissingPermission(ctx, REQUIRED_PERMISSION_LIST);
    }

    public static List<String> checkForMissingPermission(Context ctx, String[] requiredPermissionList) {
        List<String> missingPermission = new ArrayList<>();
        // Check for permissions
        for (String one : requiredPermissionList) {
            if (ContextCompat.checkSelfPermission(ctx, one) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(one);
            }
        }
        return missingPermission;
    }

    private PermissionUtils() {
        // Utility class
    }

}
