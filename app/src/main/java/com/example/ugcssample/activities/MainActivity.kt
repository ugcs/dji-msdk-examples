package com.example.ugcssample.activities

import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.ugcssample.R
import com.example.ugcssample.drone.CameraTestCallableEnum
import com.example.ugcssample.drone.DroneBridgeImpl
import com.example.ugcssample.drone.camera.Lens
import com.example.ugcssample.fragment.VideoViewFragment
import com.example.ugcssample.services.DjiAppMainService
import com.example.ugcssample.services.DjiAppMainServiceBinder
import com.example.ugcssample.services.DjiAppMainServiceImpl
import com.example.ugcssample.utils.ArrayUtils
import com.example.ugcssample.utils.PermissionUtils
import dji.sdk.camera.VideoFeeder
import timber.log.Timber
import java.lang.reflect.Method

class MainActivity : AppCompatActivity() {
    companion object {
        private val EVENT_FILTER = IntentFilter()
        const val REQUEST_PERMISSION_CODE = 2358

        init {
            EVENT_FILTER.addAction(DroneBridgeImpl.ON_DRONE_CONNECTED)
        }
    }

    private var sIntent: Intent? = null
    private var sConn: ServiceConnection? = null
    private var btnSimulator: Button? = null
    private var btnDetectCameraModes: Button? = null
    protected var appMainService: DjiAppMainService? = null
    var broadcastManager: LocalBroadcastManager? = null
    private var primaryVideoFeedView: VideoViewFragment? = null
    private val eventReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            if (DroneBridgeImpl.ON_DRONE_CONNECTED == action) {
                btnSimulator!!.isEnabled = true
                btnDetectCameraModes!!.isEnabled = true
                primaryVideoFeedView!!.registerLiveVideo(
                    VideoFeeder.getInstance().primaryVideoFeed,
                    true
                                                        )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sIntent = Intent(this, DjiAppMainServiceImpl::class.java)
        sConn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                onMainServiceConnected(name, binder)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                onMainServiceDisconnected()
            }
        }
        primaryVideoFeedView =
            findViewById<View>(R.id.video_view_primary_video_feed) as VideoViewFragment
        btnSimulator = findViewById<View>(R.id.btn_simulator) as Button
        btnSimulator!!.setOnClickListener { appMainService!!.startSimulator() }
        btnDetectCameraModes = findViewById(R.id.btn_detect_camera_modes)
        btnDetectCameraModes?.setOnClickListener { appMainService!!.testCameraModes() }
    
    }

    protected fun onMainServiceConnected(name: ComponentName?, binder: IBinder) {
        appMainService = (binder as DjiAppMainServiceBinder).getService()
        checkAndRequestAndroidPermissions()
    }

    protected fun onMainServiceDisconnected() {}
    override fun onResume() {
        super.onResume()
        bindService(sIntent, sConn!!, BIND_AUTO_CREATE)
        broadcastManager = LocalBroadcastManager.getInstance(this)
        broadcastManager!!.registerReceiver(eventReceiver, EVENT_FILTER)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        unbindService(sConn!!)
        super.onDestroy()
    }

    private fun checkAndRequestAndroidPermissions() {
        val missing = PermissionUtils.checkForMissingPermission(applicationContext)
        if (!ArrayUtils.isEmpty(missing)) {
            val missingPermissions = missing?.toTypedArray()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && missingPermissions != null) {
                ActivityCompat.requestPermissions(this, missingPermissions, REQUEST_PERMISSION_CODE)
            } else {
                //failed
            }
        } else {
            //all android permissions granted;
            onAndroidPermissionsValid()
        }
    }

    protected fun onAndroidPermissionsValid() {
        appMainService!!.init()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
                                           ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            Timber.d("onRequestPermissionsResult...")
            checkAndRequestAndroidPermissions()
        }
    }
}