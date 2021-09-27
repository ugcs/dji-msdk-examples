package com.example.ugcssample.activities

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import com.example.ugcssample.activities.MainActivity
import com.example.ugcssample.drone.DroneBridgeImpl
import android.widget.TextView
import com.example.ugcssample.services.DjiAppMainService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.ugcssample.fragment.VideoViewFragment
import dji.sdk.camera.VideoFeeder
import com.example.ugcssample.drone.BeaconController
import android.os.Bundle
import com.example.ugcssample.R
import com.example.ugcssample.services.DjiAppMainServiceImpl
import android.os.IBinder
import com.example.ugcssample.services.DjiAppMainServiceBinder
import com.example.ugcssample.utils.PermissionUtils
import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.ugcssample.utils.ArrayUtils
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope  {
    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    
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
    private var btnBeacon: Button? = null
    private var tvBeaconState: TextView? = null
    protected var appMainService: DjiAppMainService? = null
    var broadcastManager: LocalBroadcastManager? = null
    private var primaryVideoFeedView: VideoViewFragment? = null
    private val eventReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            if (DroneBridgeImpl.ON_DRONE_CONNECTED == action) {
                btnSimulator!!.isEnabled = true
                btnBeacon!!.isEnabled = true
                primaryVideoFeedView!!.registerLiveVideo(VideoFeeder.getInstance().primaryVideoFeed, true)
                outputBeaconState()
            }
        }
    }
    
    private fun outputBeaconState() {
        appMainService?.beaconController?.let { beaconController ->
            launch {
                val text = "Connected: ${beaconController.areBeaconsSupported()}\nEnabled:${beaconController.areBeaconsSwitchOn()}"
                runOnUiThread {
                    tvBeaconState!!.setText(text)
                }
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
        primaryVideoFeedView = findViewById<View>(R.id.video_view_primary_video_feed) as VideoViewFragment
        btnSimulator = findViewById<View>(R.id.btn_simulator) as Button
        btnSimulator!!.setOnClickListener { v: View? -> appMainService!!.startSimulator() }
        btnBeacon = findViewById(R.id.btn_beacon_trigger)
        tvBeaconState = findViewById(R.id.tv_beacon_state)
        outputBeaconState()
        btnBeacon?.setOnClickListener {
            val controller = appMainService?.beaconController
            Toast.makeText(this,"Switching beacon",Toast.LENGTH_SHORT).show()
            launch {
                controller?.switchBeaconsOn(!controller.areBeaconsSwitchOn())
                runOnUiThread {
                    Toast.makeText(baseContext,"Switched beacon",Toast.LENGTH_SHORT).show()
                }
                outputBeaconState()
            }
        }
    }
    
    protected fun onMainServiceConnected(name: ComponentName?, binder: IBinder) {
        appMainService = (binder as DjiAppMainServiceBinder).service
        checkAndRequestAndroidPermissions()
        outputBeaconState()
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
            val missingPermissions = missing.toTypedArray()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this, missingPermissions, REQUEST_PERMISSION_CODE)
            }
            else {
                //failed
            }
        }
        else {
            //all android permissions granted;
            onAndroidPermissionsValid()
        }
    }
    
    protected fun onAndroidPermissionsValid() {
        appMainService!!.init()
    }
    
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            Timber.d("onRequestPermissionsResult...")
            checkAndRequestAndroidPermissions()
        }
    }
}