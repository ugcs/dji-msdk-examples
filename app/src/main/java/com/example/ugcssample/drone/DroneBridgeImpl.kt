package com.example.ugcssample.drone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbAccessory
import android.location.LocationManager
import android.provider.MediaStore.Video
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.dji.frame.util.V_JsonUtil.gson
import com.example.ugcssample.drone.domain.RouteModel
import com.example.ugcssample.drone.domain.RouteModelV2
import com.example.ugcssample.utils.PermissionCheckResult
import com.example.ugcssample.utils.ToastUtils
import com.google.gson.reflect.TypeToken
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.common.error.DJIWaypointV2Error
import dji.common.flightcontroller.simulator.InitializationData
import dji.common.mission.waypoint.*
import dji.common.mission.waypointv2.Action.*
import dji.common.mission.waypointv2.WaypointV2
import dji.common.mission.waypointv2.WaypointV2Mission
import dji.common.mission.waypointv2.WaypointV2MissionTypes
import dji.common.model.LocationCoordinate2D
import dji.common.product.Model
import dji.common.util.CommonCallbacks.CompletionCallbackWith
import dji.keysdk.*
import dji.keysdk.callback.ActionCallback
import dji.keysdk.callback.KeyListener
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.base.BaseProduct.ComponentKey
import dji.sdk.base.DJIDiagnostics
import dji.sdk.basestation.BaseStation
import dji.sdk.mission.MissionControl
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener
import dji.sdk.mission.waypoint.WaypointV2ActionListener
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.sdkmanager.DJISDKManager.SDKManagerCallback
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference


class DroneBridgeImpl(val mContext : Context) : DroneBridgeBase(mContext), DroneBridge {
    enum class BroadcastActions {
        ON_DRONE_CONNECTED, ON_MISSION_STATUS, ON_SIMULATOR_START
    }
    
    private val serialNumberKey = FlightControllerKey.create(FlightControllerKey.SERIAL_NUMBER)
    private val connectionKey = ProductKey.create(ProductKey.CONNECTION)
    private val connectionKeyListener = KeyListener { oldValue: Any?, newValue: Any? -> onConnectionChanged(newValue) }
    override var permissionCheckResult: PermissionCheckResult? = null
        private set
    
    /**
     * Gets instance of the specific product connected after the
     * API KEY is successfully validated. Please make sure the
     * API_KEY has been added in the Manifest
     */
    override var aircraftInstance: Aircraft? = null
        private set
    private val baseStation: BaseStation? = null
    private val locationManager: LocationManager
    
    private var droneInitFuture: ScheduledFuture<*>? = null
    private val waypointMissionOperatorListener: WaypointMissionOperatorListener = object : WaypointMissionOperatorListener {
        override fun onDownloadUpdate(waypointMissionDownloadEvent: WaypointMissionDownloadEvent) {}
        override fun onUploadUpdate(waypointMissionState: WaypointMissionUploadEvent) {
            val up = waypointMissionState.progress
            val currentState = waypointMissionState.currentState
            val previousState = waypointMissionState.previousState
            if (currentState === WaypointMissionState.UPLOADING) {
                if (up != null) {
                    val index = up.uploadedWaypointIndex + 1
                    val size = up.totalWaypointCount
                    val progress = index.toFloat() / size
                    Timber.i("MissionUploadProgress = %3.3f (%d/%d)", progress, index, size)
                    reportMission(1, String.format("WP upload %3.0f%%", progress))
                }
            }
            else if (currentState === WaypointMissionState.READY_TO_EXECUTE) {
                reportMission(1, "WP upload 100%")
            }
        }
        
        override fun onExecutionUpdate(waypointMissionExecutionEvent: WaypointMissionExecutionEvent) {
            val progress = waypointMissionExecutionEvent.progress
            val tp = progress!!.targetWaypointIndex
            val reached = progress.isWaypointReached
            val state = progress.executeState
            val total = progress.totalWaypointCount
            reportMission(3, String.format("WP running. Completed  %d / %d", tp, total))
        }
        
        override fun onExecutionStart() {
            reportMission(3, "WP - Exec start")
        }
        
        override fun onExecutionFinish(djiError: DJIError?) {
            reportMission(3, "WP - Exec Finish")
        }
    }
    private var missionActionsAwaitingUpload: List<WaypointV2Action>? = null
    private val waypointV2ActionListener: WaypointV2ActionListener = object : WaypointV2ActionListener {
        override fun onDownloadUpdate(@NotNull actionDownloadEvent: ActionDownloadEvent) {}
        override fun onUploadUpdate(@NotNull actionUploadEvent: ActionUploadEvent) {
            val currentState = actionUploadEvent.currentState
            uploadActionsV2(currentState)
        }
        
        override fun onExecutionUpdate(@NotNull actionExecutionEvent: ActionExecutionEvent) {}
        override fun onExecutionStart(i: Int) {}
        override fun onExecutionFinish(i: Int, @Nullable djiWaypointV2Error: DJIWaypointV2Error?) {}
    }
    
