package com.example.ugcssample.drone;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbAccessory;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.ugcssample.drone.mission.DjiMissionAndHome;
import com.example.ugcssample.drone.mission.DjiMissionSpecificUtils;
import com.example.ugcssample.model.Mission;
import com.example.ugcssample.utils.PermissionCheckResult;
import com.example.ugcssample.utils.PtMissionUtils;
import com.example.ugcssample.utils.ToastUtils;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.simulator.InitializationData;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.keysdk.ProductKey;
import dji.keysdk.callback.KeyListener;
import dji.log.DJILog;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.base.DJIDiagnostics;
import dji.sdk.basestation.BaseStation;
import dji.sdk.flightcontroller.FlightAssistant;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import timber.log.Timber;

public class DroneBridgeImpl extends DroneBridgeBase implements DroneBridge, MyMissionGlobalEventListener.MainController {

    private static final double BASE_LATITUDE = 22;
    private static final double BASE_LONGITUDE = 113;
    private static final int REFRESH_FREQ = 10;
    private static final int SATELLITE_COUNT = 10;

    public static final String ON_DRONE_CONNECTED = "ON_DRONE_CONNECTED";
    public static final String ON_MISSION_UPLOADED = "ON_MISSION_UPLOADED";

    private final FlightControllerKey serialNumberKey = FlightControllerKey.create(FlightControllerKey.SERIAL_NUMBER);
    private final ProductKey connectionKey = ProductKey.create(ProductKey.CONNECTION);

    private final KeyListener connectionKeyListener = (oldValue, newValue) -> onConnectionChanged(newValue);
    private PermissionCheckResult permissionCheckResult;
    private MyFlightControllerUpdateCallback flightControllerUpdateCallback;
    private MyMissionGlobalEventListener myMissionGlobalEventListener;
    private Aircraft aircraft = null;
    private BaseStation baseStation = null;

    private final LocationManager locationManager;
    //private final MySimulatorUpdateCallbackAndController simulatorUpdateCallbackAndController;

    private ScheduledFuture<?> droneInitFuture;
    private Context context;


    public DroneBridgeImpl(@NonNull Context mContext) {
        super(mContext);
        this.context = mContext;

       // this.simulatorUpdateCallbackAndController
       //         = new MySimulatorUpdateCallbackAndController(vehicleModelContainer, lbm);

        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        myMissionGlobalEventListener = new MyMissionGlobalEventListener(this);
        flightControllerUpdateCallback = new MyFlightControllerUpdateCallback(lbm);
    }

