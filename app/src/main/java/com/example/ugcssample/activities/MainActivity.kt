package com.example.ugcssample.activities

import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.ugcssample.R
import com.example.ugcssample.drone.DroneBridgeImpl
import com.example.ugcssample.fragment.VideoViewFragment
import com.example.ugcssample.services.DjiAppMainService
import com.example.ugcssample.services.DjiAppMainServiceBinder
import com.example.ugcssample.services.DjiAppMainServiceImpl
import com.example.ugcssample.utils.ArrayUtils
import com.example.ugcssample.utils.ExceptionWriter
import com.example.ugcssample.utils.PermissionUtils
import dji.sdk.camera.VideoFeeder
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    companion object {
        private val EVENT_FILTER = IntentFilter()
        const val REQUEST_PERMISSION_CODE = 2358

        init {
            DroneBridgeImpl.DroneActions.values().forEach { EVENT_FILTER.addAction(it.name) }
            
        }
    }

    private var sIntent: Intent? = null
    private var sConn: ServiceConnection? = null
    private var btnDetectCameraModes: Button? = null
    private var tvStatus: TextView? = null
    protected var appMainService: DjiAppMainService? = null
    var broadcastManager: LocalBroadcastManager? = null
    private var primaryVideoFeedView: VideoViewFragment? = null
    private val eventReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context : Context, intent : Intent) {
            val action = intent.action ?: return
            when (DroneBridgeImpl.DroneActions.valueOf(action)) {
                DroneBridgeImpl.DroneActions.ON_DRONE_CONNECTED ->
                {
                    tvStatus?.text = ("Drone ready. Press 'Start Tests'")
                    btnDetectCameraModes!!.isEnabled = true
                    primaryVideoFeedView!!.registerLiveVideo(
                            VideoFeeder.getInstance().primaryVideoFeed,
                            true
                                                            )
                }
                DroneBridgeImpl.DroneActions.ON_DRONE_DISCONNECTED ->
                {
                    tvStatus?.text = ("Drone disconnected. Connect drone")
                }
                DroneBridgeImpl.DroneActions.CAMERA_TESTS_STARTED ->
                {
                    btnDetectCameraModes?.isEnabled = false
                    tvStatus?.text = ("Tests started. Please, wait")
                    Toast.makeText(context, "Tests started. Please, wait", Toast.LENGTH_SHORT).show()
                }
                DroneBridgeImpl.DroneActions.CAMERA_TESTS_PROGRESS ->
                {
                    tvStatus?.text = ("Tests running. Test #${intent.getIntExtra("test-index", -1)}")
                }
                DroneBridgeImpl.DroneActions.CAMERA_TESTS_FINISHED ->
                {
                    btnDetectCameraModes?.isEnabled = true
                    tvStatus?.text = ("File dumped at\n${intent.getStringExtra("file-path")}")
                    Toast.makeText(context, "Tests finished. File dumped", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState : Bundle?) {
        ExceptionWriter.setupCrashHandler(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sIntent = Intent(this, DjiAppMainServiceImpl::class.java)
        sConn = object : ServiceConnection {
            override fun onServiceConnected(name : ComponentName, binder : IBinder) {
                onMainServiceConnected(name, binder)
            }

            override fun onServiceDisconnected(name : ComponentName) {
                onMainServiceDisconnected()
            }
        }
        primaryVideoFeedView =
            findViewById<View>(R.id.video_view_primary_video_feed) as VideoViewFragment
        btnDetectCameraModes = findViewById(R.id.btn_detect_camera_modes)
        tvStatus = findViewById(R.id.tv_status)
        btnDetectCameraModes?.setOnClickListener {
            appMainService!!.testCameraModes()
            
        }
    
        
    }

    protected fun onMainServiceConnected(name : ComponentName?, binder : IBinder) {
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
            requestCode : Int,
            permissions : Array<String>,
            grantResults : IntArray
                                           ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            Timber.d("onRequestPermissionsResult...")
            checkAndRequestAndroidPermissions()
        }
    }
}