    private fun uploadActionsV2(currentState: ActionState) {
        if (currentState == ActionState.READY_TO_UPLOAD
            && missionActionsAwaitingUpload != null
            && missionActionsAwaitingUpload!!.isNotEmpty()) {
            val waypointV2MissionOperator = MissionControl.getInstance().waypointMissionV2Operator
            waypointV2MissionOperator!!.uploadWaypointActions(missionActionsAwaitingUpload) { djiWaypointV2ErrorActions: DJIWaypointV2Error? ->
                if (djiWaypointV2ErrorActions != null) {
                    Timber.e("Action upload failed %s", djiWaypointV2ErrorActions.description)
                    reportMission(-1, "Error. " + djiWaypointV2ErrorActions.errorCode)
                }
                else {
                    missionActionsAwaitingUpload = null
                    Timber.i("Action upload success")
                    reportMission(1, "Success")
                }
            }
        }
    }
    
    override fun submitLocationInit() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                override fun onComponentChange(key: ComponentKey?, oldComponent: BaseComponent?,
                                               newComponent: BaseComponent?) {
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
    
    public override fun onUsbAccessoryConnectedSync(usbAccessory: UsbAccessory) {
        if (!DJISDKManager.getInstance().hasSDKRegistered()) {
            return
        }
        val attachedIntent = Intent()
        attachedIntent.action = DJISDKManager.USB_ACCESSORY_ATTACHED
        getContext().sendBroadcast(attachedIntent)
    }
    
    public override fun onUsbAccessoryDisconnectedSync() {}
    
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
            }
            else {
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
        }
        else {
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
                ToastUtils.setResultToToast(String.format("getSerialNumber method - error: %s", djiError.description))
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
                }
                else {
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
    private fun onComponentChanged(key: ComponentKey?, oldComponent: BaseComponent?,
                                   newComponent: BaseComponent?) {
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
        aircraftInstance = mProduct
        Timber.i("aircraft connected %s", aircraftInstance!!.model.displayName)
        ToastUtils.setResultToToast(String.format("aircraft connected %s", aircraftInstance!!.model.displayName))
        if (aircraftInstance != null) {
            aircraftInstance!!.setDiagnosticsInformationCallback { list: List<DJIDiagnostics> ->
                synchronized(this) {
                    for (message in list) {
                        Timber.i("DJIDiagnostics: %s - %s: %s", message.code, message.reason, message.solution)
                    }
                }
            }
        }
        val intent = Intent()
        intent.action = BroadcastActions.ON_DRONE_CONNECTED.name
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent)
        val missionOperator = MissionControl.getInstance().waypointMissionOperator
        val missionOperator2 = MissionControl.getInstance().waypointMissionV2Operator
        if (aircraftInstance != null) {
            missionOperator.addListener(waypointMissionOperatorListener)
            if (isInV2SDKMode) {
                missionOperator2?.addActionListener(waypointV2ActionListener)
            }
        }
        else {
            missionOperator.removeListener(waypointMissionOperatorListener)
            missionOperator2?.removeActionListener(waypointV2ActionListener)
        }
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
        aircraftInstance = null
    }
    
