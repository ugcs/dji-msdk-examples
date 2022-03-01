package com.example.ugcssample.drone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbAccessory
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.ugcssample.drone.camera.Camera
import com.example.ugcssample.drone.camera.DjiCamera
import com.example.ugcssample.utils.PermissionCheckResult
import com.example.ugcssample.utils.ToastUtils
import com.google.gson.GsonBuilder
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.common.flightcontroller.simulator.InitializationData
import dji.common.model.LocationCoordinate2D
import dji.common.util.CommonCallbacks.CompletionCallbackWith
import dji.keysdk.FlightControllerKey
import dji.keysdk.KeyManager
import dji.keysdk.ProductKey
import dji.keysdk.callback.KeyListener
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.base.BaseProduct.ComponentKey
import dji.sdk.base.DJIDiagnostics
import dji.sdk.basestation.BaseStation
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.sdkmanager.DJISDKManager.SDKManagerCallback
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


class DroneBridgeImpl(private val mContext: Context) : DroneBridgeBase(mContext), DroneBridge {
    private val serialNumberKey = FlightControllerKey.create(FlightControllerKey.SERIAL_NUMBER)
    private val connectionKey = ProductKey.create(ProductKey.CONNECTION)
    private val connectionKeyListener =
        KeyListener { oldValue: Any?, newValue: Any? -> onConnectionChanged(newValue) }
    override var permissionCheckResult: PermissionCheckResult? = null
    private var aircraft: Aircraft? = null
    override val aircraftInstance: Aircraft?
        get() = aircraft
    private val baseStation: BaseStation? = null
    private val locationManager: LocationManager

