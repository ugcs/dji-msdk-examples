package com.example.ugcssample.drone

import android.content.Context
import com.example.ugcssample.utils.PermissionCheckResult
import dji.sdk.products.Aircraft

interface DroneBridge {
    fun submitLocationInit()
    fun submitSdkInit()
    fun onUsbAccessoryAttached()
    val permissionCheckResult: PermissionCheckResult?
    val aircraftInstance: Aircraft?
    fun startModelSimulator()
    fun onDestroy()
    fun testCameraModes()
}