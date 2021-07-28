package com.example.ugcssample.drone

import android.content.Context
import android.hardware.usb.UsbAccessory
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.ugcssample.utils.AppScheduledExecutorService
import com.example.ugcssample.utils.ThreadUtils

abstract class DroneBridgeBase internal constructor(protected val context: Context) : DroneBridge {
    protected val lbm: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
    protected val usbAccessoryManager: UsbAccessoryManager
    abstract fun onUsbAccessoryConnectedSync(usbAccessory: UsbAccessory?)
    abstract fun onUsbAccessoryDisconnectedSync()

    companion object {
        val WORKER: AppScheduledExecutorService = ThreadUtils.newSingleThreadScheduledExecutor(
            DroneBridge::class.java
                                                                                )
    }

    init {
        val usbAccessoryManagerListener: UsbAccessoryManager.Listener =
            object : UsbAccessoryManager.Listener {
                override fun onUsbAccessoryConnected(usbAccessory: UsbAccessory?) {
                    WORKER.submit { onUsbAccessoryConnectedSync(usbAccessory) }
                }

                override fun onUsbAccessoryDisconnected() {
                    WORKER.submit { onUsbAccessoryDisconnectedSync() }
                }
            }
        usbAccessoryManager =
            UsbAccessoryManagerImplV2(context, WORKER, usbAccessoryManagerListener)
    }
}