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

import com.example.ugcssample.utils.PermissionCheckResult;
import com.example.ugcssample.utils.ToastUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.simulator.InitializationData;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointExecutionProgress;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecuteState;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.mission.waypoint.WaypointUploadProgress;
import dji.common.mission.waypointv2.WaypointV2;
import dji.common.mission.waypointv2.WaypointV2Mission;
import dji.common.mission.waypointv2.WaypointV2MissionState;
import dji.common.mission.waypointv2.WaypointV2MissionTypes;
import dji.common.model.LocationCoordinate2D;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.keysdk.ProductKey;
import dji.keysdk.callback.ActionCallback;
import dji.keysdk.callback.KeyListener;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.base.DJIDiagnostics;
import dji.sdk.basestation.BaseStation;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.mission.waypoint.WaypointV2MissionOperator;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import timber.log.Timber;

import static dji.common.product.Model.MATRICE_300_RTK;

public class DroneBridgeImpl extends DroneBridgeBase implements DroneBridge {

    private static final double BASE_LATITUDE = 56.8629537;
    private static final double BASE_LONGITUDE = 24.1120178;
    private static final int REFRESH_FREQ = 10;
    private static final int SATELLITE_COUNT = 18;
    private static final FlightControllerKey TAKEOFF_KEY_ACTION_KEY = FlightControllerKey.create(FlightControllerKey.TAKE_OFF);
    private static final FlightControllerKey LAND_KEY_ACTION_KEY = FlightControllerKey.create(FlightControllerKey.START_LANDING);
    private static final CameraKey SHOOT_PHOTO_ACTION_KEY = CameraKey.create(CameraKey.START_SHOOT_PHOTO);
    private static final FlightControllerKey SET_HOME_CURRENT_KEY = FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_USING_CURRENT_AIRCRAFT_LOCATION);
    public static enum BroadcastActions {
        ON_DRONE_CONNECTED, ON_MISSION_STATUS, ON_SIMULATOR_START
    }

    private final FlightControllerKey serialNumberKey = FlightControllerKey.create(FlightControllerKey.SERIAL_NUMBER);
    private final ProductKey connectionKey = ProductKey.create(ProductKey.CONNECTION);

    private final KeyListener connectionKeyListener = (oldValue, newValue) -> onConnectionChanged(newValue);
    private PermissionCheckResult permissionCheckResult;
    private Aircraft aircraft = null;
    private BaseStation baseStation = null;

    private final LocationManager locationManager;
    //private final MySimulatorUpdateCallbackAndController simulatorUpdateCallbackAndController;

    private ScheduledFuture<?> droneInitFuture;
    private Context context;
    private WaypointMissionOperatorListener waypointMissionOperatorListener =  new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(@NonNull @NotNull WaypointMissionDownloadEvent waypointMissionDownloadEvent) {

        }

        @Override
        public void onUploadUpdate(@NonNull @NotNull WaypointMissionUploadEvent waypointMissionState) {

            WaypointUploadProgress up = waypointMissionState.getProgress();
            WaypointMissionState currentState = waypointMissionState.getCurrentState();
            WaypointMissionState previousState = waypointMissionState.getPreviousState();

            if (currentState == WaypointMissionState.UPLOADING) {
                if (up != null) {
                    Integer index = up.uploadedWaypointIndex + 1;
                    int size = up.totalWaypointCount;
                    float progress = ((float) index) / size;
                    Timber.i("MissionUploadProgress = %3.3f (%d/%d)", progress, index, size);
                    reportMission(1, String.format("WP upload %3.0f%%", progress));

                }
            } else if(currentState == WaypointMissionState.READY_TO_EXECUTE) {
                reportMission(1,"WP upload 100%");
            }
        }

        @Override
        public void onExecutionUpdate(@NonNull @NotNull WaypointMissionExecutionEvent waypointMissionExecutionEvent) {
            WaypointExecutionProgress progress = waypointMissionExecutionEvent.getProgress();

            int tp = progress.targetWaypointIndex;
            boolean reached = progress.isWaypointReached;
            WaypointMissionExecuteState state = progress.executeState;
            int total = progress.totalWaypointCount;
            reportMission(3, String.format("WP running. Completed  %d / %d", tp, total));
        }

        @Override
        public void onExecutionStart() {
            reportMission(3,"WP - Exec start");
        }

        @Override
        public void onExecutionFinish(@Nullable @org.jetbrains.annotations.Nullable DJIError djiError) {
            reportMission(3,"WP - Exec Finish");
        }
    };

    public DroneBridgeImpl(@NonNull Context mContext) {
        super(mContext);
        this.context = mContext;

       // this.simulatorUpdateCallbackAndController
       //         = new MySimulatorUpdateCallbackAndController(vehicleModelContainer, lbm);

        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
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

    // ProductKey.COMPONENT_KEY - Not works in SDK 4.3.2, so this method is invoked vis baseProductListener
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
        intent.setAction(BroadcastActions.ON_DRONE_CONNECTED.name());
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        WaypointMissionOperator missionOperator = MissionControl.getInstance().getWaypointMissionOperator();
        if (aircraft != null) {
            missionOperator.addListener(waypointMissionOperatorListener);
        } else {
            missionOperator.removeListener(waypointMissionOperatorListener);
        }
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
            KeyManager km = KeyManager.getInstance();
            if (km == null) {
                Timber.e("can't start simulator - KeyManager == null");
                return;
            }
            Timber.i("starting Simulator (%2.7f / %2.7f)...", BASE_LATITUDE, BASE_LONGITUDE);
            LocationCoordinate2D simHome = new LocationCoordinate2D(BASE_LATITUDE, BASE_LONGITUDE);
            InitializationData data = InitializationData.createInstance(simHome, REFRESH_FREQ, SATELLITE_COUNT);
            km.performAction(FlightControllerKey.create(FlightControllerKey.START_SIMULATOR), new ActionCallback() {
                @Override
                public void onSuccess() {
                    Timber.i("starting Simulator SUCCESS");
                    ToastUtils.setResultToToast("Simulator started");
                    Intent intent = new Intent();
                    intent.setAction(BroadcastActions.ON_SIMULATOR_START.name());
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
                }
                @Override
                public void onFailure(@NonNull DJIError djiError) {
                    Timber.e("starting Simulator ERROR - %s", djiError.getDescription());

                }
            }, data);
            /*aircraft.getFlightController()
                    .getSimulator()
                    .start(InitializationData.createInstance(simHome,REFRESH_FREQ, SATELLITE_COUNT),
                        djiError -> {
                            String result = djiError == null ? "Simulator started" : djiError.getDescription();
                            Timber.i(result);
                            ToastUtils.setResultToToast(result);
                        });*/
        });
    }
    private boolean isInV2SDKMode() {
        Model m = aircraft.getModel();
        return m == MATRICE_300_RTK;

    }
    private void reportMission(String descr) {
        reportMission(0, descr);
    }
    private void reportMission(int code, String descr) {
        Intent intent = new Intent();
        intent.setAction(BroadcastActions.ON_MISSION_STATUS.name());
        intent.putExtra("desc", descr);
        intent.putExtra("code", code);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }
    private List<Waypoint> getDemoMissionPoints() {
        List<Waypoint> wps = new ArrayList<>();
        float altitude = 5;
        wps.add(new Waypoint(56.8629537, 24.1120178, altitude));
        wps.add(new Waypoint(56.8630303, 24.1128173, altitude));
        wps.add(new Waypoint(56.8624685, 24.1145755, altitude));
        return wps;
    }
    private WaypointMission generateWPV1demoMission() {
        WaypointMission.Builder builder = new WaypointMission.Builder();
        builder.repeatTimes(1);
        builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
        builder.setExitMissionOnRCSignalLostEnabled(false);
        builder.setGimbalPitchRotationEnabled(false);
        builder.headingMode(WaypointMissionHeadingMode.AUTO);
        builder.finishedAction(WaypointMissionFinishedAction.NO_ACTION);
//        builder.flightPathMode(WaypointMissionFlightPathMode.CURVED);
        builder.maxFlightSpeed(WaypointMission.MAX_FLIGHT_SPEED);
        builder.autoFlightSpeed(5f);
        for (Waypoint point: getDemoMissionPoints()) {
            builder.addWaypoint(point);
        }

        return builder.build();

    }
    private WaypointV2Mission generateWPV2demoMission() {
        WaypointV2Mission.Builder builder = new WaypointV2Mission.Builder();

        builder.setRepeatTimes(1);
        builder.setGotoFirstWaypointMode(WaypointV2MissionTypes.MissionGotoWaypointMode.SAFELY);
        builder.setExitMissionOnRCSignalLostEnabled(false);
        builder.setFinishedAction(WaypointV2MissionTypes.MissionFinishedAction.NO_ACTION);
        builder.setMaxFlightSpeed(WaypointMission.MAX_FLIGHT_SPEED);
        builder.setAutoFlightSpeed(5f);


        for (Waypoint wp : getDemoMissionPoints()) {
            WaypointV2.Builder wpBuilder = new WaypointV2.Builder();
            wpBuilder.setCoordinate(wp.coordinate);
            wpBuilder.setAltitude(wp.altitude);
            builder.addWaypoint(wpBuilder.build());
        }

        return builder.build();

    }
    private void performAction(final DJIKey key) {
        KeyManager km = KeyManager.getInstance();
        if (km != null) {
            km.performAction(key, new ActionCallback() {
                @Override
                public void onSuccess() {
                    Timber.i("Perform %s SUCCESS", key.toString());
                }

                @Override
                public void onFailure(@NonNull DJIError djiError) {
                    Timber.w(" Perform %s Error %d: %s", key.toString(),
                            djiError.getErrorCode(), djiError.getDescription());

                }
            });
        } else {
            Timber.w(" Perform %s. No KeyManager instance", key.toString());
        }
    }
    @Override
    public void takeOff() {
        performAction(TAKEOFF_KEY_ACTION_KEY);
    }

    @Override
    public void land() {
        performAction(LAND_KEY_ACTION_KEY);
    }
    @Override
    public void uploadDemoMission() {
        if (!isInV2SDKMode()) {
            WaypointMissionState state = MissionControl.getInstance().getWaypointMissionOperator().getCurrentState();
            MissionControl.getInstance().getWaypointMissionOperator().clearMission();
            DJIError e;
            WaypointMissionOperator waypointMissionOperator = MissionControl.getInstance().getWaypointMissionOperator();
            WaypointMission mission = generateWPV1demoMission();
            aircraft.getFlightController().getHomeLocation(new CommonCallbacks.CompletionCallbackWith<LocationCoordinate2D>() {
                @Override
                public void onSuccess(LocationCoordinate2D locationCoordinate2D) {
                    Timber.i("Drone location %s", locationCoordinate2D.toString());
                }

                @Override
                public void onFailure(DJIError djiError) {
                    Timber.i("Drone location is unavailable due to %s", djiError.getDescription());
                }
            });

            Timber.i("Mission Upload - Start (state = %s)", state.getName());
            reportMission(0, "Started");
            e = waypointMissionOperator.loadMission(mission);
            state = waypointMissionOperator.getCurrentState();

            if (e != null) {
                Timber.i("Mission Upload - WaypointMissionOperator - loadMission FAILED (state = %s) as %s", state.getName(), e.getDescription());
                reportMission(-1,"Error. "+e.getErrorCode());
            } else {
                Timber.i("Mission Upload - WaypointMissionOperator - uploadMission... (state = %s)", state.getName());
                waypointMissionOperator.uploadMission(djiError -> {
                    if (djiError == null) {
                        reportMission(1, "Success");
                        Timber.i("Mission Upload - OK - error is null");
                    } else {
                        Timber.e("Mission Upload - FAILED - djiError: %s", djiError.getDescription());
                        reportMission(-1, "Error. "+djiError.getErrorCode());
                    }
                });
            }
        } else {
            AtomicReference<WaypointV2MissionState> state = new AtomicReference<>(Objects.requireNonNull(MissionControl.getInstance().getWaypointMissionV2Operator()).getCurrentState());
            Timber.i("Mission Upload - WaypointMissionOperator - loadMission... (state = %s)", state.get().name());
            WaypointV2Mission m = generateWPV2demoMission();
            WaypointV2MissionOperator waypointV2MissionOperator = MissionControl.getInstance().getWaypointMissionV2Operator();
            waypointV2MissionOperator.loadMission(m, djiWaypointV2Error -> {
                state.set(waypointV2MissionOperator.getCurrentState());
                if (djiWaypointV2Error != null) {
                    reportMission(-1, "Error. "+djiWaypointV2Error.getErrorCode());
                    Timber.e("Mission Upload - WaypointMissionOperator - loadMission FAILED (state = %s) as %s", state.get().name(), djiWaypointV2Error.getDescription());
                } else {
                    Timber.i("Mission Upload - WaypointMissionOperator - uploadMission... (state = %s)", state.get().name());
                    waypointV2MissionOperator.uploadMission(djiError -> {
                        if (djiError == null) {
                            reportMission(1, "Success");
                            Timber.i("Mission Upload - OK - error is null");
                        } else {
                            Timber.e("Mission Upload - FAILED - djiError: %s", djiError.getDescription());
                            reportMission(-1, "Error. "+djiError.getErrorCode());
                        }
                    });
                }

            });
        }
    }

    @Override
    public void startMission() {
        MissionControl.getInstance().getWaypointMissionOperator().startMission(djiError -> {
            if (djiError == null) {
                Timber.i("Mission Start - OK - error is null");
                reportMission(2, "Success");
            } else {
                Timber.e("Mission Start - FAILED - djiError: %s", djiError.getDescription());
                reportMission(-1, "Error. "+djiError.getErrorCode());
            }
        });
    }
}
