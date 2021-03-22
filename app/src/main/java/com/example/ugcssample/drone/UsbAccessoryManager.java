package com.example.ugcssample.drone;

import android.hardware.usb.UsbAccessory;

public interface UsbAccessoryManager {

    interface Listener {
        /**
         * There is a USB accessor, which is ready for use.
         */
        void onUsbAccessoryConnected(UsbAccessory usbAccessory);

        /**
         * No available USB assessor.
         */
        void onUsbAccessoryDisconnected();
    }

    void init();

    void check();

}

