package com.example.ugcssample.drone;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbAccessory;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.ugcssample.utils.PermissionCheckResult;
import com.example.ugcssample.utils.ToastUtils;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import dji.common.camera.CameraVideoStreamSource;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.simulator.InitializationData;
import dji.common.model.LocationCoordinate2D;
import dji.common.remotecontroller.HardwareState;
import dji.common.remotecontroller.ProfessionalRC;
import dji.common.util.CommonCallbacks;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.keysdk.ProductKey;
import dji.keysdk.callback.KeyListener;
import dji.keysdk.callback.SetCallback;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.base.DJIDiagnostics;
import dji.sdk.basestation.BaseStation;
import dji.sdk.camera.Camera;
import dji.sdk.camera.Lens;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.products.Aircraft;
import dji.sdk.remotecontroller.RemoteController;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import timber.log.Timber;

public class DroneBridgeImpl extends DroneBridgeBase implements DroneBridge {

    private static final double BASE_LATITUDE = 22;
    private static final double BASE_LONGITUDE = 113;
    private static final int REFRESH_FREQ = 10;
    private static final int SATELLITE_COUNT = 10;

    public static final String ON_DRONE_CONNECTED = "ON_DRONE_CONNECTED";

    private final FlightControllerKey serialNumberKey = FlightControllerKey.create(FlightControllerKey.SERIAL_NUMBER);
    private final ProductKey connectionKey = ProductKey.create(ProductKey.CONNECTION);
    static final DJIKey CUSTOM_INFORMATION_KEY = CameraKey.create(CameraKey.CUSTOM_INFORMATION);

    private final KeyListener connectionKeyListener = (oldValue, newValue) -> onConnectionChanged(newValue);
    private PermissionCheckResult permissionCheckResult;
    private Aircraft aircraft = null;
    private BaseStation baseStation = null;

    private final LocationManager locationManager;
    //private final MySimulatorUpdateCallbackAndController simulatorUpdateCallbackAndController;

    private ScheduledFuture<?> droneInitFuture;
    private Context context;
    private SharedPreferences prefs;
    private List<Camera> cameraList;
    private GimbalController gimbalController;


