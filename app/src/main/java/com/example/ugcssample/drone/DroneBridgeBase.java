package com.example.ugcssample.drone;

import android.content.Context;
import android.hardware.usb.UsbAccessory;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.ugcssample.utils.AppScheduledExecutorService;
import com.example.ugcssample.utils.ThreadUtils;

public abstract class DroneBridgeBase implements DroneBridge {

    protected static final AppScheduledExecutorService WORKER = ThreadUtils.newSingleThreadScheduledExecutor(DroneBridge.class);

    protected final Context context;
    protected final LocalBroadcastManager lbm;
    protected final UsbAccessoryManager usbAccessoryManager;

    DroneBridgeBase(Context context) {
        this.context = context;
        this.lbm = LocalBroadcastManager.getInstance(context);
        UsbAccessoryManager.Listener usbAccessoryManagerListener = new UsbAccessoryManager.Listener() {

            @Override
            public void onUsbAccessoryConnected(final UsbAccessory usbAccessory) {
                WORKER.submit(() -> DroneBridgeBase.this.onUsbAccessoryConnectedSync(usbAccessory));
            }

            @Override
            public void onUsbAccessoryDisconnected() {
                WORKER.submit(DroneBridgeBase.this::onUsbAccessoryDisconnectedSync);
            }
        };
        this.usbAccessoryManager = new UsbAccessoryManagerImplV2(context, WORKER, usbAccessoryManagerListener);
    }

    abstract void onUsbAccessoryConnectedSync(UsbAccessory usbAccessory);

    abstract void onUsbAccessoryDisconnectedSync();

    protected Context getContext() {
        return context;
    }
}