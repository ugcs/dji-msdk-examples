package com.example.ugcssample.drone

import android.hardware.usb.UsbAccessory

interface UsbAccessoryManager {
    interface Listener {
        /**
         * There is a USB accessor, which is ready for use.
         */
        open fun onUsbAccessoryConnected(usbAccessory: UsbAccessory?)

        /**
         * No available USB assessor.
         */
        open fun onUsbAccessoryDisconnected()
    }

    open fun init()
    open fun check()
}