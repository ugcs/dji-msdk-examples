package com.example.ugcssample.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.ugcssample.drone.DroneBridge
import com.example.ugcssample.drone.DroneBridgeImpl
import timber.log.Timber

class DjiAppMainServiceImpl : Service(), DjiAppMainService {
    private var binder: DjiAppMainServiceBinder? = null
    private var droneBridge: DroneBridge? = null
    override fun onBind(intent: Intent?): IBinder? {
        Timber.d("DjiAppMainServiceImpl onBind")
        return binder
    }

    override fun onCreate() {
        Timber.d("DjiAppMainServiceImpl onCreate")
        super.onCreate()
        val context = applicationContext
        binder = DjiAppMainServiceBinder(this)
        droneBridge = DroneBridgeImpl(context)
    }

    override fun onDestroy() {
        Timber.d("DjiAppMainServiceImpl onDestroy")
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        val action = intent?.action
        Timber.d("DjiAppMainServiceImpl onUnbind, action = %s", action)
        return super.onUnbind(intent)
    }

    override fun init() {
        droneBridge?.submitSdkInit()
    }

    override fun startSimulator() {
        droneBridge?.startModelSimulator()
    }

    override fun testCameraModes() {
        droneBridge?.testCameraModes()
    }

    private fun newDroneBridge(context: Context?): DroneBridge? {
        return context?.let { DroneBridgeImpl(it) }
    }
}