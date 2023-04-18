package com.example.ugcssample

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.SDKManager
import dji.v5.manager.aircraft.payload.PayloadCenter
import dji.v5.manager.aircraft.payload.PayloadIndexType
import dji.v5.manager.interfaces.SDKManagerCallback

class MainActivity : AppCompatActivity() {
    private val TAG: String = MainActivity::class.java.name
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sdk = SDKManager.getInstance()
        sdk.init(this@MainActivity, object : SDKManagerCallback {
            override fun onRegisterSuccess() {
                onSuccess()
            }

            override fun onRegisterFailure(error: IDJIError?) {
                printLog("Failed to register MSDK: ${error?.description()}")
            }

            override fun onProductDisconnect(productId: Int) {
            }

            override fun onProductConnect(productId: Int) {
            }

            override fun onProductChanged(productId: Int) {
            }

            override fun onInitProcess(event: DJISDKInitEvent?, totalProcess: Int) {
                if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
                    sdk.registerApp()
                }
            }

            override fun onDatabaseDownloadProgress(current: Long, total: Long) {
            }

        })
    }

    private fun onSuccess() {
        printLog("MSDK registered successfully.")
        // We are using M300 RTK's OSDK port.
        val payloadManager = PayloadCenter.getInstance().payloadManager[PayloadIndexType.EXTERNAL]!!
        payloadManager.addPayloadDataListener {
            val message = "Received data: ${it.contentToString()}"
            Log.i(TAG, message)
            printLog(message)
        }
        val sampleOfData = byteArrayOf(8, 101, 18, 14, 8, -6, 78, 18, 9, 42, 7, 82)
        payloadManager.sendDataToPayload(sampleOfData, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                val message = "Data has been successfully sent."
                Log.i(TAG, message)
                printLog(message)
            }

            override fun onFailure(error: IDJIError) {
                val errorMessage = "Failed to send data to payload. Error code=${error.errorCode()}, description=${error.description()}"
                Log.e(TAG, errorMessage)
                printLog(errorMessage)
            }
        })
    }

    private fun printLog(message: String) {
        runOnUiThread {
            findViewById<TextView>(R.id.log).append(message + "\n")
        }
    }
}