    public DroneBridgeImpl(@NonNull Context mContext) {
        super(mContext);
        this.context = mContext;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        // this.simulatorUpdateCallbackAndController
        //         = new MySimulatorUpdateCallbackAndController(vehicleModelContainer, lbm);

        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        this.gimbalController = new GimbalControllerDjiImpl(this, new Handler(Looper
                .getMainLooper()));
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
                //ToastUtils.setResultToToast("DJISDKManager -> `already registered");
              //  Timber.i("DJISDKManager hasSDKRegistered.");
                initDrone(null);
                return;
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
        Aircraft af = (Aircraft) baseProduct;
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
        aircraft = (Aircraft) mProduct;
        Timber.i("aircraft connected %s", aircraft.getModel().getDisplayName());
        ToastUtils.setResultToToast(String.format("aircraft connected %s", aircraft.getModel().getDisplayName()));

        if (aircraft != null) {
            aircraft.setDiagnosticsInformationCallback(list -> {
                synchronized (this) {
                    for (DJIDiagnostics message :
                            list) {
                        Timber.i("DJIDiagnostics: %s - %s: %s", message.getCode(), message.getReason(), message.getSolution());
                    }
                }
            });
            cameraList = aircraft.getCameras();
        }
        Intent intent = new Intent();
        intent.setAction(DroneBridgeImpl.ON_DRONE_CONNECTED);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
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

    @Override
    public void remoteController() {

        WORKER.submit(() -> {
            Gimbal gimbal = aircraft.getGimbal();
            if (cameraList.size() == 0) {
                Timber.i("cameraList is null");
                ToastUtils.setResultToToast("cameraList is null");
                return;
            }
            if (gimbal == null) {
                Timber.i("gimbal is null");
                ToastUtils.setResultToToast("gimbal is null");
                return;
            }
            Camera camera = cameraList.get(0);
            camera.setCameraVideoStreamSource(CameraVideoStreamSource.ZOOM, djiError -> {
                if (djiError != null) {
                    Timber.i(djiError.getDescription());
                }
            });
            Lens activeLens = null;
            if (camera.isMultiLensCameraSupported()) {
                List<Lens> lensesList = camera.getLenses();
                if (lensesList != null) {
                    for (Lens lens :
                            lensesList) {
                        if (lens.isHybridZoomSupported()) {
                            activeLens = lens;
                            break;
                        }
                    }
                } else {
                    Timber.i("Lenses list is null");
                }
            } else {

                Timber.i("not M300");
                ToastUtils.setResultToToast("not M300");
                //Lens singleLens = new DjiLens(djiCamera, context);
                //lenses.put(singleLens.getId(), singleLens);
                //singleLens.addOnInitialisedListener(lensInitialisationListener);
            }
            if (activeLens == null) {
                Timber.i("no zoom lens support");
                ToastUtils.setResultToToast("no zoom lens support");
                return;
            }
            RemoteController remoteController = aircraft.getRemoteController();

            Lens finalActiveLens = activeLens;
            remoteController.setHardwareStateCallback(rcHardwareState -> {
                HardwareState.Button btnC1 = rcHardwareState.getC1Button();
                HardwareState.Button btnC2 = rcHardwareState.getC2Button();
                HardwareState.FiveDButton btn5D = rcHardwareState.getFiveDButton();
                HardwareState.FiveDButtonDirection horizontalDirection = btn5D.getHorizontalDirection();
                HardwareState.FiveDButtonDirection verticalDirection = btn5D.getVerticalDirection();
                // ToastUtils.setResultToToast("C1 " + btnC1.isClicked() + " horizontalDirection " + horizontalDirection.toString());
                String list_preference_C1 = prefs.getString("list_preference_C1", "");
                String list_preference_C2 = prefs.getString("list_preference_C2", "");
                String list_preference_C1_C2_release = prefs.getString("list_preference_C1_C2_release", "");
                String list_preference_5d_left = prefs.getString("list_preference_5d_left", "");
                String list_preference_5d_right = prefs.getString("list_preference_5d_right", "");
                String list_preference_5d_up = prefs.getString("list_preference_5d_up", "");
                String list_preference_5d_down = prefs.getString("list_preference_5d_down", "");
                String list_preference_5d_middle = prefs.getString("list_preference_5d_middle", "");
                String list_preference_5d_push = prefs.getString("list_preference_5d_push", "");
                Timber.i("%s %s %s %s %s %s %s %s %s",
                        list_preference_C1,
                        list_preference_C2,
                        list_preference_C1_C2_release,
                        list_preference_5d_left,
                        list_preference_5d_right,
                        list_preference_5d_up,
                        list_preference_5d_down,
                        list_preference_5d_middle,
                        list_preference_5d_push
                        );
                // <item>ZoomInContinue</item>
                // <item>ZoomOutContinue</item>
                //  <item>ZoomStop</item>
                //  <item>CamUp</item>
                //  <item>CamDown</item>
                //  <item>CamLeft</item>
                //  <item>CamRight</item>
                // <item>CamStop</item>
                if (btnC1 != null && btnC1.isClicked()) {
                    executeAction(list_preference_C1, finalActiveLens);
                }
                if (btnC2 != null && btnC2.isClicked()) {
                    executeAction(list_preference_C2, finalActiveLens);
                }
                if (btnC2 != null && !btnC2.isClicked() && btnC1 != null && !btnC1.isClicked()) {
                    executeAction(list_preference_C1_C2_release, finalActiveLens);
                }
                if (horizontalDirection == HardwareState.FiveDButtonDirection.NEGATIVE) {
                    executeAction(list_preference_5d_left, finalActiveLens);
                }
                if (horizontalDirection == HardwareState.FiveDButtonDirection.POSITIVE) {
                    executeAction(list_preference_5d_right, finalActiveLens);
                }
                if (verticalDirection == HardwareState.FiveDButtonDirection.NEGATIVE) {
                    executeAction(list_preference_5d_down, finalActiveLens);
                }
                if (verticalDirection == HardwareState.FiveDButtonDirection.POSITIVE) {
                    executeAction(list_preference_5d_up, finalActiveLens);
                }
                if (btn5D.isClicked()) {
                    executeAction(list_preference_5d_push, finalActiveLens);
                }
                if (verticalDirection == HardwareState.FiveDButtonDirection.MIDDLE
                        && horizontalDirection == HardwareState.FiveDButtonDirection.MIDDLE
                        && !btn5D.isClicked()) {
                    executeAction(list_preference_5d_middle, finalActiveLens);
                }
            });
            ToastUtils.setResultToToast("RC Binded");
        });
    }

    private void executeAction(String action, Lens l) {
        switch (action) {
            case "ZoomInContinue":
                cameraZoomIn(l);
                break;
            case "ZoomOutContinue":
                cameraZoomOut(l);
                break;
            case "ZoomStop":
                cameraZoomStop(l);
                break;
            case "CamUp":
                gimbalUp();
                break;
            case "CamDown":
                gimbalDown();
                break;
            case "CamLeft":
                gimbalLeft();
                break;
            case "CamRight":
                gimbalRight();
                break;
            case "CamStop":
                gimbalStop();
                break;
            case "CamResetToMiddle":
                gimbalReset();
                break;
            case "Unbind":
                //do nothing
                break;
            default:
                break;
        }
    }

    private void cameraZoomIn(Lens l) {
        l.startContinuousOpticalZoom(SettingsDefinitions.ZoomDirection.ZOOM_IN, SettingsDefinitions.ZoomSpeed.NORMAL, djiError -> {
            if (djiError != null) {
                Timber.i(djiError.getDescription());
            }
        });
    }

    private void cameraZoomOut(Lens l) {
        l.startContinuousOpticalZoom(SettingsDefinitions.ZoomDirection.ZOOM_OUT, SettingsDefinitions.ZoomSpeed.NORMAL, djiError -> {
            if (djiError != null) {
                Timber.i(djiError.getDescription());
            }
        });
    }

    private void cameraZoomStop(Lens l) {
        l.stopContinuousOpticalZoom(djiError -> {
            if (djiError != null) {
                Timber.i(djiError.getDescription());
            }
        });
    }

    private void gimbalReset() {
        gimbalController.reset();
    }
    private void gimbalStop() {
        gimbalController.stopRotation();
    }
    private void gimbalLeft() {
        gimbalController.startRotation(0, -0.2f);
    }
    private void gimbalRight() {
        gimbalController.startRotation(0, 0.2f);
    }
    private void gimbalUp() {
        gimbalController.startRotation(0.2f, 0);
    }
    private void gimbalDown() {
        gimbalController.startRotation(-0.2f, 0);
    }

    @Override
    public void zoomCamera() {
        if (cameraList.size() == 0) {
            Timber.i("cameraList is null");
            ToastUtils.setResultToToast("cameraList is null");
            return;
        }
        Camera camera = cameraList.get(0);
        camera.setCameraVideoStreamSource(CameraVideoStreamSource.ZOOM, djiError -> {
            if (djiError != null) {
                Timber.i(djiError.getDescription());
            }
        });
    }

    @Override
    public void wideCamera() {
        if (cameraList.size() == 0) {
            Timber.i("cameraList is null");
            ToastUtils.setResultToToast("cameraList is null");
            return;
        }
        Camera camera = cameraList.get(0);
        camera.setCameraVideoStreamSource(CameraVideoStreamSource.WIDE, djiError -> {
            if (djiError != null) {
                Timber.i(djiError.getDescription());
            }
        });
    }

    private void proceedClick() {

    }

    @Override
    public void remoteControllerBind() {

        WORKER.submit(() -> {

            RemoteController remoteController = aircraft.getRemoteController();

            boolean supported = remoteController.isCustomizableButtonSupported();
            remoteController.getButtonConfig(new CommonCallbacks.CompletionCallbackWith<ProfessionalRC.ButtonConfiguration>() {
                @Override
                public void onSuccess(ProfessionalRC.ButtonConfiguration buttonConfiguration) {

                }

                @Override
                public void onFailure(DJIError djiError) {

                }
            });
            remoteController.getSelectedButtonProfileGroup(new CommonCallbacks.CompletionCallbackWith<String>() {
                @Override
                public void onSuccess(String s) {

                }

                @Override
                public void onFailure(DJIError djiError) {

                }
            });
            remoteController.fetchCustomizedActionOfButton(ProfessionalRC.CustomizableButton.C1, new CommonCallbacks.CompletionCallbackWith<ProfessionalRC.ButtonAction>() {
                @Override
                public void onSuccess(ProfessionalRC.ButtonAction buttonAction) {

                }

                @Override
                public void onFailure(DJIError djiError) {

                }
            });
            remoteController.customizeButton(ProfessionalRC.CustomizableButton.C1, ProfessionalRC.ButtonAction.CameraZoom, djiError -> {
                //  ToastUtils.showToast("C1 ZOOM binded");
            });
            remoteController.customizeButton(ProfessionalRC.CustomizableButton.FIVE_D_UP, ProfessionalRC.ButtonAction.GIMBAL_YAW_RECENTER, djiError -> {
                //ToastUtils.showToast("FIVE_D_UP ZOOM binded");
            });
        });


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
            aircraft.getFlightController().getSimulator().start(InitializationData.createInstance(new LocationCoordinate2D(BASE_LATITUDE, BASE_LONGITUDE), REFRESH_FREQ, SATELLITE_COUNT), new CommonCallbacks.CompletionCallback() {
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
    public void setMediaFileCustomInformation(String information) {
        KeyManager km = KeyManager.getInstance();

        Timber.i("setMediaFileCustomInformation %s", information);
        DJIKey CUSTOM_INFORMATION_KEY = CameraKey.create(CameraKey.CUSTOM_INFORMATION);

        km.setValue(CUSTOM_INFORMATION_KEY, information, new SetCallback() {
            @Override
            public void onSuccess() {
                ToastUtils.showToast("Set CUSTOM_INFORMATION_KEY - SUCCESS");
            }

            @Override
            public void onFailure(@NonNull DJIError djiError) {
                ToastUtils.showToast("Set CUSTOM_INFORMATION_KEY - Failure - " + djiError.getDescription());
            }
        });
    }
}
