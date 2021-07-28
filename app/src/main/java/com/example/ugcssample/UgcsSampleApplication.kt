package com.example.ugcssample

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.secneo.sdk.Helper
import dji.sdk.base.BaseProduct
import dji.sdk.sdkmanager.BluetoothProductConnector
import timber.log.Timber
import timber.log.Timber.DebugTree

/**
 * Main application
 */
class UgcsSampleApplication : Application() {
    override fun attachBaseContext(paramContext: Context?) {
        super.attachBaseContext(paramContext)
        MultiDex.install(this)
        Helper.install(this)
        app = this
    }

    override fun onCreate() {
        super.onCreate()
        // This will initialise Timber
        Timber.plant(DebugTree())
    }

    companion object {
        val TAG: String? = UgcsSampleApplication::class.java.name
        private val product: BaseProduct? = null
        private val bluetoothConnector: BluetoothProductConnector? = null
        private var app: Application? = null

        /**
         * Gets instance of the specific product connected after the
         * API KEY is successfully validated. Please make sure the
         * API_KEY has been added in the Manifest
         */
        fun getInstance(): Application? {
            return app
        }
    }
}