    @Override
    public void submitLocationInit() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
    }

    @Override
    public void submitSdkInit() {

        WORKER.submit(() -> {
            Timber.i("DJISDKManager -> registerApp...");
            ToastUtils.setResultToToast("DJISDKManager -> registerApp..");
            Timber.i("DJISDKManager -> SDK VERSION = %s", DJISDKManager.getInstance().getSDKVersion());
            if (DJISDKManager.getInstance().hasSDKRegistered()) {
                //ToastUtils.setResultToToast("DJISDKManager -> already registered");
                //Timber.i("DJISDKManager hasSDKRegistered.");
                //return;
            }
            permissionCheckResult = null;
            DJISDKManager.getInstance().registerApp(context, new DJISDKManager.SDKManagerCallback() {
                @Override
                public void onRegister(DJIError djiError) {
                    DroneBridgeImpl.this.onRegister(djiError);
                }

                @Override
                public void onProductDisconnect() {
                    DroneBridgeImpl.this.onProductDisconnect();
                }

                @Override
                public void onProductConnect(BaseProduct baseProduct) {
                    DroneBridgeImpl.this.onProductConnect(baseProduct);
                }

                @Override
                public void onProductChanged(BaseProduct baseProduct) {

                }

                @Override
                public void onComponentChange(BaseProduct.ComponentKey key, BaseComponent oldComponent,
                                              BaseComponent newComponent) {
                    onComponentChanged(key, oldComponent, newComponent);
                }

                @Override
                public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {

                }

                @Override
                public void onDatabaseDownloadProgress(long currentSize, long totalSize) {
                }

            });
        });
    }

    @Override
    public void onUsbAccessoryAttached() {
        Timber.i("onUsbAccessoryAttached");
        usbAccessoryManager.check();
    }

    @Override
    void onUsbAccessoryConnectedSync(UsbAccessory usbAccessory) {
        if (!DJISDKManager.getInstance().hasSDKRegistered()) {
            return;
        }

        Intent attachedIntent = new Intent();
        attachedIntent.setAction(DJISDKManager.USB_ACCESSORY_ATTACHED);
        getContext().sendBroadcast(attachedIntent);

    }

    @Override
    void onUsbAccessoryDisconnectedSync() {

    }

    /**
     * Callback method after the application attempts to register.
     * <p>
     * in method initSdkManager (see above) we are passing sdkManagerCallback
     */
    private void onRegister(final DJIError registrationResult) {
        WORKER.submit(() -> {
            PermissionCheckResult pc = new PermissionCheckResult();
            boolean startConnect = false;
            if (registrationResult == null || registrationResult == DJISDKError.REGISTRATION_SUCCESS) {
                ToastUtils.setResultToToast("DJISDKManager -> onRegister OK");
                Timber.i("DJISDKManager -> onRegister OK");
                pc.permissionCheckResult = DJISDKError.REGISTRATION_SUCCESS.toString();
                pc.permissionCheckResultDesc = DJISDKError.REGISTRATION_SUCCESS.getDescription();
                startConnect = true;
            } else {
                pc.permissionCheckResult = registrationResult.toString();
                pc.permissionCheckResultDesc = registrationResult.getDescription();
                ToastUtils.setResultToToast(String.format("DJISDKManager -> onRegister ERROR = %s (%s)",
                        pc.permissionCheckResult, pc.permissionCheckResultDesc));
                Timber.w("DJISDKManager -> onRegister ERROR = %s (%s)",
                        pc.permissionCheckResult, pc.permissionCheckResultDesc);
            }
            permissionCheckResult = pc;
            if (startConnect) {
                startConnectionToProduct();
            }
        });
        if (registrationResult == null) {
            Timber.i("DJI SDK is initialised");
        } else {
            Timber.e("DJI SDK initialisation failure. Error: %s", registrationResult.getDescription());
        }
    }

    private void onProductDisconnect() {
        Timber.d("onProductDisconnect");
    }

    private void onProductConnect(BaseProduct baseProduct) {
        Aircraft af = (Aircraft)baseProduct;
        if (af.getFlightController() == null) {
            ToastUtils.setResultToToast("Unable to get flight controller");
        } else {
            af.getFlightController().getSerialNumber(new CommonCallbacks.CompletionCallbackWith<String>() {
                @Override
                public void onSuccess(String s) {
                    if (droneInitFuture != null) {
                        droneInitFuture.cancel(false);
                        droneInitFuture = null;
                    }
                    droneInitFuture = WORKER.schedule(() ->
                            initDrone(s), 2, TimeUnit.SECONDS);
                }

                @Override
                public void onFailure(DJIError djiError) {
                    ToastUtils.setResultToToast(String.format("getSerialNumber method - error: %s", djiError.getDescription()));
                    Timber.i("getSerialNumber method - error: %s", djiError.getDescription());
                }
            });
        }
        Timber.d("onProductConnect");
    }

    /**
     * Starts a connection between the SDK and the DJI product.
     * This method should be called after successful registration of the app and once there is a data connection
     * between the mobile device and DJI product.
     */
    private void startConnectionToProduct() {
        WORKER.submit(() -> {
            ToastUtils.setResultToToast("DJISDKManager -> startConnectionToProduct...");
            Timber.i("DJISDKManager -> startConnectionToProduct...");
            KeyManager km = KeyManager.getInstance();
            km.addListener(connectionKey, connectionKeyListener);
            DJISDKManager.getInstance().startConnectionToProduct();
        });
    }

    private void onConnectionChanged(Object newValue) {
        if (newValue instanceof Boolean) {
            final boolean connection = (Boolean) newValue;
            WORKER.submit(() -> {
                if (connection) {
                    Timber.i("KeyManager -> onConnectionChanged = true");
                    // We will not do anything, we will wait for serial number update.
                } else {
                    Timber.w("KeyManager -> onConnectionChanged = false");
                    disconnectDrone();
                }
            });
        }
    }

    private void onSerialNumberChanged(Object newValue) {
        if (newValue instanceof String) {
            final String sn = (String) newValue;
            WORKER.submit(() -> {
                Timber.i("KeyManager -> onSerialNumberChanged = %s", sn);
                if (droneInitFuture != null) {
                    droneInitFuture.cancel(false);
                    droneInitFuture = null;
                }
                droneInitFuture = WORKER.schedule(() ->
                            initDrone(sn), 2, TimeUnit.SECONDS);
            });
        }
    }

    private void onComponentChanged(final BaseProduct.ComponentKey key, final BaseComponent oldComponent,
                                    final BaseComponent newComponent) {
        WORKER.submit(() -> {
            if (key == null) {
                return;
            }

            Timber.i("DJISDKManager -> onComponentChanged %s (%s to %s)",
                    key.toString(),
                    oldComponent == null ? "NULL" : "CONNECTED",
                    newComponent == null ? "NULL" : "CONNECTED");

            switch (key) {
                case BATTERY:
                    //reinit battery if null was before
                    break;
                case BASE_STATION:
                    break;
                case CAMERA:
                default:
                    break;
            }
        });
    }

    private void initDrone(String serial) {
        droneInitFuture = null;
        Timber.i("OK, starting drone init...");

        BaseProduct mProduct = DJISDKManager.getInstance().getProduct();
        if (!(mProduct instanceof Aircraft)) {
            return;
        }
        aircraft = (Aircraft)mProduct;
        Timber.i("aircraft connected %s", aircraft.getModel().getDisplayName());
        ToastUtils.setResultToToast(String.format("aircraft connected %s", aircraft.getModel().getDisplayName()));

        if (aircraft != null) {
            aircraft.setDiagnosticsInformationCallback(list -> {
                synchronized (this) {
                    for (DJIDiagnostics message:
                    list) {
                        Timber.i("DJIDiagnostics: %s - %s: %s", message.getCode(), message.getReason(), message.getSolution());
                    }
                }
            });
        };
        Intent intent = new Intent();
        intent.setAction(DroneBridgeImpl.ON_DRONE_CONNECTED);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        flightControllerUpdateCallback.setUpKeyListeners();
    }
    private void disconnectDrone() {
        // Drone is disconnected
        // 1. cancel initialization future if it is scheduled
        if (droneInitFuture != null) {
            droneInitFuture.cancel(false);
            droneInitFuture = null;
        }

        BaseProduct mProduct = DJISDKManager.getInstance().getProduct();
        Timber.i("aircraft disconnected");
        Timber.d("Drone connection lost, in DJISDKManager BaseProduct %s",
                mProduct == null ? "NULL" : "EXISTS");
        aircraft = null;
    }

    private void deInitDroneUpdate() {
        Timber.i("tearDownKeyListeners... (ALL)");
    }

    @Override
    public PermissionCheckResult getPermissionCheckResult() {
        return permissionCheckResult;
    }

    /**
     * Gets instance of the specific product connected after the
     * API KEY is successfully validated. Please make sure the
     * API_KEY has been added in the Manifest
     */
    @Override
    public Aircraft getAircraftInstance() {
        return aircraft;
    }

    @Override
    public void onDestroy() {
        deInitDroneUpdate();
        tearDownKeyListeners();
        DJISDKManager mng = DJISDKManager.getInstance();
        if (mng != null) {
            try {
                mng.stopConnectionToProduct();
                mng.destroy();
            } catch (Exception e) {
                Timber.i("DJISDKManager.getInstance().stopConnectionToProduct -> NullPointer again");
            }
        }
    }

    private void tearDownKeyListeners() {
        KeyManager km = KeyManager.getInstance();
        if (km != null) {
            km.removeListener(connectionKeyListener);
        }
    }

    @Override
    public void startModelSimulator() {
        WORKER.submit(() -> {
            aircraft.getFlightController().getSimulator().start(InitializationData.createInstance(new LocationCoordinate2D(BASE_LATITUDE, BASE_LONGITUDE),REFRESH_FREQ, SATELLITE_COUNT), new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    String result = djiError == null ? "Simulator started" : djiError.getDescription();
                    Timber.i(result);
                    ToastUtils.setResultToToast(result);
                }
            });
        });
    }

    @Override
    public void uploadMission() {
        if (flightControllerUpdateCallback.latitude == null
                || flightControllerUpdateCallback.longitude == null
                || flightControllerUpdateCallback.altitude == null) {
            String result =  "Wrong coordinates";
            Timber.i(result);
            ToastUtils.setResultToToast(result);
            return;
        }
        Mission m = PtMissionUtils.ascentMission(flightControllerUpdateCallback.latitude , flightControllerUpdateCallback.longitude, 0, 1.2192);
        final DjiMissionAndHome gsTask =
                DjiMissionSpecificUtils.generateDjiTaskForMission(new MsgIdAndSession(), m, true);

        WaypointMission waypointMission = gsTask.waypointMission;
        WaypointMissionOperator waypointMissionOperator =
                MissionControl.getInstance().getWaypointMissionOperator();
        DJIError e = waypointMissionOperator.loadMission(waypointMission);
        // e = null;
        WaypointMissionState state = waypointMissionOperator.getCurrentState();
        if (e != null) {
            String result = String.format("Mission Upload - WaypointMissionOperator - loadMission FAILED" +
                            " (state = %s) as %s",
                    state.getName(), e.getDescription());
            Timber.i(result);
            ToastUtils.setResultToToast(result);
        } else {
            String result =  String.format("Mission Upload - WaypointMissionOperator - uploadMission... " +
                            "(state = %s)",
                    state.getName());
            Timber.i(result);
            ToastUtils.setResultToToast(result);
            waypointMissionOperator.removeListener(myMissionGlobalEventListener);
            waypointMissionOperator.addListener(myMissionGlobalEventListener);
            waypointMissionOperator.uploadMission(myMissionGlobalEventListener);
        }
    }

    @Override
    public void startMission() {
        WaypointMissionOperator waypointMissionOperator =
                MissionControl.getInstance().getWaypointMissionOperator();
        waypointMissionOperator.startMission(djiError -> {
            if (djiError != null) {
                String result =  djiError.getDescription();
                Timber.i(result);
                ToastUtils.setResultToToast(result);
            } else {
                String result =  "Mission started. Good luck!";
                Timber.i(result);
                ToastUtils.setResultToToast(result);
            }
        });
    }

    @Override
    public void onMissionUploadResult(DJIError djiError) {
        if (djiError != null) {
            String result =  djiError.getDescription();
            Timber.i(result);
            ToastUtils.setResultToToast(result);
        } else {
            String result =  "Success mission upload";
            Timber.i(result);
            ToastUtils.setResultToToast(result);
            Intent intent = new Intent();
            intent.setAction(DroneBridgeImpl.ON_MISSION_UPLOADED);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        }
    }

    @Override
    public void onMissionUploadUpdate(WaypointMissionUploadEvent waypointMissionUploadEvent) {

    }

    @Override
    public void onMissionExecutionStart() {

    }

    @Override
    public void onMissionWpReached(int reachedWpIndex) {

    }

    @Override
    public void onMissionWpMovingTo(int movingToWpIndex) {

    }

    @Override
    public void onMissionExecutionFinish() {

    }
}
