package com.example.ugcssample.drone

import android.content.Context
import android.hardware.usb.UsbManager
import com.example.ugcssample.utils.AppScheduledExecutorService
import timber.log.Timber

class UsbAccessoryManagerImplV2(
    private val context: Context?,
    private val worker: AppScheduledExecutorService?,
    private val listener: UsbAccessoryManager.Listener?
                               ) : UsbAccessoryManager {
    private val permissionsRequesting = false
    override fun init() {}
    override fun check() {
        worker?.submit(Runnable { checkSync() })
    }

    private fun checkSync() {
        if (permissionsRequesting) return
        val usbManager = context?.getSystemService(Context.USB_SERVICE) as UsbManager
        if (usbManager == null) {
            Timber.w("usbManager == null")
            return
        }
        val usbAccessories = usbManager.accessoryList
        if (usbAccessories == null) {
            Timber.i("No usb accessories found.")
            return
        }
        Timber.i("There are %d attached UsbAccessory", usbAccessories.size)
        for (usbAccessory in usbAccessories) {
            val hasPermission = usbManager.hasPermission(usbAccessory)
            Timber.i("permission(%b) - %s", hasPermission, usbAccessory.toString())
            if (hasPermission) {
                listener?.onUsbAccessoryConnected(usbAccessory)
            }
        }
    }
}