    //private final MySimulatorUpdateCallbackAndController simulatorUpdateCallbackAndController;
    private var droneInitFuture: ScheduledFuture<*>? = null
    private val cameraSet: MutableList<Camera> = ArrayList()
    override fun submitLocationInit() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
                                              ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
                                                 ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
    }

    override fun submitSdkInit() {
        WORKER.submit {
            Timber.i("DJISDKManager -> registerApp...")
            ToastUtils.setResultToToast("DJISDKManager -> registerApp..")
            Timber.i("DJISDKManager -> SDK VERSION = %s", DJISDKManager.getInstance().sdkVersion)
            if (DJISDKManager.getInstance().hasSDKRegistered()) {
                //ToastUtils.setResultToToast("DJISDKManager -> already registered");
                //Timber.i("DJISDKManager hasSDKRegistered.");
                //return;
            }
            permissionCheckResult = null
            DJISDKManager.getInstance().registerApp(context, object : SDKManagerCallback {
                override fun onRegister(djiError: DJIError) {
                    this@DroneBridgeImpl.onRegister(djiError)
                }

                override fun onProductDisconnect() {
                    this@DroneBridgeImpl.onProductDisconnect()
                }

                override fun onProductConnect(baseProduct: BaseProduct) {
                    this@DroneBridgeImpl.onProductConnect(baseProduct)
                }

                override fun onProductChanged(baseProduct: BaseProduct) {}
                override fun onComponentChange(
                    key: ComponentKey?, oldComponent: BaseComponent?,
                    newComponent: BaseComponent?
                                              ) {
                    onComponentChanged(key, oldComponent, newComponent)
                }

                override fun onInitProcess(djisdkInitEvent: DJISDKInitEvent, i: Int) {}
                override fun onDatabaseDownloadProgress(currentSize: Long, totalSize: Long) {}
            })
        }
    }

    override fun onUsbAccessoryAttached() {
        Timber.i("onUsbAccessoryAttached")
        usbAccessoryManager.check()
    }

    override fun onUsbAccessoryConnectedSync(usbAccessory: UsbAccessory?) {
        if (!DJISDKManager.getInstance().hasSDKRegistered()) {
            return
        }
        val attachedIntent = Intent()
        attachedIntent.action = DJISDKManager.USB_ACCESSORY_ATTACHED
        context.sendBroadcast(attachedIntent)
    }
    
    override fun onUsbAccessoryDisconnectedSync() {}

    /**
     * Callback method after the application attempts to register.
     *
     *
     * in method initSdkManager (see above) we are passing sdkManagerCallback
     */
    private fun onRegister(registrationResult: DJIError?) {
        WORKER.submit {
            val pc = PermissionCheckResult()
            var startConnect = false
            if (registrationResult == null || registrationResult === DJISDKError.REGISTRATION_SUCCESS) {
                ToastUtils.setResultToToast("DJISDKManager -> onRegister OK")
                Timber.i("DJISDKManager -> onRegister OK")
                pc.permissionCheckResult = DJISDKError.REGISTRATION_SUCCESS.toString()
                pc.permissionCheckResultDesc = DJISDKError.REGISTRATION_SUCCESS.description
                startConnect = true
            } else {
                pc.permissionCheckResult = registrationResult.toString()
                pc.permissionCheckResultDesc = registrationResult.description
                ToastUtils.setResultToToast(
                    String.format(
                        "DJISDKManager -> onRegister ERROR = %s (%s)",
                        pc.permissionCheckResult, pc.permissionCheckResultDesc
                                 )
                                           )
                Timber.w(
                    "DJISDKManager -> onRegister ERROR = %s (%s)",
                    pc.permissionCheckResult, pc.permissionCheckResultDesc
                        )
            }
            permissionCheckResult = pc
            if (startConnect) {
                startConnectionToProduct()
            }
        }
        if (registrationResult == null) {
            Timber.i("DJI SDK is initialised")
        } else {
            Timber.e("DJI SDK initialisation failure. Error: %s", registrationResult.description)
        }
    }

    private fun onProductDisconnect() {
        Timber.d("onProductDisconnect")
    }

    private fun onProductConnect(baseProduct: BaseProduct) {
        val af = baseProduct as Aircraft
        af.flightController.getSerialNumber(object : CompletionCallbackWith<String> {
            override fun onSuccess(s: String) {
                if (droneInitFuture != null) {
                    droneInitFuture!!.cancel(false)
                    droneInitFuture = null
                }
                droneInitFuture = WORKER.schedule({ initDrone(s) }, 2, TimeUnit.SECONDS)
            }

            override fun onFailure(djiError: DJIError) {
                ToastUtils.setResultToToast(
                    String.format(
                        "getSerialNumber method - error: %s",
                        djiError.description
                                 )
                                           )
                Timber.i("getSerialNumber method - error: %s", djiError.description)
            }
        })
        Timber.d("onProductConnect")
    }

    /**
     * Starts a connection between the SDK and the DJI product.
     * This method should be called after successful registration of the app and once there is a data connection
     * between the mobile device and DJI product.
     */
    private fun startConnectionToProduct() {
        WORKER.submit {
            ToastUtils.setResultToToast("DJISDKManager -> startConnectionToProduct...")
            Timber.i("DJISDKManager -> startConnectionToProduct...")
            val km = KeyManager.getInstance()
            km.addListener(connectionKey, connectionKeyListener)
            DJISDKManager.getInstance().startConnectionToProduct()
        }
    }

    private fun onConnectionChanged(newValue: Any?) {
        if (newValue is Boolean) {
            WORKER.submit {
                if (newValue) {
                    Timber.i("KeyManager -> onConnectionChanged = true")
                    // We will not do anything, we will wait for serial number update.
                } else {
                    Timber.w("KeyManager -> onConnectionChanged = false")
                    disconnectDrone()
                }
            }
        }
    }

    private fun onSerialNumberChanged(newValue: Any) {
        if (newValue is String) {
            val sn = newValue
            WORKER.submit {
                Timber.i("KeyManager -> onSerialNumberChanged = %s", sn)
                if (droneInitFuture != null) {
                    droneInitFuture!!.cancel(false)
                    droneInitFuture = null
                }
                droneInitFuture = WORKER.schedule({ initDrone(sn) }, 2, TimeUnit.SECONDS)
            }
        }
    }

    // ProductKey.COMPONENT_KEY - Not works in SDK 4.3.2, so this method is invoked vis baseProductListener
    private fun onComponentChanged(
        key: ComponentKey?, oldComponent: BaseComponent?,
        newComponent: BaseComponent?
                                  ) {
        WORKER.submit {
            if (key == null) {
                return@submit
            }
            Timber.i(
                "DJISDKManager -> onComponentChanged %s (%s to %s)",
                key.toString(),
                if (oldComponent == null) "NULL" else "CONNECTED",
                if (newComponent == null) "NULL" else "CONNECTED"
                    )
            when (key) {
                ComponentKey.BATTERY -> {
                }
                ComponentKey.BASE_STATION -> {
                }
                ComponentKey.CAMERA -> {
                }
                else -> {
                }
            }
        }
    }

    private fun initDrone(serial: String) {
        droneInitFuture = null
        Timber.i("OK, starting drone init...")
        val mProduct = DJISDKManager.getInstance().product as? Aircraft ?: return
        aircraft = mProduct
        Timber.i("aircraft connected %s", aircraft!!.model.displayName)
        ToastUtils.setResultToToast(
            String.format(
                "aircraft connected %s",
                aircraft!!.model.displayName
                         )
                                   )
        if (aircraft != null) {
            aircraft!!.setDiagnosticsInformationCallback { list: List<DJIDiagnostics> ->
                synchronized(this) {
                    for (message in list) {
                        Timber.i(
                            "DJIDiagnostics: %s - %s: %s",
                            message.code,
                            message.reason,
                            message.solution
                                )
                    }
                }
            }
        }
        val cameraList = aircraft!!.cameras
        var isCamerasInitialized = true
        if (cameraList != null) {
            for (camera in cameraList) {
                isCamerasInitialized = isCameraInitialized(camera)
                if (isCamerasInitialized) {
                    val djiCamera = DjiCamera(camera, context)
                    cameraSet.add(djiCamera)
                } else {
                    break
                }
            }
        }
        val intent = Intent()
        intent.action = DroneActions.ON_DRONE_CONNECTED.toString()
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun isCameraInitialized(camera: dji.sdk.camera.Camera): Boolean {
        if (camera.displayName == null) {
            return false
        }
        if (camera.lenses != null && !camera.lenses!!.isEmpty()) {
            for (lens in camera.lenses!!) {
                if (lens.displayName == null || lens.type == null) {
                    return false
                }
            }
        }
        return true
    }

    private fun disconnectDrone() {
        // Drone is disconnected
        // 1. cancel initialization future if it is scheduled
        if (droneInitFuture != null) {
            droneInitFuture!!.cancel(false)
            droneInitFuture = null
        }
        val mProduct = DJISDKManager.getInstance().product
        Timber.i("aircraft disconnected")
        Timber.d(
            "Drone connection lost, in DJISDKManager BaseProduct %s",
            if (mProduct == null) "NULL" else "EXISTS"
                )
        aircraft = null
        val intent = Intent()
        intent.action = DroneActions.ON_DRONE_DISCONNECTED.toString()
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun deInitDroneUpdate() {
        Timber.i("tearDownKeyListeners... (ALL)")
    }

    /**
     * Gets instance of the specific product connected after the
     * API KEY is successfully validated. Please make sure the
     * API_KEY has been added in the Manifest
     */


    override fun onDestroy() {
        deInitDroneUpdate()
        tearDownKeyListeners()
        val mng = DJISDKManager.getInstance()
        if (mng != null) {
            try {
                mng.stopConnectionToProduct()
                mng.destroy()
            } catch (e: Exception) {
                Timber.i("DJISDKManager.getInstance().stopConnectionToProduct -> NullPointer again")
            }
        }
    }

    private fun tearDownKeyListeners() {
        val km = KeyManager.getInstance()
        km?.removeListener(connectionKeyListener)
    }

    override fun startModelSimulator() {
        WORKER.submit {
            aircraft!!.flightController.simulator.start(InitializationData.createInstance(
                LocationCoordinate2D(BASE_LATITUDE, BASE_LONGITUDE),
                REFRESH_FREQ,
                SATELLITE_COUNT)
                                                       ) { djiError ->
                val result =
                    if (djiError == null) "Simulator started" else djiError.description
                Timber.i(result)
                ToastUtils.setResultToToast(result)
            }
        }
    }
    val job = SupervisorJob()                               // (1)
    val scope = CoroutineScope(Dispatchers.Default + job)
    

    @RequiresApi(Build.VERSION_CODES.N)
    override fun testCameraModes() {
    
        scope.launch {
            val intent = Intent()
            val name = aircraft?.model?.displayName ?: throw IllegalStateException("Drone not connected")
            intent.action = DroneActions.CAMERA_TESTS_STARTED.toString()
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            val testerListResults = mutableListOf<CameraResultsInfo>()
            for (camera in cameraSet) {
                val tester = CameraTester(camera)
                val results = tester.runTests()
                testerListResults.add(results)
            }
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH_mm")
            val date = dateFormat.format(Date())
            val  report = testerListResults
            val filePath = "${context.getExternalFilesDir(null)}/cam_test_report_${name}_$date.json"
            FileWriter(filePath).use { writer ->
                val gson = GsonBuilder().setPrettyPrinting().create()
                gson.toJson(report, writer)
                writer.flush()
                writer.close()
            }
            intent.action = DroneActions.CAMERA_TESTS_FINISHED.toString()
            intent.putExtra("file-path",filePath)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    
        }
    }

    companion object {
        private const val BASE_LATITUDE = 22.0
        private const val BASE_LONGITUDE = 113.0
        private const val REFRESH_FREQ = 10
        private const val SATELLITE_COUNT = 10
    }
    enum class DroneActions {
        ON_DRONE_CONNECTED,
        ON_DRONE_DISCONNECTED,
        CAMERA_TESTS_STARTED,
        CAMERA_TESTS_FINISHED,
        
    }
    init {

        // this.simulatorUpdateCallbackAndController
        //         = new MySimulatorUpdateCallbackAndController(vehicleModelContainer, lbm);
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
}