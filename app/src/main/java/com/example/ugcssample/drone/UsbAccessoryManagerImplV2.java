package com.example.ugcssample.drone;

import android.content.Context;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

import com.example.ugcssample.utils.AppScheduledExecutorService;

import timber.log.Timber;

public class UsbAccessoryManagerImplV2 implements UsbAccessoryManager {

    private final Context context;
    private final AppScheduledExecutorService worker;
    private final UsbAccessoryManager.Listener listener;

    private boolean permissionsRequesting = false;

    public UsbAccessoryManagerImplV2(Context context, AppScheduledExecutorService worker, Listener listener) {
        this.context = context;
        this.worker = worker;
        this.listener = listener;
    }

    @Override
    public void init() {

    }

    @Override
    public void check() {
        worker.submit(new Runnable() {
            @Override
            public void run() {
                checkSync();
            }
        });
    }

    private void checkSync() {
        if (permissionsRequesting)
            return;
        UsbManager usbManager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            Timber.w("usbManager == null");
            return;
        }

        UsbAccessory[] usbAccessories = usbManager.getAccessoryList();
        if (usbAccessories == null) {
            Timber.i("No usb accessories found.");
            return;
        }

        Timber.i("There are %d attached UsbAccessory", usbAccessories.length);
        for (UsbAccessory usbAccessory : usbAccessories) {
            boolean hasPermission = usbManager.hasPermission(usbAccessory);
            Timber.i("permission(%b) - %s", hasPermission, usbAccessory.toString());
            if (hasPermission) {
                listener.onUsbAccessoryConnected(usbAccessory);
            }
        }
    }
}