    private fun deInitDroneUpdate() {
        Timber.i("tearDownKeyListeners... (ALL)")
    }
    
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
            val km = KeyManager.getInstance()
            if (km == null) {
                Timber.e("can't start simulator - KeyManager == null")
                return@submit
            }
            Timber.i("starting Simulator (%2.7f / %2.7f)...", BASE_LATITUDE, BASE_LONGITUDE)
            val simHome = LocationCoordinate2D(BASE_LATITUDE, BASE_LONGITUDE)
            val data = InitializationData.createInstance(simHome, REFRESH_FREQ, SATELLITE_COUNT)
            km.performAction(FlightControllerKey.create(FlightControllerKey.START_SIMULATOR), object : ActionCallback {
                override fun onSuccess() {
                    Timber.i("starting Simulator SUCCESS")
                    ToastUtils.setResultToToast("Simulator started")
                    val intent = Intent()
                    intent.action = BroadcastActions.ON_SIMULATOR_START.name
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent)
                }
                
                override fun onFailure(djiError: DJIError) {
                    Timber.e("starting Simulator ERROR - %s", djiError.description)
                }
            }, data)
        }
    }
    
    private val isInV2SDKMode: Boolean
        private get() {
            val m = aircraftInstance?.model
            return m == Model.MATRICE_300_RTK
        }
    
    private fun reportMission(descr: String) {
        reportMission(0, descr)
    }
    
    private fun reportMission(code: Int, descr: String) {
        val intent = Intent()
        intent.action = BroadcastActions.ON_MISSION_STATUS.name
        intent.putExtra("desc", descr)
        intent.putExtra("code", code)
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent)
    }
    
    private val demoMissionPoints: List<Waypoint>
        private get() {
            val wps: MutableList<Waypoint> = ArrayList()
            val altitude = 5f
            wps.add(Waypoint(56.8629537, 24.1120178, altitude))
            wps.add(Waypoint(56.8630303, 24.1128173, altitude))
            wps.add(Waypoint(56.8624685, 24.1145755, altitude))
            return wps
        }
    
    private fun generateWPV1demoMission(): WaypointMission {
        val builder = WaypointMission.Builder()
        builder.repeatTimes(1)
        builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY)
        builder.isExitMissionOnRCSignalLostEnabled = false
        builder.isGimbalPitchRotationEnabled = false
        builder.headingMode(WaypointMissionHeadingMode.AUTO)
        builder.finishedAction(WaypointMissionFinishedAction.NO_ACTION)
        //        builder.flightPathMode(WaypointMissionFlightPathMode.CURVED);
        builder.maxFlightSpeed(WaypointMission.MAX_FLIGHT_SPEED)
        builder.autoFlightSpeed(5f)
        for (point in demoMissionPoints) {
            builder.addWaypoint(point)
        }
        return builder.build()
    }
    
    private fun generateWPV2demoMission(): WaypointV2Mission {
        val builder = WaypointV2Mission.Builder()
        builder.repeatTimes = 1
        builder.gotoFirstWaypointMode = WaypointV2MissionTypes.MissionGotoWaypointMode.SAFELY
        builder.isExitMissionOnRCSignalLostEnabled = false
        builder.finishedAction = WaypointV2MissionTypes.MissionFinishedAction.NO_ACTION
        builder.maxFlightSpeed = WaypointMission.MAX_FLIGHT_SPEED
        builder.autoFlightSpeed = 5f
        for (wp in demoMissionPoints) {
            val wpBuilder = WaypointV2.Builder()
            wpBuilder.setCoordinate(wp.coordinate)
            wpBuilder.setAltitude(wp.altitude.toDouble())
            builder.addWaypoint(wpBuilder.build())
        }
        return builder.build()
    }
    
    private fun performAction(key: DJIKey) {
        val km = KeyManager.getInstance()
        if (km != null) {
            km.performAction(key, object : ActionCallback {
                override fun onSuccess() {
                    Timber.i("Perform %s SUCCESS", key.toString())
                }
                
                override fun onFailure(djiError: DJIError) {
                    Timber.w(
                        " Perform %s Error %d: %s", key.toString(),
                        djiError.errorCode, djiError.description
                            )
                }
            })
        }
        else {
            Timber.w(" Perform %s. No KeyManager instance", key.toString())
        }
    }
    
    override fun takeOff() {
        performAction(TAKEOFF_KEY_ACTION_KEY)
    }
    
    override fun land(useKeyInterface: Boolean) {
        if (useKeyInterface) {
            performAction(LAND_KEY_ACTION_KEY)
        }
        else {
            aircraftInstance!!.flightController.startLanding { djiError: DJIError? ->
                if (djiError != null) {
                    Timber.e(
                        " Landing error %d: %s", djiError.errorCode,
                        djiError.description
                            )
                }
                else {
                    ToastUtils.setResultToToast("Landing started")
                }
            }
        }
    }
    
    override fun uploadDemoMission() {
        if (!isInV2SDKMode) {
            val mission = generateWPV1demoMission()
            uploadMissionV1(mission)
        }
        else {
            val m = generateWPV2demoMission()
            uploadMissionV2(m)
        }
    }
    
    private fun uploadMissionV2(m: WaypointV2Mission, actions : List<WaypointV2Action>? = null) {
        val state = AtomicReference(
            Objects.requireNonNull(MissionControl.getInstance().waypointMissionV2Operator)?.currentState
                                   )
        Timber.i("Mission Upload - WaypointMissionOperator - loadMission... (state = %s)", state.get()?.name)
        val waypointV2MissionOperator = MissionControl.getInstance().waypointMissionV2Operator
        waypointV2MissionOperator!!.loadMission(m) { djiWaypointV2Error: DJIWaypointV2Error? ->
            state.set(waypointV2MissionOperator.currentState)
            if (djiWaypointV2Error != null) {
                reportMission(-1, "Error. " + djiWaypointV2Error.errorCode)
                Timber.e("Mission Upload - WaypointMissionOperator - loadMission FAILED (state = %s) as %s", state.get()?.name, djiWaypointV2Error.description)
            }
            else {
                Timber.i("Mission Upload - WaypointMissionOperator - uploadMission... (state = %s)", state.get()?.name)
                waypointV2MissionOperator.uploadMission { djiError: DJIWaypointV2Error? ->
                    if (djiError == null) {
                        reportMission(1, "Success")
                        if (actions != null && actions.isNotEmpty()) {
                            missionActionsAwaitingUpload = actions
                            uploadActionsV2(waypointV2MissionOperator.currentActionState)
                        }
                        Timber.i("Mission Upload - OK - error is null")
                    }
                    else {
                        Timber.e("Mission Upload - FAILED - djiError: %s", djiError.description)
                        reportMission(-1, "Error. " + djiError.errorCode)
                    }
                }
            }
        }
    }
    
    private fun uploadMissionV1(mission: WaypointMission) {
        var state = MissionControl.getInstance().waypointMissionOperator.currentState
        MissionControl.getInstance().waypointMissionOperator.clearMission()
    
        val waypointMissionOperator = MissionControl.getInstance().waypointMissionOperator
        aircraftInstance!!.flightController.getHomeLocation(object : CompletionCallbackWith<LocationCoordinate2D> {
            override fun onSuccess(locationCoordinate2D: LocationCoordinate2D) {
                Timber.i("Drone location %s", locationCoordinate2D.toString())
            }
        
            override fun onFailure(djiError: DJIError) {
                Timber.i("Drone location is unavailable due to %s", djiError.description)
            }
        })
        Timber.i("Mission Upload - Start (state = %s)", state.name)
        reportMission(0, "Started")
        val e = waypointMissionOperator.loadMission(mission)
        state = waypointMissionOperator.currentState
        if (e != null) {
            Timber.i("Mission Upload - WaypointMissionOperator - loadMission FAILED (state = %s) as %s", state.name, e.description)
            reportMission(-1, "Error. " + e.errorCode)
        }
        else {
            Timber.i("Mission Upload - WaypointMissionOperator - uploadMission... (state = %s)", state.name)
            waypointMissionOperator.uploadMission { djiError: DJIError? ->
                if (djiError == null) {
                    reportMission(1, "Success")
                    Timber.i("Mission Upload - OK - error is null")
                }
                else {
                    Timber.e("Mission Upload - FAILED - djiError: %s", djiError.description)
                    reportMission(-1, "Error. " + djiError.errorCode)
                }
            }
        }
    }
    
    override fun uploadMission(nativeRoute: File) {
        val bufferedReader: BufferedReader = nativeRoute.bufferedReader()
        val inputString = bufferedReader.use { it.readText() }
        if (isInV2SDKMode) {
            val data = gson.fromJson<RouteModelV2>(inputString, object : TypeToken<RouteModelV2?>() {}.type)
            data.mission?.let { uploadMissionV2(it, data.actions) }
        }
        else {
            val data = gson.fromJson<RouteModel>(inputString, object : TypeToken<RouteModel?>() {}.type)
            data.mission?.let { uploadMissionV1(it) }
        }
    
    }
    override fun startMission() {
        if (!isInV2SDKMode) {
            MissionControl.getInstance().waypointMissionOperator.startMission { djiError: DJIError? ->
                if (djiError == null) {
                    Timber.i("Mission Start - OK - error is null")
                    reportMission(2, "Success")
                }
                else {
                    Timber.e("Mission Start - FAILED - djiError: %s", djiError.description)
                    reportMission(-1, "Error. " + djiError.errorCode)
                }
            }
        }
        else {
            MissionControl.getInstance().waypointMissionV2Operator!!.startMission { djiError: DJIWaypointV2Error? ->
                if (djiError == null) {
                    Timber.i("Mission Start - OK - error is null")
                    reportMission(2, "Success")
                }
                else {
                    Timber.e("Mission Start - FAILED - djiError: %s", djiError.description)
                    reportMission(-1, "Error. " + djiError.errorCode)
                }
            }
        }
    }
    
    companion object {
        private const val BASE_LATITUDE = 56.8629537
        private const val BASE_LONGITUDE = 24.1120178
        private const val REFRESH_FREQ = 10
        private const val SATELLITE_COUNT = 18
        private val TAKEOFF_KEY_ACTION_KEY = FlightControllerKey.create(FlightControllerKey.TAKE_OFF)
        private val LAND_KEY_ACTION_KEY = FlightControllerKey.create(FlightControllerKey.START_LANDING)
        private val SHOOT_PHOTO_ACTION_KEY = CameraKey.create(CameraKey.START_SHOOT_PHOTO)
        private val SET_HOME_CURRENT_KEY = FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_USING_CURRENT_AIRCRAFT_LOCATION)
    }
    
    init {
        
        // this.simulatorUpdateCallbackAndController
        //         = new MySimulatorUpdateCallbackAndController(vehicleModelContainer, lbm);
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
}