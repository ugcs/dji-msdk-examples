package com.example.ugcssample.drone.camera.settings;

import android.content.Context;
import android.graphics.PointF;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.ugcssample.drone.DjiToFutureAdapter;
import com.example.ugcssample.drone.camera.Lens;
import com.example.ugcssample.drone.camera.settings.lens.AntiFlickerFrequency;
import com.example.ugcssample.drone.camera.settings.lens.Aperture;
import com.example.ugcssample.drone.camera.settings.lens.DisplayMode;
import com.example.ugcssample.drone.camera.settings.lens.ExposureCompensation;
import com.example.ugcssample.drone.camera.settings.lens.ExposureMode;
import com.example.ugcssample.drone.camera.settings.lens.FocusAssistantSettings;
import com.example.ugcssample.drone.camera.settings.lens.FocusMode;
import com.example.ugcssample.drone.camera.settings.lens.HybridZoomSpec;
import com.example.ugcssample.drone.camera.settings.lens.ISO;
import com.example.ugcssample.drone.camera.settings.lens.LensType;
import com.example.ugcssample.drone.camera.settings.lens.MeteringMode;
import com.example.ugcssample.drone.camera.settings.lens.OpticalZoomSpec;
import com.example.ugcssample.drone.camera.settings.lens.PhotoAspectRatio;
import com.example.ugcssample.drone.camera.settings.lens.PhotoFileFormat;
import com.example.ugcssample.drone.camera.settings.lens.ShutterSpeed;
import com.example.ugcssample.drone.camera.settings.lens.VideoFileFormat;
import com.example.ugcssample.drone.camera.settings.lens.VideoFrameRate;
import com.example.ugcssample.drone.camera.settings.lens.VideoResolution;
import com.example.ugcssample.drone.camera.settings.lens.VideoStandard;
import com.example.ugcssample.drone.camera.settings.lens.WhiteBalance;
import com.example.ugcssample.drone.camera.settings.lens.ZoomDirection;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dji.common.camera.ExposureSettings;
import dji.common.camera.ResolutionAndFrameRate;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.keysdk.CameraKey;
import dji.keysdk.KeyManager;
import dji.sdk.camera.Camera;
import timber.log.Timber;


public class DjiLens implements Lens {

    private final Object initLock = new Object();

    private static final ScheduledExecutorService WORKER = Executors.newSingleThreadScheduledExecutor();

    private static final List<String> MSX_SUPPORTED_CAMERAS = new ArrayList<>(Arrays.asList(
            Camera.DisplayNameMavic2EnterpriseDual_IR,
            Camera.DisplayNameXT2_IR)
    );

    private dji.sdk.camera.Lens djiLens;
    private Camera djiMultiLensCamera; // required to determine settings range
    private Camera djiSingleLensCamera;
    private String name;
    private int id;

    private boolean isInitialised;
    private final Set<Callback> initialisedListeners = new HashSet<>();
    private final Set<ValueChangeListener<AntiFlickerFrequency>> antiFlickerChangeListeners = new HashSet<>();
    private final Set<ValueChangeListener<Aperture>> apertureChangeListeners = new HashSet<>();
    private final Set<ValueChangeListener<DisplayMode>> displayModeChangeListeners = new HashSet<>();
    private final Set<ValueChangeListener<ExposureCompensation>> exposureCompensationChangeListeners = new HashSet<>();
    private final Set<ValueChangeListener<ExposureMode>> exposureModeChangeListeners = new HashSet<>();
    private final Set<ValueChangeListener<FocusMode>> focusModeChangeListeners = new HashSet<>();
    private final Set<ValueChangeListener<ISO>> isoChangeListeners = new HashSet<>();
    private final Set<ValueChangeListener<MeteringMode>> meteringModeChangeListeners = new HashSet<>();
    private final Set<ValueChangeListener<PhotoAspectRatio>> photoAspectRatioChangeListeners = new HashSet<>();
    private final Set<ValueChangeListener<PhotoFileFormat>> photoFileFormatChangeListeners = new HashSet<>();
    private final Set<ValueChangeListener<ShutterSpeed>> shutterSpeedChangeListeners = new HashSet<>();
    private final Set<ValueChangeListener<VideoFileFormat>> videoFileFormatChangeListeners = new HashSet<>();
    private final Set<ValueChangeListener<com.example.ugcssample.drone.camera.settings.lens.ResolutionAndFrameRate>> resolutionAndFrameRateChangeListeners = new HashSet<>();
    private final Set<ValueChangeListener<VideoStandard>> videoStandardChangeListeners = new HashSet<>();
    private final Set<ValueChangeListener<WhiteBalance>> whiteBalanceChangeListeners = new HashSet<>();
    private final Set<ValueChangeListener<PointF>> focusTargetChangeListeners = new HashSet<>();
    private final Set<ValueChangeListener<FocusAssistantSettings>> focusAssistantSettingsChangeListeners = new HashSet<>();
    private final Set<ValueChangeListener<Integer>> focusRingValueChangeListeners = new HashSet<>();
    
    private void handleExposureSettings(ExposureSettings exposureSettings) {
        settings.iso = DjiLensValuesMapping.intToSdkIso(exposureSettings.getISO());
        settings.shutterSpeed = exposureSettings.getShutterSpeed();
        settings.aperture = exposureSettings.getAperture();
        settings.exposureCompensation = exposureSettings.getExposureCompensation();
        if (djiLens != null) {
            djiLens.setExposureSettingsCallback(null);
        }
        if (djiSingleLensCamera != null) {
            djiSingleLensCamera.setExposureSettingsCallback(null);
        }
    }

    private DjiLensSettings settings;

    public DjiLens(@NonNull dji.sdk.camera.Lens djiLens, @NonNull Camera djiCamera, @NonNull Context context) {
        this.djiLens = djiLens;
        this.djiMultiLensCamera = djiCamera;
        id = djiLens.getIndex();
        name = getLensName(context);
        initSettings();
    }

    public DjiLens(@NonNull Camera djiCamera, @NonNull Context context) {
        this.djiSingleLensCamera = djiCamera;
        id = 0;
        name = getSingleLensName(context);
        initSettings();
    }

    private void onSettingsComponentInitialised() {
        synchronized (initLock) {
            boolean last = isInitialised;
            isInitialised = calculateIsInitialised();
            if (isInitialised) {
                if (last) {
                    throw new AssertionError("Assertion failed. The initialisation happens two times");
                }
                onIsInitialisedChanged();
            }
        }
    }

    private void initSettings() {
        settings = new DjiLensSettings();

        initThermalTempRange();

        // TODO: workaround because callback isn't called more then once
        // try to remove when dji sdk will update
        WORKER.scheduleAtFixedRate(() -> {
            if (djiLens != null) {
                djiLens.setExposureSettingsCallback(this::handleExposureSettings);
            }
            if (djiSingleLensCamera != null) {
                djiSingleLensCamera.setExposureSettingsCallback(this::handleExposureSettings);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        if (djiLens != null) {
            String cameraName = djiLens.getDisplayName();
            Timber.i("Receiving settings from lens %s", cameraName);
            djiLens.getThermalIsothermEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    Timber.i("Receiving thermalIsothermEnabled: [%s], camera: [%s]", aBoolean, cameraName);
                    settings.thermalIsothermEnabledInited = true;
                    settings.thermalIsothermEnabled = aBoolean;
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.thermalIsothermEnabledInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving thermalIsothermEnabled: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getThermalIsothermLowerValue(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                @Override
                public void onSuccess(Integer val) {
                    Timber.i("Receiving thermalIsothermLowerValue: [%s], camera: [%s]", val, cameraName);
                    settings.thermalIsothermLowerValueInited = true;
                    settings.thermalIsothermLowerValue = val;
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.thermalIsothermLowerValueInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving thermalIsothermLowerValue: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getThermalIsothermUpperValue(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                @Override
                public void onSuccess(Integer val) {
                    Timber.i("Receiving thermalIsothermUpperValue: [%s], camera: [%s]", val, cameraName);
                    settings.thermalIsothermUpperValueInited = true;
                    settings.thermalIsothermUpperValue = val;
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.thermalIsothermUpperValueInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving thermalIsothermUpperValue: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getAntiFlickerFrequency(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.AntiFlickerFrequency>() {
                @Override
                public void onSuccess(SettingsDefinitions.AntiFlickerFrequency antiFlickerFrequency) {
                    settings.antiFlickerInited = true;
                    setAntiFlickerFrequencyInternal(antiFlickerFrequency);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.antiFlickerInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving antiFlickerFrequency: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getAperture(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.Aperture>() {
                @Override
                public void onSuccess(SettingsDefinitions.Aperture aperture) {
                    settings.apertureInited = true;
                    setApertureInternal(aperture);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.apertureInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving aperture: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getDisplayMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.DisplayMode>() {
                @Override
                public void onSuccess(SettingsDefinitions.DisplayMode displayMode) {
                    settings.displayModeInited = true;
                    setDisplayModeInternal(displayMode);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.displayModeInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving displayMode: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getExposureCompensation(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.ExposureCompensation>() {
                @Override
                public void onSuccess(SettingsDefinitions.ExposureCompensation exposureCompensation) {
                    settings.exposureCompensationInited = true;
                    setExposureCompensationInternal(exposureCompensation);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.exposureCompensationInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving exposureCompensation: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getExposureMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.ExposureMode>() {
                @Override
                public void onSuccess(SettingsDefinitions.ExposureMode exposureMode) {
                    settings.exposureModeInited = true;
                    setExposureModeInternal(exposureMode);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.exposureModeInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving exposureMode: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getFocusMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.FocusMode>() {
                @Override
                public void onSuccess(SettingsDefinitions.FocusMode focusMode) {
                    settings.focusModeInited = true;
                    setFocusModeInternal(focusMode);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.focusModeInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving focusMode: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getISO(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.ISO>() {
                @Override
                public void onSuccess(SettingsDefinitions.ISO iso) {
                    settings.isoInited = true;
                    setIsoInternal(iso);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.isoInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving iso: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getMeteringMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.MeteringMode>() {
                @Override
                public void onSuccess(SettingsDefinitions.MeteringMode meteringMode) {
                    settings.meteringModeInited = true;
                    setMeteringModeInternal(meteringMode);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.meteringModeInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving meteringMode: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getPhotoAspectRatio(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.PhotoAspectRatio>() {
                @Override
                public void onSuccess(SettingsDefinitions.PhotoAspectRatio photoAspectRatio) {
                    settings.photoAspectRatioInited = true;
                    setPhotoAspectRatioInternal(photoAspectRatio);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.photoAspectRatioInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving photoAspectRatio: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getPhotoFileFormat(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.PhotoFileFormat>() {
                @Override
                public void onSuccess(SettingsDefinitions.PhotoFileFormat photoFileFormat) {
                    settings.photoFileFormatInited = true;
                    setPhotoFileFormatInternal(photoFileFormat);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.photoFileFormatInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving photoFileFormat: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getShutterSpeed(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.ShutterSpeed>() {
                @Override
                public void onSuccess(SettingsDefinitions.ShutterSpeed shutterSpeed) {
                    settings.shutterSpeedInited = true;
                    setShutterSpeedInternal(shutterSpeed);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.shutterSpeedInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving shutterSpeed: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getVideoFileFormat(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.VideoFileFormat>() {
                @Override
                public void onSuccess(SettingsDefinitions.VideoFileFormat videoFileFormat) {
                    settings.videoFileFormatInited = true;
                    setVideoFileFormatInternal(videoFileFormat);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.videoFileFormatInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving videoFileFormat: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getVideoResolutionAndFrameRate(new CommonCallbacks.CompletionCallbackWith<ResolutionAndFrameRate>() {
                @Override
                public void onSuccess(ResolutionAndFrameRate resolutionAndFrameRate) {
                    settings.videoResolutionAndFrameRateInited = true;
                    setResolutionAndFrameRateInternal(resolutionAndFrameRate);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.videoResolutionAndFrameRateInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving videoResolutionAndFrameRate: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getVideoStandard(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.VideoStandard>() {
                @Override
                public void onSuccess(SettingsDefinitions.VideoStandard videoStandard) {
                    settings.videoStandardInited = true;
                    setVideoStandardInternal(videoStandard);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.videoStandardInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving videoStandard: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getWhiteBalance(new CommonCallbacks.CompletionCallbackWith<dji.common.camera.WhiteBalance>() {
                @Override
                public void onSuccess(dji.common.camera.WhiteBalance whiteBalance) {
                    settings.whiteBalanceInited = true;
                    setWhiteBalanceInternal(whiteBalance);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.whiteBalanceInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving videoStandard: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getFocusTarget(new CommonCallbacks.CompletionCallbackWith<PointF>() {
                @Override
                public void onSuccess(PointF pointF) {
                    settings.focusTargetInited = true;
                    setFocusTargetInternal(pointF);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.focusTargetInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving focusTarget: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getFocusRingValue(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                @Override
                public void onSuccess(Integer integer) {
                    settings.focusRingValueInited = true;
                    setFocusRingValueInternal(integer);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.focusRingValueInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving focusRingValue: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getFocusRingValueUpperBound(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                @Override
                public void onSuccess(Integer integer) {
                    settings.focusRingValueMaxInited = true;
                    settings.focusRingValueMax = integer;
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.focusRingValueMaxInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving focusRingValue: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getFocusAssistantSettings(new CommonCallbacks.CompletionCallbackWithTwoParam<Boolean, Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean, Boolean aBoolean2) {
                    settings.focusAssistantSettingsInited = true;
                    setFocusAssistantSettingsInternal(DjiLensValuesMapping.sdkFocusAssistantSettings(new FocusAssistantSettings(aBoolean, aBoolean2)));
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.focusAssistantSettingsInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving focusAssistantSettings: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getOpticalZoomSpec(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.OpticalZoomSpec>() {
                @Override
                public void onSuccess(SettingsDefinitions.OpticalZoomSpec opticalZoomSpec) {
                    settings.opticalZoomSpecInited = true;
                    settings.opticalZoomSpec = opticalZoomSpec;
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.opticalZoomSpecInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving opticalZoomSpec: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getHybridZoomSpec(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.HybridZoomSpec>() {
                @Override
                public void onSuccess(SettingsDefinitions.HybridZoomSpec value) {
                    settings.hybridZoomSpecInited = true;
                    settings.hybridZoomSpec = value;
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.hybridZoomSpecInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving hybridZoomSpec: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getOpticalZoomFocalLength(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                @Override
                public void onSuccess(Integer value) {
                    settings.opticalZoomFocalLengthInited = true;
                    settings.opticalZoomFocalLength = value;
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.opticalZoomFocalLengthInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving opticalZoomFocalLength: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiLens.getHybridZoomFocalLength(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                @Override
                public void onSuccess(Integer value) {
                    settings.hybridZoomFocalLengthInited = true;
                    settings.hybridZoomFocalLength = value;
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.hybridZoomFocalLengthInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving hybridZoomFocalLength: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });
        }
        //=====================================================================================
        //=====================================================================================
        if (djiSingleLensCamera != null)
        //=====================================================================================
        //=====================================================================================
        {

            final String cameraName = djiSingleLensCamera.getDisplayName();
            Timber.i("Receiving settings from camera = %s", cameraName);

            djiSingleLensCamera.getThermalIsothermEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    Timber.i("Receiving thermalIsothermEnabled: [%s], camera: [%s]", aBoolean, cameraName);
                    settings.thermalIsothermEnabledInited = true;
                    settings.thermalIsothermEnabled = aBoolean;
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.thermalIsothermEnabledInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving thermalIsothermEnabled: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getThermalIsothermLowerValue(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                @Override
                public void onSuccess(Integer val) {
                    Timber.i("Receiving thermalIsothermLowerValue: [%s], camera: [%s]", val, cameraName);
                    settings.thermalIsothermLowerValueInited = true;
                    settings.thermalIsothermLowerValue = val;
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.thermalIsothermLowerValueInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving thermalIsothermLowerValue: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getThermalIsothermUpperValue(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                @Override
                public void onSuccess(Integer val) {
                    Timber.i("Receiving thermalIsothermUpperValue: [%s], camera: [%s]", val, cameraName);
                    settings.thermalIsothermUpperValueInited = true;
                    settings.thermalIsothermUpperValue = val;
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.thermalIsothermUpperValueInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving thermalIsothermUpperValue: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getAntiFlickerFrequency(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.AntiFlickerFrequency>() {
                @Override
                public void onSuccess(SettingsDefinitions.AntiFlickerFrequency antiFlickerFrequency) {
                    settings.antiFlickerInited = true;
                    setAntiFlickerFrequencyInternal(antiFlickerFrequency);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.antiFlickerInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving antiFlickerFrequency: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getAperture(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.Aperture>() {
                @Override
                public void onSuccess(SettingsDefinitions.Aperture aperture) {
                    settings.apertureInited = true;
                    setApertureInternal(aperture);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.apertureInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving aperture: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getDisplayMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.DisplayMode>() {
                @Override
                public void onSuccess(SettingsDefinitions.DisplayMode displayMode) {
                    settings.displayModeInited = true;
                    setDisplayModeInternal(displayMode);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.displayModeInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving displayMode: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getExposureCompensation(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.ExposureCompensation>() {
                @Override
                public void onSuccess(SettingsDefinitions.ExposureCompensation exposureCompensation) {
                    settings.exposureCompensationInited = true;
                    setExposureCompensationInternal(exposureCompensation);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.exposureCompensationInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving exposureCompensation: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getExposureMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.ExposureMode>() {
                @Override
                public void onSuccess(SettingsDefinitions.ExposureMode exposureMode) {
                    settings.exposureModeInited = true;
                    setExposureModeInternal(exposureMode);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.exposureModeInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving exposureMode: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getFocusMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.FocusMode>() {
                @Override
                public void onSuccess(SettingsDefinitions.FocusMode focusMode) {
                    settings.focusModeInited = true;
                    setFocusModeInternal(focusMode);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.focusModeInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving focusMode: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getISO(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.ISO>() {
                @Override
                public void onSuccess(SettingsDefinitions.ISO iso) {
                    settings.isoInited = true;
                    setIsoInternal(iso);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.isoInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving iso: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getMeteringMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.MeteringMode>() {
                @Override
                public void onSuccess(SettingsDefinitions.MeteringMode meteringMode) {
                    settings.meteringModeInited = true;
                    setMeteringModeInternal(meteringMode);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.meteringModeInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving meteringMode: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getPhotoAspectRatio(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.PhotoAspectRatio>() {
                @Override
                public void onSuccess(SettingsDefinitions.PhotoAspectRatio photoAspectRatio) {
                    settings.photoAspectRatioInited = true;
                    setPhotoAspectRatioInternal(photoAspectRatio);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.photoAspectRatioInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving photoAspectRatio: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getPhotoFileFormat(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.PhotoFileFormat>() {
                @Override
                public void onSuccess(SettingsDefinitions.PhotoFileFormat photoFileFormat) {
                    settings.photoFileFormatInited = true;
                    setPhotoFileFormatInternal(photoFileFormat);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.photoFileFormatInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving photoFileFormat: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getShutterSpeed(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.ShutterSpeed>() {
                @Override
                public void onSuccess(SettingsDefinitions.ShutterSpeed shutterSpeed) {
                    settings.shutterSpeedInited = true;
                    setShutterSpeedInternal(shutterSpeed);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.shutterSpeedInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving shutterSpeed: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getVideoFileFormat(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.VideoFileFormat>() {
                @Override
                public void onSuccess(SettingsDefinitions.VideoFileFormat videoFileFormat) {
                    settings.videoFileFormatInited = true;
                    setVideoFileFormatInternal(videoFileFormat);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.videoFileFormatInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving videoFileFormat: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getVideoResolutionAndFrameRate(new CommonCallbacks.CompletionCallbackWith<ResolutionAndFrameRate>() {
                @Override
                public void onSuccess(ResolutionAndFrameRate resolutionAndFrameRate) {
                    settings.videoResolutionAndFrameRateInited = true;
                    setResolutionAndFrameRateInternal(resolutionAndFrameRate);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.videoResolutionAndFrameRateInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving videoResolutionAndFrameRate: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getVideoStandard(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.VideoStandard>() {
                @Override
                public void onSuccess(SettingsDefinitions.VideoStandard videoStandard) {
                    settings.videoStandardInited = true;
                    setVideoStandardInternal(videoStandard);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.videoStandardInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving videoStandard: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getWhiteBalance(new CommonCallbacks.CompletionCallbackWith<dji.common.camera.WhiteBalance>() {
                @Override
                public void onSuccess(dji.common.camera.WhiteBalance whiteBalance) {
                    settings.whiteBalanceInited = true;
                    setWhiteBalanceInternal(whiteBalance);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.whiteBalanceInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving videoStandard: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getFocusTarget(new CommonCallbacks.CompletionCallbackWith<PointF>() {
                @Override
                public void onSuccess(PointF pointF) {
                    settings.focusTargetInited = true;
                    setFocusTargetInternal(pointF);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.focusTargetInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving focusTarget: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getFocusRingValue(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                @Override
                public void onSuccess(Integer integer) {
                    settings.focusRingValueInited = true;
                    setFocusRingValueInternal(integer);
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.focusRingValueInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving focusRingValue: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getFocusRingValueUpperBound(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                @Override
                public void onSuccess(Integer integer) {
                    settings.focusRingValueMaxInited = true;
                    settings.focusRingValueMax = integer;
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.focusRingValueMaxInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving focusRingValue: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getFocusAssistantSettings(new CommonCallbacks.CompletionCallbackWithTwoParam<Boolean, Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean, Boolean aBoolean2) {
                    settings.focusAssistantSettingsInited = true;
                    setFocusAssistantSettingsInternal(DjiLensValuesMapping.sdkFocusAssistantSettings(new FocusAssistantSettings(aBoolean, aBoolean2)));
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.focusAssistantSettingsInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving focusAssistantSettings: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getOpticalZoomSpec(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.OpticalZoomSpec>() {
                @Override
                public void onSuccess(SettingsDefinitions.OpticalZoomSpec opticalZoomSpec) {
                    settings.opticalZoomSpecInited = true;
                    settings.opticalZoomSpec = opticalZoomSpec;
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.opticalZoomSpecInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving opticalZoomSpec: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getHybridZoomSpec(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.HybridZoomSpec>() {
                @Override
                public void onSuccess(SettingsDefinitions.HybridZoomSpec value) {
                    settings.hybridZoomSpecInited = true;
                    settings.hybridZoomSpec = value;
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.hybridZoomSpecInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving hybridZoomSpec: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getOpticalZoomFocalLength(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                @Override
                public void onSuccess(Integer value) {
                    settings.opticalZoomFocalLengthInited = true;
                    settings.opticalZoomFocalLength = value;
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.opticalZoomFocalLengthInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving opticalZoomFocalLength: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });

            djiSingleLensCamera.getHybridZoomFocalLength(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                @Override
                public void onSuccess(Integer value) {
                    settings.hybridZoomFocalLengthInited = true;
                    settings.hybridZoomFocalLength = value;
                    onSettingsComponentInitialised();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    settings.hybridZoomFocalLengthInited = true;
                    onSettingsComponentInitialised();
                    Timber.e("An error is raised on receiving hybridZoomFocalLength: [%s], camera: [%s]", djiError.getDescription(), cameraName);
                }
            });
        }
    }

    @NonNull
    @Override
    public LensType getType() {
        if (djiLens != null)
            return DjiLensValuesMapping.lensType(djiLens.getType());

        if (djiSingleLensCamera != null) {
            if (djiSingleLensCamera.isThermalCamera())
                return LensType.THERMAL;
            else
                return LensType.UNKNOWN;
        }
        return LensType.UNKNOWN;
    }

    @Override
    public int getId() {
        return id;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public AntiFlickerFrequency getAntiFlickerFrequency() {
        return DjiLensValuesMapping.antiFlickerFrequency(settings.antiFlicker);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public CompletableFuture<Void> setAntiFlickerFrequency(@NotNull AntiFlickerFrequency antiFlickerFrequency) {
        SettingsDefinitions.AntiFlickerFrequency sdkAntiFlickerFrequency = DjiLensValuesMapping.sdkAntiFlickerFrequency(antiFlickerFrequency);
        if (djiLens != null) {
            return DjiToFutureAdapter.getFuture(djiLens::setAntiFlickerFrequency, sdkAntiFlickerFrequency);
        }

        if (djiSingleLensCamera != null) {
            return DjiToFutureAdapter.getFuture(djiSingleLensCamera::setAntiFlickerFrequency, sdkAntiFlickerFrequency);
        }
        return null;
    }

    @Override
    public void addAntiFlickerFrequencyListener(@NonNull ValueChangeListener<AntiFlickerFrequency> listener) {
        synchronized (antiFlickerChangeListeners) {
            antiFlickerChangeListeners.add(listener);
        }
    }

    @Override
    public void removeAntiFlickerFrequencyListener(@NonNull ValueChangeListener<AntiFlickerFrequency> listener) {
        synchronized (antiFlickerChangeListeners) {
            antiFlickerChangeListeners.remove(listener);
        }
    }

    @Override
    public Aperture getAperture() {
        return DjiLensValuesMapping.aperture(settings.aperture);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public CompletableFuture<Void> setAperture(@NonNull Aperture aperture) {
        SettingsDefinitions.Aperture sdkAperture = DjiLensValuesMapping.sdkAperture(aperture);
        if (djiLens != null) {
            return DjiToFutureAdapter.getFuture(djiLens::setAperture, sdkAperture);
        }

        if (djiSingleLensCamera != null) {
            return DjiToFutureAdapter.getFuture(djiSingleLensCamera::setAperture, sdkAperture);
        }
        return null;
    }

    @Override
    public void addApertureChangeListener(@NonNull ValueChangeListener<Aperture> listener) {
        synchronized (apertureChangeListeners) {
            apertureChangeListeners.add(listener);
        }
    }

    @Override
    public void removeApertureChangeListener(@NonNull ValueChangeListener<Aperture> listener) {
        synchronized (apertureChangeListeners) {
            apertureChangeListeners.add(listener);
        }
    }

    @NonNull
    @Override
    public List<Aperture> getSupportedApertures() {
        if (djiLens != null) {
            if (djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20T)
                    || djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20)) {
                ArrayList<Aperture> arr = new ArrayList<>();
                if (djiLens.getType() == SettingsDefinitions.LensType.ZOOM) {
                    arr.add(Aperture.F_3_DOT_5);
                    return arr;
                }
            }
        }
        if (djiSingleLensCamera != null) {
            SettingsDefinitions.Aperture[] raw = djiSingleLensCamera.getCapabilities().apertureRange();
            List<Aperture> result = new ArrayList<>();
            if (raw != null) {
                for (SettingsDefinitions.Aperture a :
                        raw) {
                    result.add(DjiLensValuesMapping.aperture(a));
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    @Override
    public DisplayMode getDisplayMode() {
        return DjiLensValuesMapping.displayMode(settings.displayMode);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public CompletableFuture<Void> setDisplayMode(@NotNull DisplayMode displayMode) {
        SettingsDefinitions.DisplayMode sdkDisplayMode = DjiLensValuesMapping.sdkDisplayMode(displayMode);
        if (djiLens != null) {
            return DjiToFutureAdapter.getFuture(djiLens::setDisplayMode,sdkDisplayMode);
        }

        if (djiSingleLensCamera != null) {
            return DjiToFutureAdapter.getFuture(djiSingleLensCamera::setDisplayMode,sdkDisplayMode);
        }
        return null;
    }

    @Override
    public void addDisplayModeChangeListener(@NonNull ValueChangeListener<DisplayMode> listener) {
        synchronized (displayModeChangeListeners) {
            displayModeChangeListeners.add(listener);
        }
    }

    @Override
    public void removeDisplayModeChangeListener(@NonNull ValueChangeListener<DisplayMode> listener) {
        synchronized (displayModeChangeListeners) {
            displayModeChangeListeners.remove(listener);
        }
    }

    @NonNull
    @Override
    public List<DisplayMode> getSupportedDisplayModes() {
        if (djiSingleLensCamera != null &&
                MSX_SUPPORTED_CAMERAS.contains(djiSingleLensCamera.getDisplayName())) {
            ArrayList<DisplayMode> result = new ArrayList<>();
            result.add(DisplayMode.VISUAL_ONLY);
            result.add(DisplayMode.THERMAL_ONLY);
            result.add(DisplayMode.MSX);
            return result;
        }
        return new ArrayList<>();
    }

    @Override
    public ExposureCompensation getExposureCompensation() {
        return DjiLensValuesMapping.exposureCompensation(settings.exposureCompensation);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public CompletableFuture<Void> setExposureCompensation(@NonNull ExposureCompensation exposureCompensation) {
        SettingsDefinitions.ExposureCompensation sdkExposureCompensation = DjiLensValuesMapping.sdkExposureCompensation(exposureCompensation);
        if (djiLens != null) {
            return DjiToFutureAdapter.getFuture(djiLens::setExposureCompensation,sdkExposureCompensation);
        }

        if (djiSingleLensCamera != null) {
            return DjiToFutureAdapter.getFuture(djiSingleLensCamera::setExposureCompensation,sdkExposureCompensation);
        }
        return null;
    }

    @Override
    public void addExposureCompensationChangeListener(@NonNull ValueChangeListener<ExposureCompensation> listener) {
        synchronized (exposureCompensationChangeListeners) {
            exposureCompensationChangeListeners.add(listener);
        }
    }

    @Override
    public void removeExposureCompensationChangeListener(@NonNull ValueChangeListener<ExposureCompensation> listener) {
        synchronized (exposureCompensationChangeListeners) {
            exposureCompensationChangeListeners.remove(listener);
        }
    }

    @NotNull
    @Override
    public List<ExposureCompensation> getSupportedExposureCompensations() {
        if (djiLens != null) {
            if ((djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20T)
                    || djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20))
                    &&
                    (djiLens.getType() == SettingsDefinitions.LensType.WIDE
                            || djiLens.getType() == SettingsDefinitions.LensType.ZOOM)) {
                ArrayList<ExposureCompensation> arr = new ArrayList<>();
                arr.add(ExposureCompensation.N_3_0);
                arr.add(ExposureCompensation.N_2_7);
                arr.add(ExposureCompensation.N_2_3);
                arr.add(ExposureCompensation.N_2_0);
                arr.add(ExposureCompensation.N_1_7);
                arr.add(ExposureCompensation.N_1_3);
                arr.add(ExposureCompensation.N_1_0);
                arr.add(ExposureCompensation.N_0_7);
                arr.add(ExposureCompensation.N_0_3);
                arr.add(ExposureCompensation.N_0_0);
                arr.add(ExposureCompensation.P_0_3);
                arr.add(ExposureCompensation.P_0_7);
                arr.add(ExposureCompensation.P_1_0);
                arr.add(ExposureCompensation.P_1_3);
                arr.add(ExposureCompensation.P_1_7);
                arr.add(ExposureCompensation.P_2_0);
                arr.add(ExposureCompensation.P_2_3);
                arr.add(ExposureCompensation.P_2_7);
                arr.add(ExposureCompensation.P_3_0);
                return arr;
            }
        }
        if (djiSingleLensCamera != null) {
            SettingsDefinitions.ExposureCompensation[] raw = djiSingleLensCamera.getCapabilities().exposureCompensationRange();
            List<ExposureCompensation> result = new ArrayList<>();
            for (SettingsDefinitions.ExposureCompensation val :
                    raw) {
                result.add(DjiLensValuesMapping.exposureCompensation(val));
            }
            return result;
        }
        return new ArrayList<>();
    }

    @Override
    public ExposureMode getExposureMode() {
        return DjiLensValuesMapping.exposureMode(settings.exposureMode);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public CompletableFuture<Void> setExposureMode(@NonNull ExposureMode exposureMode) {
        SettingsDefinitions.ExposureMode sdkExposureMode = DjiLensValuesMapping.sdkExposureMode(exposureMode);
        if (djiLens != null) {
            return DjiToFutureAdapter.getFuture(djiLens::setExposureMode,sdkExposureMode);
        }

        if (djiSingleLensCamera != null) {
            return DjiToFutureAdapter.getFuture(djiSingleLensCamera::setExposureMode,sdkExposureMode);
        }
        return null;
    }

    @Override
    public void addExposureModeChangeListener(@NonNull ValueChangeListener<ExposureMode> listener) {
        synchronized (exposureModeChangeListeners) {
            exposureModeChangeListeners.add(listener);
        }
    }

    @Override
    public void removeExposureModeChangeListener(@NonNull ValueChangeListener<ExposureMode> listener) {
        synchronized (exposureModeChangeListeners) {
            exposureModeChangeListeners.remove(listener);
        }
    }

    @NonNull
    @Override
    public List<ExposureMode> getSupportedExposureModes() {
        if (djiLens != null) {
            if ((djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20T)
                    || djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20))) {
                if (djiLens.getType() == SettingsDefinitions.LensType.ZOOM) {
                    ArrayList<ExposureMode> arr = new ArrayList<>();
                    arr.add(ExposureMode.MANUAL);
                    arr.add(ExposureMode.PROGRAM);
                    return arr;
                }
            }
        }
        if (djiSingleLensCamera != null) {
            SettingsDefinitions.ExposureMode[] raw = djiSingleLensCamera.getCapabilities().exposureModeRange();
            List<ExposureMode> result = new ArrayList<>();
            for (SettingsDefinitions.ExposureMode val :
                    raw) {
                result.add(DjiLensValuesMapping.exposureMode(val));
            }
            return result;
        }

        return new ArrayList<>();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public CompletableFuture<SettingsDefinitions.FocusMode> getFocusMode() {
        if (djiLens != null) {
            return DjiToFutureAdapter.getFuture(djiLens::getFocusMode);
        }
        if (djiSingleLensCamera != null) {
            return DjiToFutureAdapter.getFuture(djiSingleLensCamera::getFocusMode);
        }
        return null;
    }

    @Override
    public void setFocusMode(@NonNull FocusMode focusMode, Callback onSet) {
        if (djiLens != null) {
            djiLens.setFocusMode(DjiLensValuesMapping.sdkFocusMode(focusMode), djiError -> {
                if (djiError == null) {
                    setFocusModeInternal(DjiLensValuesMapping.sdkFocusMode(focusMode));
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }

        if (djiSingleLensCamera != null) {
            djiSingleLensCamera.setFocusMode(DjiLensValuesMapping.sdkFocusMode(focusMode), djiError -> {
                if (djiError == null) {
                    setFocusModeInternal(DjiLensValuesMapping.sdkFocusMode(focusMode));
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }
    }

    @Override
    public void addFocusModeChangeListener(@NonNull ValueChangeListener<FocusMode> listener) {
        synchronized (focusModeChangeListeners) {
            focusModeChangeListeners.add(listener);
        }
    }

    @Override
    public void removeFocusModeChangeListener(@NonNull ValueChangeListener<FocusMode> listener) {
        synchronized (focusModeChangeListeners) {
            focusModeChangeListeners.remove(listener);
        }
    }

    @Override
    public ISO getISO() {
        return DjiLensValuesMapping.iso(settings.iso);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public CompletableFuture<Void> setISO(@NonNull ISO iso) {
        SettingsDefinitions.ISO sdkIso = DjiLensValuesMapping.sdkIso(iso);
        if (djiLens != null) {
            return DjiToFutureAdapter.getFuture(djiLens::setISO,sdkIso);
        }

        if (djiSingleLensCamera != null) {
            return DjiToFutureAdapter.getFuture(djiSingleLensCamera::setISO,sdkIso);
        }
        return null;
    }

    @Override
    public void addISOChangeListener(@NonNull ValueChangeListener<ISO> listener) {
        synchronized (isoChangeListeners) {
            isoChangeListeners.add(listener);
        }
    }

    @Override
    public void removeISOChangeListener(@NonNull ValueChangeListener<ISO> listener) {
        synchronized (isoChangeListeners) {
            isoChangeListeners.remove(listener);
        }
    }

    @NonNull
    @Override
    public List<ISO> getSupportedISOs() {
        if (djiLens != null) {
            if ((djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20T)
                    || djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20))
                    &&
                    (djiLens.getType() == SettingsDefinitions.LensType.WIDE
                            || djiLens.getType() == SettingsDefinitions.LensType.ZOOM)) {
                ArrayList<ISO> arr = new ArrayList<>();
                arr.add(ISO.ISO_100);
                arr.add(ISO.ISO_200);
                arr.add(ISO.ISO_400);
                arr.add(ISO.ISO_800);
                arr.add(ISO.ISO_1600);
                arr.add(ISO.ISO_3200);
                arr.add(ISO.ISO_6400);
                arr.add(ISO.ISO_12800);
                arr.add(ISO.ISO_25600);
                return arr;
            }
        }
        if (djiSingleLensCamera != null) {
            SettingsDefinitions.ISO[] raw = djiSingleLensCamera.getCapabilities().ISORange();
            List<ISO> result = new ArrayList<>();
            for (SettingsDefinitions.ISO val :
                    raw) {
                result.add(DjiLensValuesMapping.iso(val));
            }
            return result;
        }
        return new ArrayList<>();
    }

    @Override
    public MeteringMode getMeteringMode() {
        return DjiLensValuesMapping.meteringMode(settings.meteringMode);
    }

    @Override
    public void setMeteringMode(@NonNull MeteringMode meteringMode, Callback onSet) {
        if (djiLens != null) {
            djiLens.setMeteringMode(DjiLensValuesMapping.sdkMeteringMode(meteringMode), djiError -> {
                if (djiError == null) {
                    setMeteringModeInternal(DjiLensValuesMapping.sdkMeteringMode(meteringMode));
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }

        if (djiSingleLensCamera != null) {
            djiSingleLensCamera.setMeteringMode(DjiLensValuesMapping.sdkMeteringMode(meteringMode), djiError -> {
                if (djiError == null) {
                    setMeteringModeInternal(DjiLensValuesMapping.sdkMeteringMode(meteringMode));
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }
    }

    @Override
    public void addMeteringModeChangeListener(@NonNull ValueChangeListener<MeteringMode> listener) {
        synchronized (meteringModeChangeListeners) {
            meteringModeChangeListeners.add(listener);
        }
    }

    @Override
    public void removeMeteringModeChangeListener(@NonNull ValueChangeListener<MeteringMode> listener) {
        synchronized (meteringModeChangeListeners) {
            meteringModeChangeListeners.remove(listener);
        }
    }

    @NonNull
    @Override
    public List<MeteringMode> getSupportedMeteringModes() {
        if (djiLens != null) {
            if (djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20)
                    || djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20T)) {
                if (djiLens.getType() == SettingsDefinitions.LensType.ZOOM) {
                    ArrayList<MeteringMode> modes = new ArrayList<>();
                    modes.add(MeteringMode.SPOT);
                    modes.add(MeteringMode.CENTER);
                    return modes;
                }
            }
        }
        if (djiSingleLensCamera != null) {
            KeyManager km = KeyManager.getInstance();
            CameraKey djiKey = CameraKey.create(CameraKey.IS_METERING_MODE_SUPPORTED);
            if (djiKey == null) {
                Timber.e("Can't create CameraKey IS_METERING_MODE_SUPPORTED.");
                return new ArrayList<>();
            }
            Boolean isMeteringModeSupported = (Boolean) km.getValue(djiKey);
            if (isMeteringModeSupported != null && isMeteringModeSupported) {
                return Arrays.asList(MeteringMode.values());
            }
        }

        return new ArrayList<>();
    }

    @Override
    public PhotoAspectRatio getPhotoAspectRatio() {
        return DjiLensValuesMapping.photoAspectRatio(settings.photoAspectRatio);
    }

    @Override
    public void setPhotoAspectRatio(@NotNull PhotoAspectRatio photoAspectRatio, Callback onSet) {
        if (djiLens != null) {
            djiLens.setPhotoAspectRatio(DjiLensValuesMapping.sdkPhotoAspectRatio(photoAspectRatio), djiError -> {
                if (djiError == null) {
                    setPhotoAspectRatioInternal(DjiLensValuesMapping.sdkPhotoAspectRatio(photoAspectRatio));
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }

        if (djiSingleLensCamera != null) {
            djiSingleLensCamera.setPhotoAspectRatio(DjiLensValuesMapping.sdkPhotoAspectRatio(photoAspectRatio), djiError -> {
                if (djiError == null) {
                    setPhotoAspectRatioInternal(DjiLensValuesMapping.sdkPhotoAspectRatio(photoAspectRatio));
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }
    }

    @Override
    public void addPhotoAspectRatioChangeListener(@NonNull ValueChangeListener<PhotoAspectRatio> listener) {
        synchronized (photoAspectRatioChangeListeners) {
            photoAspectRatioChangeListeners.add(listener);
        }
    }

    @Override
    public void removePhotoAspectRatioChangeListener(@NonNull ValueChangeListener<PhotoAspectRatio> listener) {
        synchronized (photoAspectRatioChangeListeners) {
            photoAspectRatioChangeListeners.remove(listener);
        }
    }

    @NonNull
    @Override
    public List<PhotoAspectRatio> getSupportedPhotoAspectRatio() {
        ArrayList<PhotoAspectRatio> range = new ArrayList<>();
        if (djiSingleLensCamera != null) {
            KeyManager km = KeyManager.getInstance();
            CameraKey djiKey = CameraKey.create(CameraKey.PHOTO_ASPECT_RATIO_RANGE);
            if (djiKey == null) {
                Timber.e("Can't create CameraKey PHOTO_ASPECT_RATIO_RANGE.");
                return new ArrayList<>();
            }
            SettingsDefinitions.PhotoAspectRatio[] sdkRange =
                    (SettingsDefinitions.PhotoAspectRatio[]) km.getValue(djiKey);
            if (sdkRange == null)
                return new ArrayList<>();
            for (SettingsDefinitions.PhotoAspectRatio sdkValue : sdkRange) {
                range.add(DjiLensValuesMapping.photoAspectRatio(sdkValue));
            }
        }

        if (djiLens != null &&
                (djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20T) ||
                        djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20))) {
            if (getType() == LensType.ZOOM) {
                range.add(PhotoAspectRatio.RATIO_4_3);
            }
            if (getType() == LensType.WIDE) {
                range.add(PhotoAspectRatio.RATIO_4_3);
            }
        }

        return range;
    }

    @Override
    public PhotoFileFormat getPhotoFileFormat() {
        return DjiLensValuesMapping.photoFileFormat(settings.photoFileFormat);
    }

    @Override
    public void setPhotoFileFormat(@NotNull PhotoFileFormat photoFileFormat, Callback onSet) {
        if (djiLens != null) {
            djiLens.setPhotoFileFormat(DjiLensValuesMapping.sdkPhotoFileFormat(photoFileFormat), djiError -> {
                if (djiError == null) {
                    setPhotoFileFormatInternal(DjiLensValuesMapping.sdkPhotoFileFormat(photoFileFormat));
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }

        if (djiSingleLensCamera != null) {
            djiSingleLensCamera.setPhotoFileFormat(DjiLensValuesMapping.sdkPhotoFileFormat(photoFileFormat), djiError -> {
                if (djiError == null) {
                    setPhotoFileFormatInternal(DjiLensValuesMapping.sdkPhotoFileFormat(photoFileFormat));
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }
    }

    @Override
    public void addPhotoFileFormatChangeListener(@NonNull ValueChangeListener<PhotoFileFormat> listener) {
        synchronized (photoFileFormatChangeListeners) {
            photoFileFormatChangeListeners.add(listener);
        }
    }

    @Override
    public void removePhotoFileFormatChangeListener(@NonNull ValueChangeListener<PhotoFileFormat> listener) {
        synchronized (photoFileFormatChangeListeners) {
            photoFileFormatChangeListeners.remove(listener);
        }
    }

    @NonNull
    @Override
    public List<PhotoFileFormat> getSupportedPhotoFileFormats() {
        ArrayList<PhotoFileFormat> range = new ArrayList<>();

        if (djiSingleLensCamera != null) {
            KeyManager km = KeyManager.getInstance();
            CameraKey djiKey = CameraKey.create(CameraKey.PHOTO_FILE_FORMAT_RANGE);
            if (djiKey == null) {
                Timber.e("Can't create CameraKey PHOTO_FILE_FORMAT_RANGE.");
                return new ArrayList<>();
            }
            SettingsDefinitions.PhotoFileFormat[] sdkRange =
                    (SettingsDefinitions.PhotoFileFormat[]) km.getValue(djiKey);
            if (sdkRange == null)
                return new ArrayList<>();
            for (SettingsDefinitions.PhotoFileFormat sdkValue : sdkRange) {
                range.add(DjiLensValuesMapping.photoFileFormat(sdkValue));
            }
        }

        if (djiLens != null &&
                (djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20T) ||
                        djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20))) {
            if (getType() == LensType.ZOOM) {
                range.add(PhotoFileFormat.JPEG);
            }
            if (getType() == LensType.WIDE) {
                range.add(PhotoFileFormat.JPEG);
            }
            if (getType() == LensType.THERMAL) {
                range.add(PhotoFileFormat.RADIOMETRIC_JPEG);
            }
        }

        return range;
    }

    @Override
    public ShutterSpeed getShutterSpeed() {
        return DjiLensValuesMapping.shutterSpeed(settings.shutterSpeed);
    }

    @Override
    public void setShutterSpeed(@NonNull ShutterSpeed shutterSpeed, Callback onSet) {
        if (djiLens != null) {
            djiLens.setShutterSpeed(DjiLensValuesMapping.sdkShutterSpeed(shutterSpeed), djiError -> {
                if (djiError == null) {
                    setShutterSpeedInternal(DjiLensValuesMapping.sdkShutterSpeed(shutterSpeed));
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }

        if (djiSingleLensCamera != null) {
            djiSingleLensCamera.setShutterSpeed(DjiLensValuesMapping.sdkShutterSpeed(shutterSpeed), djiError -> {
                if (djiError == null) {
                    setShutterSpeedInternal(DjiLensValuesMapping.sdkShutterSpeed(shutterSpeed));
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }
    }

    @Override
    public void addShutterSpeedChangeListener(@NonNull ValueChangeListener<ShutterSpeed> listener) {
        synchronized (shutterSpeedChangeListeners) {
            shutterSpeedChangeListeners.add(listener);
        }
    }

    @Override
    public void removeShutterSpeedChangeListener(@NonNull ValueChangeListener<ShutterSpeed> listener) {
        synchronized (shutterSpeedChangeListeners) {
            shutterSpeedChangeListeners.remove(listener);
        }
    }

    @NonNull
    @Override
    public List<ShutterSpeed> getSupportedShutterSpeeds() {
        if (djiLens != null) {
            if ((djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20T)
                    || djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20))
                    &&
                    (djiLens.getType() == SettingsDefinitions.LensType.WIDE
                            || djiLens.getType() == SettingsDefinitions.LensType.ZOOM)) {
                ArrayList<ShutterSpeed> arr = new ArrayList<>();
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_8000);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_6400);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_6000);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_5000);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_4000);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_3200);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_3000);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_2500);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_2000);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_1600);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_1500);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_1250);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_1000);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_800);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_725);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_640);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_500);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_400);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_350);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_320);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_250);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_240);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_200);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_180);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_160);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_125);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_120);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_100);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_90);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_80);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_60);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_50);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_40);
                arr.add(ShutterSpeed.SHUTTER_SPEED_1_30);
                return arr;
            }
        }
        if (djiSingleLensCamera != null) {
            SettingsDefinitions.ShutterSpeed[] sdkRange = djiSingleLensCamera.getCapabilities().shutterSpeedRange();
            if (sdkRange == null) {
                Timber.w("Shutter speed range is null");
                return new ArrayList<>();
            }
            ArrayList<ShutterSpeed> range = new ArrayList<>();
            for (SettingsDefinitions.ShutterSpeed sdkValue :
                    sdkRange) {
                range.add(DjiLensValuesMapping.shutterSpeed(sdkValue));
            }
            return range;
        }
        return new ArrayList<>();
    }

    @Override
    public VideoFileFormat getVideoFileFormat() {
        return DjiLensValuesMapping.videoFileFormat(settings.videoFileFormat);
    }

    @Override
    public void setVideoFileFormat(@NonNull VideoFileFormat videoFileFormat, Callback onSet) {
        if (djiLens != null) {
            djiLens.setVideoFileFormat(DjiLensValuesMapping.sdkVideoFileFormat(videoFileFormat), djiError -> {
                if (djiError == null) {
                    setVideoFileFormatInternal(DjiLensValuesMapping.sdkVideoFileFormat(videoFileFormat));
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }

        if (djiSingleLensCamera != null) {
            djiSingleLensCamera.setVideoFileFormat(DjiLensValuesMapping.sdkVideoFileFormat(videoFileFormat), djiError -> {
                if (djiError == null) {
                    setVideoFileFormatInternal(DjiLensValuesMapping.sdkVideoFileFormat(videoFileFormat));
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }
    }

    @Override
    public void addVideoFileFormatChangeListener(@NonNull ValueChangeListener<VideoFileFormat> listener) {
        synchronized (videoFileFormatChangeListeners) {
            videoFileFormatChangeListeners.add(listener);
        }
    }

    @Override
    public void removeVideoFileFormatChangeListener(@NonNull ValueChangeListener<VideoFileFormat> listener) {
        synchronized (videoFileFormatChangeListeners) {
            videoFileFormatChangeListeners.remove(listener);
        }
    }

    @NonNull
    @Override
    public List<VideoFileFormat> getSupportedVideoFileFormats() {
        ArrayList<VideoFileFormat> values = new ArrayList<>();

        if (djiSingleLensCamera != null) {
            KeyManager km = KeyManager.getInstance();
            CameraKey djiKey = CameraKey.create(CameraKey.VIDEO_FILE_FORMAT_RANGE);
            if (djiKey == null) {
                Timber.e("Can't create CameraKey VIDEO_FILE_FORMAT_RANGE.");
                return new ArrayList<>();
            }
            SettingsDefinitions.VideoFileFormat[] sdkModes = (SettingsDefinitions.VideoFileFormat[]) km.getValue(djiKey);
            if (sdkModes == null)
                return new ArrayList<>();
            for (SettingsDefinitions.VideoFileFormat format :
                    sdkModes) {
                values.add(DjiLensValuesMapping.videoFileFormat(format));
            }
        }


        if (djiLens != null &&
                (djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20T) ||
                        djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20))) {
            values.add(VideoFileFormat.MP4);
            return new ArrayList<>();
        }

        return values;
    }

    @Override
    public com.example.ugcssample.drone.camera.settings.lens.ResolutionAndFrameRate getVideoResolutionAndFrameRate() {
        return DjiLensValuesMapping.resolutionAndFrameRate(settings.videoResolutionAndFrameRate);
    }

    @Override
    public void setVideoResolutionAndFrameRate(com.example.ugcssample.drone.camera.settings.lens.ResolutionAndFrameRate resolutionAndFrameRate, Callback onSet) {
        if (djiLens != null) {
            djiLens.setVideoResolutionAndFrameRate(DjiLensValuesMapping.sdkResolutionAndFrameRate(resolutionAndFrameRate), djiError -> {
                if (djiError == null) {
                    setResolutionAndFrameRateInternal(DjiLensValuesMapping.sdkResolutionAndFrameRate(resolutionAndFrameRate));
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }

        if (djiSingleLensCamera != null) {
            djiSingleLensCamera.setVideoResolutionAndFrameRate(DjiLensValuesMapping.sdkResolutionAndFrameRate(resolutionAndFrameRate), djiError -> {
                if (djiError == null) {
                    setResolutionAndFrameRateInternal(DjiLensValuesMapping.sdkResolutionAndFrameRate(resolutionAndFrameRate));
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }
    }

    @Override
    public void addResolutionAndFrameRateChangeListener(@NonNull ValueChangeListener<com.example.ugcssample.drone.camera.settings.lens.ResolutionAndFrameRate> listener) {
        synchronized (resolutionAndFrameRateChangeListeners) {
            resolutionAndFrameRateChangeListeners.add(listener);
        }
    }

    @Override
    public void removeResolutionAndFrameRateChangeListener(@NonNull ValueChangeListener<com.example.ugcssample.drone.camera.settings.lens.ResolutionAndFrameRate> listener) {
        synchronized (resolutionAndFrameRateChangeListeners) {
            resolutionAndFrameRateChangeListeners.remove(listener);
        }
    }

    @NonNull
    @Override
    public List<com.example.ugcssample.drone.camera.settings.lens.ResolutionAndFrameRate> getSupportedVideoResolutionAndFrameRate() {

        if (djiLens != null) {
            if ((djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20T)
                    || djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20))
                    && djiLens.getType() == SettingsDefinitions.LensType.ZOOM) {
                ArrayList<com.example.ugcssample.drone.camera.settings.lens.ResolutionAndFrameRate> range = new ArrayList<>();
                range.add(new com.example.ugcssample.drone.camera.settings.lens.ResolutionAndFrameRate(VideoResolution.RESOLUTION_3840x2160, VideoFrameRate.FRAME_RATE_30_FPS));
                range.add(new com.example.ugcssample.drone.camera.settings.lens.ResolutionAndFrameRate(VideoResolution.RESOLUTION_1920x1080, VideoFrameRate.FRAME_RATE_30_FPS));
                return range;
            }
        }

        if (djiSingleLensCamera != null) {
            ResolutionAndFrameRate[] sdkRange = djiSingleLensCamera.getCapabilities().videoResolutionAndFrameRateRange();
            if (sdkRange == null) {
                Timber.w("Video resolution and frame rate range is null");
                return new ArrayList<>();
            }
            ArrayList<com.example.ugcssample.drone.camera.settings.lens.ResolutionAndFrameRate> range = new ArrayList<>();
            for (ResolutionAndFrameRate sdkValue :
                    sdkRange) {
                range.add(DjiLensValuesMapping.resolutionAndFrameRate(sdkValue));
            }
            return range;
        }
        return new ArrayList<>();
    }

    @Override
    public VideoStandard getVideoStandard() {
        return DjiLensValuesMapping.videoStandard(settings.videoStandard);
    }

    @Override
    public void setVideoStandard(@NonNull VideoStandard videoStandard, Callback onSet) {
        if (djiLens != null) {
            djiLens.setVideoStandard(DjiLensValuesMapping.sdkVideoStandard(videoStandard), djiError -> {
                if (djiError == null) {
                    setVideoStandardInternal(DjiLensValuesMapping.sdkVideoStandard(videoStandard));
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }

        if (djiSingleLensCamera != null) {
            djiSingleLensCamera.setVideoStandard(DjiLensValuesMapping.sdkVideoStandard(videoStandard), djiError -> {
                if (djiError == null) {
                    setVideoStandardInternal(DjiLensValuesMapping.sdkVideoStandard(videoStandard));
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }
    }

    @Override
    public void addVideoStandardChangeListener(@NonNull ValueChangeListener<VideoStandard> listener) {
        synchronized (videoStandardChangeListeners) {
            videoStandardChangeListeners.add(listener);
        }
    }

    @Override
    public void removeVideoStandardChangeListener(@NonNull ValueChangeListener<VideoStandard> listener) {
        synchronized (videoStandardChangeListeners) {
            videoStandardChangeListeners.remove(listener);
        }
    }

    @Override
    public @NotNull List<VideoStandard> getSupportedVideoStandards() {
        ArrayList<VideoStandard> values = new ArrayList<>();
        if (djiSingleLensCamera != null) {
            KeyManager km = KeyManager.getInstance();
            CameraKey djiKey = CameraKey.create(CameraKey.VIDEO_STANDARD_RANGE);
            if (djiKey == null) {
                Timber.e("Can't create CameraKey VIDEO_FILE_FORMAT_RANGE.");
                return new ArrayList<>();
            }
            SettingsDefinitions.VideoStandard[] sdkValues = (SettingsDefinitions.VideoStandard[]) km.getValue(djiKey);
            if (sdkValues == null)
                return new ArrayList<>();
            for (SettingsDefinitions.VideoStandard sdkValue :
                    sdkValues) {
                values.add(DjiLensValuesMapping.videoStandard(sdkValue));
            }
        }

        if (djiLens != null &&
                (djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20T) ||
                        djiMultiLensCamera.getDisplayName().equals(Camera.DisplayNameZenmuseH20))) {
            values.add(VideoStandard.PAL);
            return new ArrayList<>();
        }

        return values;
    }

    @Override
    public WhiteBalance getWhiteBalance() {
        return DjiLensValuesMapping.whiteBalance(settings.whiteBalance);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public CompletableFuture<Void> setWhiteBalance(@NonNull WhiteBalance whiteBalance) {
        dji.common.camera.WhiteBalance sdkWhiteBalance = DjiLensValuesMapping.sdkWhiteBalance(whiteBalance);
        if (djiLens != null) {
            return DjiToFutureAdapter.getFuture(djiLens::setWhiteBalance,sdkWhiteBalance);
        }
    
        if (djiSingleLensCamera != null) {
            return DjiToFutureAdapter.getFuture(djiSingleLensCamera::setWhiteBalance,sdkWhiteBalance);
        }
        return null;
    }

    @Override
    public void addWhiteBalanceListener(@NonNull ValueChangeListener<WhiteBalance> listener) {
        synchronized (whiteBalanceChangeListeners) {
            whiteBalanceChangeListeners.add(listener);
        }
    }

    @Override
    public void removeWhiteBalanceListener(@NonNull ValueChangeListener<WhiteBalance> listener) {
        synchronized (whiteBalanceChangeListeners) {
            whiteBalanceChangeListeners.remove(listener);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public CompletableFuture<PointF> getFocusTarget() {
        if (djiLens != null) {
            return DjiToFutureAdapter.getFuture(djiLens::getFocusTarget);
        }
        if (djiSingleLensCamera != null) {
            return DjiToFutureAdapter.getFuture(djiSingleLensCamera::getFocusTarget);
        }
        return null;
    }

    @Override
    public void setFocusTarget(@NonNull PointF focusTarget, Callback onSet) {
        if (djiLens != null) {
            djiLens.setFocusTarget(focusTarget, djiError -> {
                if (djiError == null) {
                    setFocusTargetInternal(focusTarget);
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }

        if (djiSingleLensCamera != null) {
            djiSingleLensCamera.setFocusTarget(focusTarget, djiError -> {
                if (djiError == null) {
                    setFocusTargetInternal(focusTarget);
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }
    }

    @Override
    public void addFocusTargetListener(@NonNull ValueChangeListener<PointF> listener) {
        synchronized (focusTargetChangeListeners) {
            focusTargetChangeListeners.add(listener);
        }
    }

    @Override
    public void removeFocusTargetListener(@NonNull ValueChangeListener<PointF> listener) {
        synchronized (focusTargetChangeListeners) {
            focusTargetChangeListeners.remove(listener);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public CompletableFuture<Integer> getFocusRingValue() {
        if (djiLens != null) {
            return DjiToFutureAdapter.getFuture(djiLens::getFocusRingValue);
        }
        if (djiSingleLensCamera != null) {
            return DjiToFutureAdapter.getFuture(djiSingleLensCamera::getFocusRingValue);
        }
        return null;
    }

    @Override
    public Integer getFocusRingMax() {
        return settings.focusRingValueMax;
    }

    @Override
    public FocusAssistantSettings getFocusAssistantSettings() {
        return DjiLensValuesMapping.focusAssistantSettings(settings.focusAssistantSettings);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public CompletableFuture<Void> setFocusAssistantSettings(@NonNull FocusAssistantSettings focusAssistantSettings) {
        dji.common.camera.FocusAssistantSettings sdkFocusAssistantSettings = DjiLensValuesMapping.sdkFocusAssistantSettings(focusAssistantSettings);
        if (djiLens != null) {
            return DjiToFutureAdapter.getFuture(djiLens::setFocusAssistantSettings, sdkFocusAssistantSettings);
        }

        if (djiSingleLensCamera != null) {
            return DjiToFutureAdapter.getFuture(djiSingleLensCamera::setFocusAssistantSettings, sdkFocusAssistantSettings);
        }
        return null;
    }

    @Override
    public void addFocusAssistantSettingsListener(@NonNull ValueChangeListener<FocusAssistantSettings> listener) {
        synchronized (focusAssistantSettingsChangeListeners) {
            focusAssistantSettingsChangeListeners.add(listener);
        }
    }

    @Override
    public void removeFocusAssistantSettingsListener(@NonNull ValueChangeListener<FocusAssistantSettings> listener) {
        synchronized (focusAssistantSettingsChangeListeners) {
            focusAssistantSettingsChangeListeners.add(listener);
        }
    }

    @Override
    public void setFocusRingValue(@NonNull Integer focusRingValue, Callback onSet) {
        if (djiLens != null) {
            djiLens.setFocusRingValue(focusRingValue, djiError -> {
                if (djiError == null) {
                    setFocusRingValueInternal(focusRingValue);
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }

        if (djiSingleLensCamera != null) {
            djiSingleLensCamera.setFocusRingValue(focusRingValue, djiError -> {
                if (djiError == null) {
                    setFocusRingValueInternal(focusRingValue);
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }
    }

    @Override
    public void addFocusRingListener(@NonNull ValueChangeListener<Integer> listener) {
        synchronized (focusRingValueChangeListeners) {
            focusRingValueChangeListeners.add(listener);
        }
    }

    @Override
    public void removeFocusRingListener(@NonNull ValueChangeListener<Integer> listener) {
        synchronized (focusRingValueChangeListeners) {
            focusRingValueChangeListeners.remove(listener);
        }
    }

    @Override
    public boolean isAdjustableFocalPointCanBeChanged() {
        if (djiLens != null) {
            return djiLens.isAdjustableFocalPointSupported();
        }

        if (djiSingleLensCamera != null) {
            String cameraName = djiSingleLensCamera.getDisplayName();
            return djiSingleLensCamera.isAdjustableFocalPointSupported() || Camera.DisplayNameZ30.equals(cameraName) || Camera.DisplayNameXT2_VL.equals(cameraName);
        }
        return false;
    }

    @Override
    public boolean isFocusAssistantEnabled() {
        return settings.focusAssistantSettings != null && (settings.focusAssistantSettings.isEnabledAF() || settings.focusAssistantSettings.isEnabledMF());
    }

    @Override
    public void setThermalIsothermLowerValue(int val, Callback onSet) {
        if (djiLens != null) {
            if (djiLens.getType() != SettingsDefinitions.LensType.INFRARED_THERMAL) {
                if (onSet != null) {
                    onSet.run(new Exception("Lens is not thermal"));
                }
                return;
            }
            djiLens.setThermalIsothermLowerValue(val, djiError -> {
                if (djiError == null) {
                    settings.thermalIsothermLowerValue = val;
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }

        if (djiSingleLensCamera != null) {
            if (!djiSingleLensCamera.isThermalCamera()) {
                if (onSet != null) {
                    onSet.run(new Exception("Lens is not thermal"));
                }
                return;
            }
            djiSingleLensCamera.setThermalIsothermLowerValue(val, djiError -> {
                if (djiError == null) {
                    settings.thermalIsothermLowerValue = val;
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }

            });
        }
    }

    @Override
    public void setThermalIsothermUpperValue(int val, Callback onSet) {
        if (djiLens != null) {
            if (djiLens.getType() != SettingsDefinitions.LensType.INFRARED_THERMAL) {
                if (onSet != null) {
                    onSet.run(new Exception("Lens is not thermal"));
                }
                return;
            }
            djiLens.setThermalIsothermUpperValue(val, djiError -> {
                if (djiError == null) {
                    settings.thermalIsothermUpperValue = val;
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }

        if (djiSingleLensCamera != null) {
            if (!djiSingleLensCamera.isThermalCamera()) {
                if (onSet != null) {
                    onSet.run(new Exception("Lens is not thermal"));
                }
                return;
            }
            djiSingleLensCamera.setThermalIsothermUpperValue(val, djiError -> {
                if (djiError == null) {
                    settings.thermalIsothermUpperValue = val;
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public CompletableFuture<Integer> getThermalIsothermLowerValue() {
        if (djiLens != null) {
            return DjiToFutureAdapter.getFuture(djiLens::getThermalIsothermLowerValue);
        }
        if (djiSingleLensCamera != null) {
            return DjiToFutureAdapter.getFuture(djiSingleLensCamera::getThermalIsothermLowerValue);
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public CompletableFuture<Integer> getThermalIsothermUpperValue() {
        if (djiLens != null) {
            return DjiToFutureAdapter.getFuture(djiLens::getThermalIsothermUpperValue);
        }
        if (djiSingleLensCamera != null) {
            return DjiToFutureAdapter.getFuture(djiSingleLensCamera::getThermalIsothermUpperValue);
        }
        return null;
    }

    @Override
    public Integer getThermalIsothermMaxValue() {
        return settings.thermalIsothermMaxValue;
    }

    @Override
    public Integer getThermalIsothermMinValue() {
        return settings.thermalIsothermMinValue;
    }

    @Override
    public void setThermalIsothermEnabled(boolean enabled, Callback onSet) {
        if (djiLens != null) {
            if (djiLens.getType() != SettingsDefinitions.LensType.INFRARED_THERMAL) {
                if (onSet != null) {
                    onSet.run(new Exception("Lens is not thermal"));
                }
                return;
            }
            djiLens.setThermalIsothermEnabled(enabled, djiError -> {
                if (djiError == null) {
                    settings.thermalIsothermEnabled = enabled;
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }

        if (djiSingleLensCamera != null) {
            if (!djiSingleLensCamera.isThermalCamera()) {
                if (onSet != null) {
                    onSet.run(new Exception("Lens is not thermal"));
                }
                return;
            }
            djiSingleLensCamera.setThermalIsothermEnabled(enabled, djiError -> {
                if (djiError == null) {
                    settings.thermalIsothermEnabled = enabled;
                    if (onSet != null) {
                        onSet.run(null);
                    }
                } else {
                    if (onSet != null) {
                        onSet.run(new Exception(djiError.getDescription()));
                    }
                }
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public CompletableFuture<Boolean> getThermalIsothermEnabled() {
        if(djiLens != null) {
            return DjiToFutureAdapter.getFuture(djiLens::getThermalIsothermEnabled);
        }
        if (djiSingleLensCamera != null) {
            return DjiToFutureAdapter.getFuture(djiSingleLensCamera::getThermalIsothermEnabled);
        }
        return null;
//        return settings.thermalIsothermEnabled;
    }

    @Override
    public void addOnInitialisedListener(@NonNull Callback listener) {
        if (isInitialised)
            listener.run(null);
        synchronized (initialisedListeners) {
            initialisedListeners.add(listener);
        }
    }

    @Override
    public void removeOnInitialisedListener(@NonNull Callback listener) {
        synchronized (initialisedListeners) {
            initialisedListeners.remove(listener);
        }
    }

    private void onIsInitialisedChanged() {
        synchronized (initialisedListeners) {
            for (Callback listener :
                    initialisedListeners) {
                listener.run(null);
            }
        }
    }

    private boolean calculateIsInitialised() {
        boolean inited = settings.apertureInited;
        inited &= settings.isoInited;
        inited &= settings.antiFlickerInited;
        inited &= settings.displayModeInited;
        inited &= settings.exposureCompensationInited;
        inited &= settings.exposureModeInited;
        inited &= settings.focusModeInited;
        inited &= settings.focusTargetInited;
        inited &= settings.meteringModeInited;
        inited &= settings.shutterSpeedInited;
        inited &= settings.videoStandardInited;
        inited &= settings.whiteBalanceInited;
        inited &= settings.focusAssistantSettingsInited;
        inited &= settings.focusRingValueInited;
        inited &= settings.photoAspectRatioInited;
        inited &= settings.photoFileFormatInited;
        inited &= settings.thermalIsothermEnabledInited;
        inited &= settings.videoFileFormatInited;
        inited &= settings.focusRingValueMaxInited;
        inited &= settings.thermalIsothermLowerValueInited;
        inited &= settings.thermalIsothermUpperValueInited;
        inited &= settings.videoResolutionAndFrameRateInited;
        inited &= settings.hybridZoomSpecInited;
        inited &= settings.opticalZoomSpecInited;
        inited &= settings.opticalZoomFocalLengthInited;
        inited &= settings.hybridZoomFocalLengthInited;
        return inited;
    }

    @Override
    public boolean isInitialised() {
        synchronized (initLock) {
            return isInitialised;
        }
    }

    @Override
    public boolean isOpticalZoomSupported() {
        if (djiLens != null)
            return djiLens.isOpticalZoomSupported();

        if (djiSingleLensCamera != null)
            return djiSingleLensCamera.isOpticalZoomSupported();

        return false;
    }

    @Override
    public boolean isHybridZoomSupported() {
        if (djiLens != null)
            return djiLens.isHybridZoomSupported();

        if (djiSingleLensCamera != null)
            return djiSingleLensCamera.isHybridZoomSupported();

        return false;
    }

    @Override
    public OpticalZoomSpec getOpticalZoomSpec() {
        return DjiLensValuesMapping.opticalZoomSpec(settings.opticalZoomSpec);
    }

    @Override
    public HybridZoomSpec getHybridZoomSpec() {
        return DjiLensValuesMapping.hybridZoomSpec(settings.hybridZoomSpec);
    }

    @Override
    public void startContiniousOpticalZoom(ZoomDirection zoomDirection, double zoomSpeed, Callback onStart) {
        if (djiLens != null) {
            djiLens.startContinuousOpticalZoom(DjiLensValuesMapping.sdkZoomDirection(zoomDirection), DjiLensValuesMapping.sdkZoomSpeed(zoomSpeed), djiError -> {
                if (djiError != null) {
                    onStart.run(new Exception(djiError.getDescription()));
                } else {
                    onStart.run(null);
                }
            });
        }

        if (djiSingleLensCamera != null) {
            djiSingleLensCamera.startContinuousOpticalZoom(DjiLensValuesMapping.sdkZoomDirection(zoomDirection), DjiLensValuesMapping.sdkZoomSpeed(zoomSpeed), djiError -> {
                if (djiError != null) {
                    onStart.run(new Exception(djiError.getDescription()));
                } else {
                    onStart.run(null);
                }
            });
        }
    }

    @Override
    public void stopContiniousOpticalZoom(Callback onStop) {
        if (djiLens != null) {
            djiLens.stopContinuousOpticalZoom(djiError -> {
                if (djiError != null) {
                    onStop.run(new Exception(djiError.getDescription()));
                } else {
                    onStop.run(null);
                }
            });
        }

        if (djiSingleLensCamera != null) {
            djiSingleLensCamera.stopContinuousOpticalZoom(djiError -> {
                if (djiError != null) {
                    onStop.run(new Exception(djiError.getDescription()));
                } else {
                    onStop.run(null);
                }
            });
        }
    }

    @Override
    public Integer getOpticalZoomFocalLength() {
        return settings.opticalZoomFocalLength;
    }

    @Override
    public void setOpticalZoomFocalLength(int value, Callback onSet) {
        if (djiLens != null) {
            djiLens.setOpticalZoomFocalLength(value, error -> {
                if (error != null)
                    onSet.run(new Exception(error.getDescription()));
                else
                    onSet.run(null);
            });
        }

        if (djiSingleLensCamera != null) {
            djiSingleLensCamera.setOpticalZoomFocalLength(value, error -> {
                if (error != null)
                    onSet.run(new Exception(error.getDescription()));
                else
                    onSet.run(null);
            });
        }
    }

    @Override
    public Integer getHybridZoomFocalLength() {
        return settings.hybridZoomFocalLength;
    }

    @Override
    public void setHybridZoomFocalLength(int value, Callback onSet) {
        if (djiLens != null) {
            djiLens.setHybridZoomFocalLength(value, error -> {
                if (error != null)
                    onSet.run(new Exception(error.getDescription()));
                else
                    onSet.run(null);
            });
        }

        if (djiSingleLensCamera != null) {
            djiSingleLensCamera.setHybridZoomFocalLength(value, error -> {
                if (error != null)
                    onSet.run(new Exception(error.getDescription()));
                else
                    onSet.run(null);
            });
        }
    }

    @Override
    public void zoomAtTarget(PointF point, Callback onZoom) {
        if (djiLens != null) {
            djiLens.tapZoomAtTarget(point, error -> {
                if (error != null) {
                    onZoom.run(new Exception(error.getDescription()));
                } else {
                    onZoom.run(null);
                }
            });
        }

        if (djiSingleLensCamera != null) {
            djiSingleLensCamera.tapZoomAtTarget(point, error -> {
                if (error != null) {
                    onZoom.run(new Exception(error.getDescription()));
                } else {
                    onZoom.run(null);
                }
            });
        }
    }

    @Override
    public void addOpticalFocalLengthListener(ValueChangeListener<Integer> valueChangeListener) throws UnsupportedOperationException {
        if (djiSingleLensCamera != null) {
            KeyManager km = KeyManager.getInstance();
            CameraKey djiKey = CameraKey.create(CameraKey.OPTICAL_ZOOM_FOCAL_LENGTH);
            if (djiKey == null) {
                throw new UnsupportedOperationException("Can't create CameraKey OPTICAL_ZOOM_FOCAL_LENGTH.");
            }
            km.addListener(djiKey, (oldValue, newValue) -> valueChangeListener.onChange((Integer) newValue));
        }

        if (djiLens != null) {
            throw new UnsupportedOperationException("Optical");
        }
    }

    private String getLensName(@NonNull Context context) {
        if (djiLens.getType() != null) {
            switch (djiLens.getType()) {
                case WIDE:
                    return "WIDE";
                case ZOOM:
                    return "ZOOM";
                case INFRARED_THERMAL:
                    return "INFRARED_THERMAL";
                case UNKNOWN:
                default:
                    Timber.w("There is a lens with unknown type");
                    return "There is a lens with unknown type";
            }
        } else {
            Timber.w("There is a lens without type");
            return "There is a lens without type";
        }
    }

    private void initThermalTempRange() {
        if (djiMultiLensCamera != null && djiMultiLensCamera.getDisplayName() != null) {
            if (Objects.equals(djiMultiLensCamera.getDisplayName(), Camera.DisplayNameZenmuseH20T)) {
                settings.thermalIsothermMinValue = -40;
                settings.thermalIsothermMaxValue = 550;
            }
        }

        if (djiSingleLensCamera != null && djiSingleLensCamera.getDisplayName() != null) {
            if (Objects.equals(djiSingleLensCamera.getDisplayName(), Camera.DisplayNameXT2_IR)) {
                settings.thermalIsothermMinValue = -40;
                settings.thermalIsothermMaxValue = 550;
            }
        }

        if (djiSingleLensCamera != null && djiSingleLensCamera.getDisplayName() != null) {
            if (Objects.equals(djiSingleLensCamera.getDisplayName(), Camera.DisplayNameMavic2EnterpriseDual_IR)) {
                settings.thermalIsothermMinValue = -10;
                settings.thermalIsothermMaxValue = 400;
            }
        }
    }

    private String getSingleLensName(Context context) {
        return "cam lens single lens name";
    }

    private void setAntiFlickerFrequencyInternal(SettingsDefinitions.AntiFlickerFrequency sdkValue) {
        settings.antiFlicker = sdkValue;
        synchronized (antiFlickerChangeListeners) {
            for (ValueChangeListener<AntiFlickerFrequency> listener : antiFlickerChangeListeners) {
                listener.onChange(DjiLensValuesMapping.antiFlickerFrequency(sdkValue));
            }
        }
    }

    private void setApertureInternal(SettingsDefinitions.Aperture sdkValue) {
        settings.aperture = sdkValue;
        synchronized (apertureChangeListeners) {
            for (ValueChangeListener<Aperture> listener : apertureChangeListeners) {
                listener.onChange(DjiLensValuesMapping.aperture(sdkValue));
            }
        }
    }

    private void setDisplayModeInternal(SettingsDefinitions.DisplayMode sdkValue) {
        settings.displayMode = sdkValue;
        synchronized (displayModeChangeListeners) {
            for (ValueChangeListener<DisplayMode> listener : displayModeChangeListeners) {
                listener.onChange(DjiLensValuesMapping.displayMode(sdkValue));
            }
        }
    }

    private void setExposureCompensationInternal(SettingsDefinitions.ExposureCompensation sdkValue) {
        settings.exposureCompensation = sdkValue;
        synchronized (exposureCompensationChangeListeners) {
            for (ValueChangeListener<ExposureCompensation> listener : exposureCompensationChangeListeners) {
                listener.onChange(DjiLensValuesMapping.exposureCompensation(sdkValue));
            }
        }
    }

    private void setExposureModeInternal(SettingsDefinitions.ExposureMode sdkValue) {
        settings.exposureMode = sdkValue;
        synchronized (exposureModeChangeListeners) {
            for (ValueChangeListener<ExposureMode> listener : exposureModeChangeListeners) {
                listener.onChange(DjiLensValuesMapping.exposureMode(sdkValue));
            }
        }
    }

    private void setFocusModeInternal(SettingsDefinitions.FocusMode sdkValue) {
        settings.focusMode = sdkValue;
        synchronized (focusModeChangeListeners) {
            for (ValueChangeListener<FocusMode> listener : focusModeChangeListeners) {
                listener.onChange(DjiLensValuesMapping.focusMode(sdkValue));
            }
        }
    }

    private void setIsoInternal(SettingsDefinitions.ISO sdkValue) {
        settings.iso = sdkValue;
        synchronized (isoChangeListeners) {
            for (ValueChangeListener<ISO> listener : isoChangeListeners) {
                listener.onChange(DjiLensValuesMapping.iso(sdkValue));
            }
        }
    }

    private void setMeteringModeInternal(SettingsDefinitions.MeteringMode sdkValue) {
        settings.meteringMode = sdkValue;
        synchronized (meteringModeChangeListeners) {
            for (ValueChangeListener<MeteringMode> listener : meteringModeChangeListeners) {
                listener.onChange(DjiLensValuesMapping.meteringMode(sdkValue));
            }
        }
    }

    private void setPhotoAspectRatioInternal(SettingsDefinitions.PhotoAspectRatio sdkValue) {
        settings.photoAspectRatio = sdkValue;
        synchronized (photoAspectRatioChangeListeners) {
            for (ValueChangeListener<PhotoAspectRatio> listener : photoAspectRatioChangeListeners) {
                listener.onChange(DjiLensValuesMapping.photoAspectRatio(sdkValue));
            }
        }
    }

    private void setPhotoFileFormatInternal(SettingsDefinitions.PhotoFileFormat sdkValue) {
        settings.photoFileFormat = sdkValue;
        synchronized (photoFileFormatChangeListeners) {
            for (ValueChangeListener<PhotoFileFormat> listener : photoFileFormatChangeListeners) {
                listener.onChange(DjiLensValuesMapping.photoFileFormat(sdkValue));
            }
        }
    }

    private void setShutterSpeedInternal(SettingsDefinitions.ShutterSpeed sdkValue) {
        settings.shutterSpeed = sdkValue;
        synchronized (shutterSpeedChangeListeners) {
            for (ValueChangeListener<ShutterSpeed> listener : shutterSpeedChangeListeners) {
                listener.onChange(DjiLensValuesMapping.shutterSpeed(sdkValue));
            }
        }
    }

    private void setVideoFileFormatInternal(SettingsDefinitions.VideoFileFormat sdkValue) {
        settings.videoFileFormat = sdkValue;
        synchronized (videoFileFormatChangeListeners) {
            for (ValueChangeListener<VideoFileFormat> listener : videoFileFormatChangeListeners) {
                listener.onChange(DjiLensValuesMapping.videoFileFormat(sdkValue));
            }
        }
    }

    private void setResolutionAndFrameRateInternal(ResolutionAndFrameRate sdkValue) {
        settings.videoResolutionAndFrameRate = sdkValue;
        synchronized (resolutionAndFrameRateChangeListeners) {
            for (ValueChangeListener<com.example.ugcssample.drone.camera.settings.lens.ResolutionAndFrameRate> listener : resolutionAndFrameRateChangeListeners) {
                listener.onChange(DjiLensValuesMapping.resolutionAndFrameRate(sdkValue));
            }
        }
    }

    private void setVideoStandardInternal(SettingsDefinitions.VideoStandard sdkValue) {
        settings.videoStandard = sdkValue;
        synchronized (videoStandardChangeListeners) {
            for (ValueChangeListener<VideoStandard> listener : videoStandardChangeListeners) {
                listener.onChange(DjiLensValuesMapping.videoStandard(sdkValue));
            }
        }
    }

    private void setWhiteBalanceInternal(dji.common.camera.WhiteBalance sdkValue) {
        settings.whiteBalance = sdkValue;
        synchronized (whiteBalanceChangeListeners) {
            for (ValueChangeListener<WhiteBalance> listener : whiteBalanceChangeListeners) {
                listener.onChange(DjiLensValuesMapping.whiteBalance(sdkValue));
            }
        }
    }

    private void setFocusTargetInternal(PointF value) {
        settings.focusTarget = value;
        synchronized (focusTargetChangeListeners) {
            for (ValueChangeListener<PointF> listener : focusTargetChangeListeners) {
                listener.onChange(value);
            }
        }
    }

    private void setFocusAssistantSettingsInternal(dji.common.camera.FocusAssistantSettings sdkValue) {
        settings.focusAssistantSettings = sdkValue;
        synchronized (focusAssistantSettingsChangeListeners) {
            for (ValueChangeListener<FocusAssistantSettings> listener : focusAssistantSettingsChangeListeners) {
                listener.onChange(DjiLensValuesMapping.focusAssistantSettings(sdkValue));
            }
        }
    }

    private void setFocusRingValueInternal(Integer sdkValue) {
        settings.focusRingValue = sdkValue;
        synchronized (focusRingValueChangeListeners) {
            for (ValueChangeListener<Integer> listener : focusRingValueChangeListeners) {
                listener.onChange(sdkValue);
            }
        }
    }

    private static class DjiLensValuesMapping {

        public static AntiFlickerFrequency antiFlickerFrequency(SettingsDefinitions.AntiFlickerFrequency value) {
            if (value == null) return null;
            switch (value) {
                case AUTO:
                    return AntiFlickerFrequency.AUTO;
                case MANUAL_60HZ:
                    return AntiFlickerFrequency.MANUAL_60HZ;
                case MANUAL_50HZ:
                    return AntiFlickerFrequency.MANUAL_50HZ;
                case DISABLED:
                    return AntiFlickerFrequency.DISABLED;
                case UNKNOWN:
                default:
                    return AntiFlickerFrequency.UNKNOWN;
            }
        }

        public static SettingsDefinitions.AntiFlickerFrequency sdkAntiFlickerFrequency(AntiFlickerFrequency value) {
            if (value == null) return null;
            switch (value) {
                case AUTO:
                    return SettingsDefinitions.AntiFlickerFrequency.AUTO;
                case MANUAL_60HZ:
                    return SettingsDefinitions.AntiFlickerFrequency.MANUAL_60HZ;
                case MANUAL_50HZ:
                    return SettingsDefinitions.AntiFlickerFrequency.MANUAL_50HZ;
                case DISABLED:
                    return SettingsDefinitions.AntiFlickerFrequency.DISABLED;
                case UNKNOWN:
                default:
                    return SettingsDefinitions.AntiFlickerFrequency.UNKNOWN;
            }
        }

        public static Aperture aperture(SettingsDefinitions.Aperture value) {
            if (value == null) return null;
            switch (value) {
                case F_1_DOT_6:
                    return Aperture.F_1_DOT_6;
                case F_1_DOT_7:
                    return Aperture.F_1_DOT_7;
                case F_1_DOT_8:
                    return Aperture.F_1_DOT_8;
                case F_2:
                    return Aperture.F_2;
                case F_2_DOT_2:
                    return Aperture.F_2_DOT_2;
                case F_2_DOT_4:
                    return Aperture.F_2_DOT_4;
                case F_2_DOT_5:
                    return Aperture.F_2_DOT_5;
                case F_2_DOT_6:
                    return Aperture.F_2_DOT_6;
                case F_2_DOT_8:
                    return Aperture.F_2_DOT_8;
                case F_3_DOT_2:
                    return Aperture.F_3_DOT_2;
                case F_3_DOT_4:
                    return Aperture.F_3_DOT_4;
                case F_3_DOT_5:
                    return Aperture.F_3_DOT_5;
                case F_4:
                    return Aperture.F_4;
                case F_4_DOT_5:
                    return Aperture.F_4_DOT_5;
                case F_4_DOT_8:
                    return Aperture.F_4_DOT_8;
                case F_5:
                    return Aperture.F_5;
                case F_5_DOT_6:
                    return Aperture.F_5_DOT_6;
                case F_6_DOT_3:
                    return Aperture.F_6_DOT_3;
                case F_6_DOT_8:
                    return Aperture.F_6_DOT_8;
                case F_7_DOT_1:
                    return Aperture.F_7_DOT_1;
                case F_8:
                    return Aperture.F_8;
                case F_9:
                    return Aperture.F_9;
                case F_9_DOT_6:
                    return Aperture.F_9_DOT_6;
                case F_10:
                    return Aperture.F_10;
                case F_11:
                    return Aperture.F_11;
                case F_13:
                    return Aperture.F_13;
                case F_14:
                    return Aperture.F_14;
                case F_16:
                    return Aperture.F_16;
                case F_18:
                    return Aperture.F_18;
                case F_19:
                    return Aperture.F_19;
                case F_20:
                    return Aperture.F_20;
                case F_22:
                    return Aperture.F_22;
                case UNKNOWN:
                default:
                    return Aperture.UNKNOWN;
            }
        }

        public static SettingsDefinitions.Aperture sdkAperture(Aperture value) {
            if (value == null) return null;
            switch (value) {
                case F_1_DOT_6:
                    return SettingsDefinitions.Aperture.F_1_DOT_6;
                case F_1_DOT_7:
                    return SettingsDefinitions.Aperture.F_1_DOT_7;
                case F_1_DOT_8:
                    return SettingsDefinitions.Aperture.F_1_DOT_8;
                case F_2:
                    return SettingsDefinitions.Aperture.F_2;
                case F_2_DOT_2:
                    return SettingsDefinitions.Aperture.F_2_DOT_2;
                case F_2_DOT_4:
                    return SettingsDefinitions.Aperture.F_2_DOT_4;
                case F_2_DOT_5:
                    return SettingsDefinitions.Aperture.F_2_DOT_5;
                case F_2_DOT_6:
                    return SettingsDefinitions.Aperture.F_2_DOT_6;
                case F_2_DOT_8:
                    return SettingsDefinitions.Aperture.F_2_DOT_8;
                case F_3_DOT_2:
                    return SettingsDefinitions.Aperture.F_3_DOT_2;
                case F_3_DOT_4:
                    return SettingsDefinitions.Aperture.F_3_DOT_4;
                case F_3_DOT_5:
                    return SettingsDefinitions.Aperture.F_3_DOT_5;
                case F_4:
                    return SettingsDefinitions.Aperture.F_4;
                case F_4_DOT_5:
                    return SettingsDefinitions.Aperture.F_4_DOT_5;
                case F_4_DOT_8:
                    return SettingsDefinitions.Aperture.F_4_DOT_8;
                case F_5:
                    return SettingsDefinitions.Aperture.F_5;
                case F_5_DOT_6:
                    return SettingsDefinitions.Aperture.F_5_DOT_6;
                case F_6_DOT_3:
                    return SettingsDefinitions.Aperture.F_6_DOT_3;
                case F_6_DOT_8:
                    return SettingsDefinitions.Aperture.F_6_DOT_8;
                case F_7_DOT_1:
                    return SettingsDefinitions.Aperture.F_7_DOT_1;
                case F_8:
                    return SettingsDefinitions.Aperture.F_8;
                case F_9:
                    return SettingsDefinitions.Aperture.F_9;
                case F_9_DOT_6:
                    return SettingsDefinitions.Aperture.F_9_DOT_6;
                case F_10:
                    return SettingsDefinitions.Aperture.F_10;
                case F_11:
                    return SettingsDefinitions.Aperture.F_11;
                case F_13:
                    return SettingsDefinitions.Aperture.F_13;
                case F_14:
                    return SettingsDefinitions.Aperture.F_14;
                case F_16:
                    return SettingsDefinitions.Aperture.F_16;
                case F_18:
                    return SettingsDefinitions.Aperture.F_18;
                case F_19:
                    return SettingsDefinitions.Aperture.F_19;
                case F_20:
                    return SettingsDefinitions.Aperture.F_20;
                case F_22:
                    return SettingsDefinitions.Aperture.F_22;
                case UNKNOWN:
                default:
                    return SettingsDefinitions.Aperture.UNKNOWN;
            }
        }

        public static DisplayMode displayMode(SettingsDefinitions.DisplayMode value) {
            if (value == null) return null;
            switch (value) {
                case VISUAL_ONLY:
                    return DisplayMode.VISUAL_ONLY;
                case THERMAL_ONLY:
                    return DisplayMode.THERMAL_ONLY;
                case PIP:
                    return DisplayMode.PIP;
                case MSX:
                    return DisplayMode.MSX;
                case OTHER:
                    return DisplayMode.OTHER;
                default:
                    return DisplayMode.UNKNOWN;
            }
        }

        public static SettingsDefinitions.DisplayMode sdkDisplayMode(DisplayMode value) {
            if (value == null) return null;
            switch (value) {
                case VISUAL_ONLY:
                    return SettingsDefinitions.DisplayMode.VISUAL_ONLY;
                case THERMAL_ONLY:
                    return SettingsDefinitions.DisplayMode.THERMAL_ONLY;
                case PIP:
                    return SettingsDefinitions.DisplayMode.PIP;
                case MSX:
                    return SettingsDefinitions.DisplayMode.MSX;
                case OTHER:
                default:
                    return SettingsDefinitions.DisplayMode.OTHER;
            }
        }

        public static ExposureCompensation exposureCompensation(SettingsDefinitions.ExposureCompensation value) {
            if (value == null) return null;
            switch (value) {
                case N_5_0:
                    return ExposureCompensation.N_5_0;
                case N_4_7:
                    return ExposureCompensation.N_4_7;
                case N_4_3:
                    return ExposureCompensation.N_4_3;
                case N_4_0:
                    return ExposureCompensation.N_4_0;
                case N_3_7:
                    return ExposureCompensation.N_3_7;
                case N_3_3:
                    return ExposureCompensation.N_3_3;
                case N_3_0:
                    return ExposureCompensation.N_3_0;
                case N_2_7:
                    return ExposureCompensation.N_2_7;
                case N_2_3:
                    return ExposureCompensation.N_2_3;
                case N_2_0:
                    return ExposureCompensation.N_2_0;
                case N_1_7:
                    return ExposureCompensation.N_1_7;
                case N_1_3:
                    return ExposureCompensation.N_1_3;
                case N_1_0:
                    return ExposureCompensation.N_1_0;
                case N_0_7:
                    return ExposureCompensation.N_0_7;
                case N_0_3:
                    return ExposureCompensation.N_0_3;
                case N_0_0:
                    return ExposureCompensation.N_0_0;
                case P_0_3:
                    return ExposureCompensation.P_0_3;
                case P_0_7:
                    return ExposureCompensation.P_0_7;
                case P_1_0:
                    return ExposureCompensation.P_1_0;
                case P_1_3:
                    return ExposureCompensation.P_1_3;
                case P_1_7:
                    return ExposureCompensation.P_1_7;
                case P_2_0:
                    return ExposureCompensation.P_2_0;
                case P_2_3:
                    return ExposureCompensation.P_2_3;
                case P_2_7:
                    return ExposureCompensation.P_2_7;
                case P_3_0:
                    return ExposureCompensation.P_3_0;
                case P_3_3:
                    return ExposureCompensation.P_3_3;
                case P_3_7:
                    return ExposureCompensation.P_3_7;
                case P_4_0:
                    return ExposureCompensation.P_4_0;
                case P_4_3:
                    return ExposureCompensation.P_4_3;
                case P_4_7:
                    return ExposureCompensation.P_4_7;
                case P_5_0:
                    return ExposureCompensation.P_5_0;
                case FIXED:
                    return ExposureCompensation.FIXED;
                case UNKNOWN:
                default:
                    return ExposureCompensation.UNKNOWN;
            }
        }

        public static SettingsDefinitions.ExposureCompensation sdkExposureCompensation(ExposureCompensation value) {
            if (value == null) return null;
            switch (value) {
                case N_5_0:
                    return SettingsDefinitions.ExposureCompensation.N_5_0;
                case N_4_7:
                    return SettingsDefinitions.ExposureCompensation.N_4_7;
                case N_4_3:
                    return SettingsDefinitions.ExposureCompensation.N_4_3;
                case N_4_0:
                    return SettingsDefinitions.ExposureCompensation.N_4_0;
                case N_3_7:
                    return SettingsDefinitions.ExposureCompensation.N_3_7;
                case N_3_3:
                    return SettingsDefinitions.ExposureCompensation.N_3_3;
                case N_3_0:
                    return SettingsDefinitions.ExposureCompensation.N_3_0;
                case N_2_7:
                    return SettingsDefinitions.ExposureCompensation.N_2_7;
                case N_2_3:
                    return SettingsDefinitions.ExposureCompensation.N_2_3;
                case N_2_0:
                    return SettingsDefinitions.ExposureCompensation.N_2_0;
                case N_1_7:
                    return SettingsDefinitions.ExposureCompensation.N_1_7;
                case N_1_3:
                    return SettingsDefinitions.ExposureCompensation.N_1_3;
                case N_1_0:
                    return SettingsDefinitions.ExposureCompensation.N_1_0;
                case N_0_7:
                    return SettingsDefinitions.ExposureCompensation.N_0_7;
                case N_0_3:
                    return SettingsDefinitions.ExposureCompensation.N_0_3;
                case N_0_0:
                    return SettingsDefinitions.ExposureCompensation.N_0_0;
                case P_0_3:
                    return SettingsDefinitions.ExposureCompensation.P_0_3;
                case P_0_7:
                    return SettingsDefinitions.ExposureCompensation.P_0_7;
                case P_1_0:
                    return SettingsDefinitions.ExposureCompensation.P_1_0;
                case P_1_3:
                    return SettingsDefinitions.ExposureCompensation.P_1_3;
                case P_1_7:
                    return SettingsDefinitions.ExposureCompensation.P_1_7;
                case P_2_0:
                    return SettingsDefinitions.ExposureCompensation.P_2_0;
                case P_2_3:
                    return SettingsDefinitions.ExposureCompensation.P_2_3;
                case P_2_7:
                    return SettingsDefinitions.ExposureCompensation.P_2_7;
                case P_3_0:
                    return SettingsDefinitions.ExposureCompensation.P_3_0;
                case P_3_3:
                    return SettingsDefinitions.ExposureCompensation.P_3_3;
                case P_3_7:
                    return SettingsDefinitions.ExposureCompensation.P_3_7;
                case P_4_0:
                    return SettingsDefinitions.ExposureCompensation.P_4_0;
                case P_4_3:
                    return SettingsDefinitions.ExposureCompensation.P_4_3;
                case P_4_7:
                    return SettingsDefinitions.ExposureCompensation.P_4_7;
                case P_5_0:
                    return SettingsDefinitions.ExposureCompensation.P_5_0;
                case FIXED:
                    return SettingsDefinitions.ExposureCompensation.FIXED;
                case UNKNOWN:
                default:
                    return SettingsDefinitions.ExposureCompensation.UNKNOWN;
            }
        }

        public static ExposureMode exposureMode(SettingsDefinitions.ExposureMode value) {
            if (value == null) return null;
            switch (value) {
                case PROGRAM:
                    return ExposureMode.PROGRAM;
                case SHUTTER_PRIORITY:
                    return ExposureMode.SHUTTER_PRIORITY;
                case APERTURE_PRIORITY:
                    return ExposureMode.APERTURE_PRIORITY;
                case MANUAL:
                    return ExposureMode.MANUAL;
                case CINE:
                    return ExposureMode.CINE;
                case UNKNOWN:
                default:
                    return ExposureMode.UNKNOWN;
            }
        }

        public static SettingsDefinitions.ExposureMode sdkExposureMode(ExposureMode value) {
            if (value == null) return null;
            switch (value) {
                case PROGRAM:
                    return SettingsDefinitions.ExposureMode.PROGRAM;
                case SHUTTER_PRIORITY:
                    return SettingsDefinitions.ExposureMode.SHUTTER_PRIORITY;
                case APERTURE_PRIORITY:
                    return SettingsDefinitions.ExposureMode.APERTURE_PRIORITY;
                case MANUAL:
                    return SettingsDefinitions.ExposureMode.MANUAL;
                case CINE:
                    return SettingsDefinitions.ExposureMode.CINE;
                case UNKNOWN:
                default:
                    return SettingsDefinitions.ExposureMode.UNKNOWN;
            }
        }

        public static FocusMode focusMode(SettingsDefinitions.FocusMode value) {
            if (value == null) return null;
            switch (value) {
                case MANUAL:
                    return FocusMode.MANUAL;
                case AUTO:
                    return FocusMode.AUTO;
                case AFC:
                    return FocusMode.AFC;
                case UNKNOWN:
                default:
                    return FocusMode.UNKNOWN;
            }
        }

        public static SettingsDefinitions.FocusMode sdkFocusMode(FocusMode value) {
            if (value == null) return null;
            switch (value) {
                case MANUAL:
                    return SettingsDefinitions.FocusMode.MANUAL;
                case AUTO:
                    return SettingsDefinitions.FocusMode.AUTO;
                case AFC:
                    return SettingsDefinitions.FocusMode.AFC;
                case UNKNOWN:
                default:
                    return SettingsDefinitions.FocusMode.UNKNOWN;
            }
        }

        public static ISO iso(SettingsDefinitions.ISO value) {
            if (value == null) return null;
            switch (value) {
                case AUTO:
                    return ISO.AUTO;
                case ISO_50:
                    return ISO.ISO_50;
                case ISO_100:
                    return ISO.ISO_100;
                case ISO_200:
                    return ISO.ISO_200;
                case ISO_400:
                    return ISO.ISO_400;
                case ISO_800:
                    return ISO.ISO_800;
                case ISO_1600:
                    return ISO.ISO_1600;
                case ISO_3200:
                    return ISO.ISO_3200;
                case ISO_6400:
                    return ISO.ISO_6400;
                case ISO_12800:
                    return ISO.ISO_12800;
                case ISO_25600:
                    return ISO.ISO_25600;
                case FIXED:
                    return ISO.FIXED;
                case UNKNOWN:
                default:
                    return ISO.UNKNOWN;
            }
        }

        public static SettingsDefinitions.ISO sdkIso(ISO value) {
            if (value == null) return null;
            switch (value) {
                case AUTO:
                    return SettingsDefinitions.ISO.AUTO;
                case ISO_50:
                    return SettingsDefinitions.ISO.ISO_50;
                case ISO_100:
                    return SettingsDefinitions.ISO.ISO_100;
                case ISO_200:
                    return SettingsDefinitions.ISO.ISO_200;
                case ISO_400:
                    return SettingsDefinitions.ISO.ISO_400;
                case ISO_800:
                    return SettingsDefinitions.ISO.ISO_800;
                case ISO_1600:
                    return SettingsDefinitions.ISO.ISO_1600;
                case ISO_3200:
                    return SettingsDefinitions.ISO.ISO_3200;
                case ISO_6400:
                    return SettingsDefinitions.ISO.ISO_6400;
                case ISO_12800:
                    return SettingsDefinitions.ISO.ISO_12800;
                case ISO_25600:
                    return SettingsDefinitions.ISO.ISO_25600;
                case FIXED:
                    return SettingsDefinitions.ISO.FIXED;
                case UNKNOWN:
                default:
                    return SettingsDefinitions.ISO.UNKNOWN;
            }
        }

        public static MeteringMode meteringMode(SettingsDefinitions.MeteringMode value) {
            if (value == null) return null;
            switch (value) {
                case CENTER:
                    return MeteringMode.CENTER;
                case AVERAGE:
                    return MeteringMode.AVERAGE;
                case SPOT:
                    return MeteringMode.SPOT;
                case UNKNOWN:
                default:
                    return MeteringMode.UNKNOWN;
            }
        }

        public static SettingsDefinitions.MeteringMode sdkMeteringMode(MeteringMode value) {
            if (value == null) return null;
            switch (value) {
                case CENTER:
                    return SettingsDefinitions.MeteringMode.CENTER;
                case AVERAGE:
                    return SettingsDefinitions.MeteringMode.AVERAGE;
                case SPOT:
                    return SettingsDefinitions.MeteringMode.SPOT;
                case UNKNOWN:
                default:
                    return SettingsDefinitions.MeteringMode.UNKNOWN;
            }
        }

        public static PhotoAspectRatio photoAspectRatio(SettingsDefinitions.PhotoAspectRatio value) {
            if (value == null) return null;
            switch (value) {
                case RATIO_4_3:
                    return PhotoAspectRatio.RATIO_4_3;
                case RATIO_16_9:
                    return PhotoAspectRatio.RATIO_16_9;
                case RATIO_3_2:
                    return PhotoAspectRatio.RATIO_3_2;
                case UNKNOWN:
                default:
                    return PhotoAspectRatio.UNKNOWN;
            }
        }

        public static SettingsDefinitions.PhotoAspectRatio sdkPhotoAspectRatio(PhotoAspectRatio value) {
            if (value == null) return null;
            switch (value) {
                case RATIO_4_3:
                    return SettingsDefinitions.PhotoAspectRatio.RATIO_4_3;
                case RATIO_16_9:
                    return SettingsDefinitions.PhotoAspectRatio.RATIO_16_9;
                case RATIO_3_2:
                    return SettingsDefinitions.PhotoAspectRatio.RATIO_3_2;
                case UNKNOWN:
                default:
                    return SettingsDefinitions.PhotoAspectRatio.UNKNOWN;
            }
        }

        public static PhotoFileFormat photoFileFormat(SettingsDefinitions.PhotoFileFormat value) {
            if (value == null) return null;
            switch (value) {
                case RAW:
                    return PhotoFileFormat.RAW;
                case JPEG:
                    return PhotoFileFormat.JPEG;
                case RAW_AND_JPEG:
                    return PhotoFileFormat.RAW_AND_JPEG;
                case TIFF_8_BIT:
                    return PhotoFileFormat.TIFF_8_BIT;
                case TIFF_14_BIT:
                    return PhotoFileFormat.TIFF_14_BIT;
                case TIFF_14_BIT_LINEAR_LOW_TEMP_RESOLUTION:
                    return PhotoFileFormat.TIFF_14_BIT_LINEAR_LOW_TEMP_RESOLUTION;
                case TIFF_14_BIT_LINEAR_HIGH_TEMP_RESOLUTION:
                    return PhotoFileFormat.TIFF_14_BIT_LINEAR_HIGH_TEMP_RESOLUTION;
                case RADIOMETRIC_JPEG:
                    return PhotoFileFormat.RADIOMETRIC_JPEG;
                case RADIOMETRIC_JPEG_LOW:
                    return PhotoFileFormat.RADIOMETRIC_JPEG_LOW;
                case RADIOMETRIC_JPEG_HIGH:
                    return PhotoFileFormat.RADIOMETRIC_JPEG_HIGH;
                case UNKNOWN:
                default:
                    return PhotoFileFormat.UNKNOWN;
            }
        }

        public static SettingsDefinitions.PhotoFileFormat sdkPhotoFileFormat(PhotoFileFormat value) {
            if (value == null) return null;
            switch (value) {
                case RAW:
                    return SettingsDefinitions.PhotoFileFormat.RAW;
                case JPEG:
                    return SettingsDefinitions.PhotoFileFormat.JPEG;
                case RAW_AND_JPEG:
                    return SettingsDefinitions.PhotoFileFormat.RAW_AND_JPEG;
                case TIFF_8_BIT:
                    return SettingsDefinitions.PhotoFileFormat.TIFF_8_BIT;
                case TIFF_14_BIT:
                    return SettingsDefinitions.PhotoFileFormat.TIFF_14_BIT;
                case TIFF_14_BIT_LINEAR_LOW_TEMP_RESOLUTION:
                    return SettingsDefinitions.PhotoFileFormat.TIFF_14_BIT_LINEAR_LOW_TEMP_RESOLUTION;
                case TIFF_14_BIT_LINEAR_HIGH_TEMP_RESOLUTION:
                    return SettingsDefinitions.PhotoFileFormat.TIFF_14_BIT_LINEAR_HIGH_TEMP_RESOLUTION;
                case RADIOMETRIC_JPEG:
                    return SettingsDefinitions.PhotoFileFormat.RADIOMETRIC_JPEG;
                case RADIOMETRIC_JPEG_LOW:
                    return SettingsDefinitions.PhotoFileFormat.RADIOMETRIC_JPEG_LOW;
                case RADIOMETRIC_JPEG_HIGH:
                    return SettingsDefinitions.PhotoFileFormat.RADIOMETRIC_JPEG_HIGH;
                case UNKNOWN:
                default:
                    return SettingsDefinitions.PhotoFileFormat.UNKNOWN;
            }
        }

        public static ShutterSpeed shutterSpeed(SettingsDefinitions.ShutterSpeed value) {
            if (value == null) return null;
            switch (value) {
                case SHUTTER_SPEED_1_20000:
                    return ShutterSpeed.SHUTTER_SPEED_1_20000;
                case SHUTTER_SPEED_1_16000:
                    return ShutterSpeed.SHUTTER_SPEED_1_16000;
                case SHUTTER_SPEED_1_12800:
                    return ShutterSpeed.SHUTTER_SPEED_1_12800;
                case SHUTTER_SPEED_1_10000:
                    return ShutterSpeed.SHUTTER_SPEED_1_10000;
                case SHUTTER_SPEED_1_8000:
                    return ShutterSpeed.SHUTTER_SPEED_1_8000;
                case SHUTTER_SPEED_1_6400:
                    return ShutterSpeed.SHUTTER_SPEED_1_6400;
                case SHUTTER_SPEED_1_6000:
                    return ShutterSpeed.SHUTTER_SPEED_1_6000;
                case SHUTTER_SPEED_1_5000:
                    return ShutterSpeed.SHUTTER_SPEED_1_5000;
                case SHUTTER_SPEED_1_4000:
                    return ShutterSpeed.SHUTTER_SPEED_1_4000;
                case SHUTTER_SPEED_1_3200:
                    return ShutterSpeed.SHUTTER_SPEED_1_3200;
                case SHUTTER_SPEED_1_3000:
                    return ShutterSpeed.SHUTTER_SPEED_1_3000;
                case SHUTTER_SPEED_1_2500:
                    return ShutterSpeed.SHUTTER_SPEED_1_2500;
                case SHUTTER_SPEED_1_2000:
                    return ShutterSpeed.SHUTTER_SPEED_1_2000;
                case SHUTTER_SPEED_1_1600:
                    return ShutterSpeed.SHUTTER_SPEED_1_1600;
                case SHUTTER_SPEED_1_1500:
                    return ShutterSpeed.SHUTTER_SPEED_1_1500;
                case SHUTTER_SPEED_1_1250:
                    return ShutterSpeed.SHUTTER_SPEED_1_1250;
                case SHUTTER_SPEED_1_1000:
                    return ShutterSpeed.SHUTTER_SPEED_1_1000;
                case SHUTTER_SPEED_1_800:
                    return ShutterSpeed.SHUTTER_SPEED_1_800;
                case SHUTTER_SPEED_1_725:
                    return ShutterSpeed.SHUTTER_SPEED_1_725;
                case SHUTTER_SPEED_1_640:
                    return ShutterSpeed.SHUTTER_SPEED_1_640;
                case SHUTTER_SPEED_1_500:
                    return ShutterSpeed.SHUTTER_SPEED_1_500;
                case SHUTTER_SPEED_1_400:
                    return ShutterSpeed.SHUTTER_SPEED_1_400;
                case SHUTTER_SPEED_1_350:
                    return ShutterSpeed.SHUTTER_SPEED_1_350;
                case SHUTTER_SPEED_1_320:
                    return ShutterSpeed.SHUTTER_SPEED_1_320;
                case SHUTTER_SPEED_1_250:
                    return ShutterSpeed.SHUTTER_SPEED_1_250;
                case SHUTTER_SPEED_1_240:
                    return ShutterSpeed.SHUTTER_SPEED_1_240;
                case SHUTTER_SPEED_1_200:
                    return ShutterSpeed.SHUTTER_SPEED_1_200;
                case SHUTTER_SPEED_1_180:
                    return ShutterSpeed.SHUTTER_SPEED_1_180;
                case SHUTTER_SPEED_1_160:
                    return ShutterSpeed.SHUTTER_SPEED_1_160;
                case SHUTTER_SPEED_1_125:
                    return ShutterSpeed.SHUTTER_SPEED_1_125;
                case SHUTTER_SPEED_1_120:
                    return ShutterSpeed.SHUTTER_SPEED_1_120;
                case SHUTTER_SPEED_1_100:
                    return ShutterSpeed.SHUTTER_SPEED_1_100;
                case SHUTTER_SPEED_1_90:
                    return ShutterSpeed.SHUTTER_SPEED_1_90;
                case SHUTTER_SPEED_1_80:
                    return ShutterSpeed.SHUTTER_SPEED_1_80;
                case SHUTTER_SPEED_1_60:
                    return ShutterSpeed.SHUTTER_SPEED_1_60;
                case SHUTTER_SPEED_1_50:
                    return ShutterSpeed.SHUTTER_SPEED_1_50;
                case SHUTTER_SPEED_1_40:
                    return ShutterSpeed.SHUTTER_SPEED_1_40;
                case SHUTTER_SPEED_1_30:
                    return ShutterSpeed.SHUTTER_SPEED_1_30;
                case SHUTTER_SPEED_1_25:
                    return ShutterSpeed.SHUTTER_SPEED_1_25;
                case SHUTTER_SPEED_1_20:
                    return ShutterSpeed.SHUTTER_SPEED_1_20;
                case SHUTTER_SPEED_1_15:
                    return ShutterSpeed.SHUTTER_SPEED_1_15;
                case SHUTTER_SPEED_1_12_DOT_5:
                    return ShutterSpeed.SHUTTER_SPEED_1_12_DOT_5;
                case SHUTTER_SPEED_1_10:
                    return ShutterSpeed.SHUTTER_SPEED_1_10;
                case SHUTTER_SPEED_1_8:
                    return ShutterSpeed.SHUTTER_SPEED_1_8;
                case SHUTTER_SPEED_1_6_DOT_25:
                    return ShutterSpeed.SHUTTER_SPEED_1_6_DOT_25;
                case SHUTTER_SPEED_1_5:
                    return ShutterSpeed.SHUTTER_SPEED_1_5;
                case SHUTTER_SPEED_1_4:
                    return ShutterSpeed.SHUTTER_SPEED_1_4;
                case SHUTTER_SPEED_1_3:
                    return ShutterSpeed.SHUTTER_SPEED_1_3;
                case SHUTTER_SPEED_1_2_DOT_5:
                    return ShutterSpeed.SHUTTER_SPEED_1_2_DOT_5;
                case SHUTTER_SPEED_1_2:
                    return ShutterSpeed.SHUTTER_SPEED_1_2;
                case SHUTTER_SPEED_1_1_DOT_67:
                    return ShutterSpeed.SHUTTER_SPEED_1_1_DOT_67;
                case SHUTTER_SPEED_1_1_DOT_25:
                    return ShutterSpeed.SHUTTER_SPEED_1_1_DOT_25;
                case SHUTTER_SPEED_1:
                    return ShutterSpeed.SHUTTER_SPEED_1;
                case SHUTTER_SPEED_1_DOT_3:
                    return ShutterSpeed.SHUTTER_SPEED_1_DOT_3;
                case SHUTTER_SPEED_1_DOT_6:
                    return ShutterSpeed.SHUTTER_SPEED_1_DOT_6;
                case SHUTTER_SPEED_2:
                    return ShutterSpeed.SHUTTER_SPEED_2;
                case SHUTTER_SPEED_2_DOT_5:
                    return ShutterSpeed.SHUTTER_SPEED_2_DOT_5;
                case SHUTTER_SPEED_3:
                    return ShutterSpeed.SHUTTER_SPEED_3;
                case SHUTTER_SPEED_3_DOT_2:
                    return ShutterSpeed.SHUTTER_SPEED_3_DOT_2;
                case SHUTTER_SPEED_4:
                    return ShutterSpeed.SHUTTER_SPEED_4;
                case SHUTTER_SPEED_5:
                    return ShutterSpeed.SHUTTER_SPEED_5;
                case SHUTTER_SPEED_6:
                    return ShutterSpeed.SHUTTER_SPEED_6;
                case SHUTTER_SPEED_7:
                    return ShutterSpeed.SHUTTER_SPEED_7;
                case SHUTTER_SPEED_8:
                    return ShutterSpeed.SHUTTER_SPEED_8;
                case SHUTTER_SPEED_9:
                    return ShutterSpeed.SHUTTER_SPEED_9;
                case SHUTTER_SPEED_10:
                    return ShutterSpeed.SHUTTER_SPEED_10;
                case SHUTTER_SPEED_13:
                    return ShutterSpeed.SHUTTER_SPEED_13;
                case SHUTTER_SPEED_15:
                    return ShutterSpeed.SHUTTER_SPEED_15;
                case SHUTTER_SPEED_20:
                    return ShutterSpeed.SHUTTER_SPEED_20;
                case SHUTTER_SPEED_25:
                    return ShutterSpeed.SHUTTER_SPEED_25;
                case SHUTTER_SPEED_30:
                    return ShutterSpeed.SHUTTER_SPEED_30;
                case UNKNOWN:
                default:
                    return ShutterSpeed.UNKNOWN;
            }
        }

        public static SettingsDefinitions.ShutterSpeed sdkShutterSpeed(ShutterSpeed value) {
            if (value == null) return null;
            switch (value) {
                case SHUTTER_SPEED_1_20000:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_20000;
                case SHUTTER_SPEED_1_16000:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_16000;
                case SHUTTER_SPEED_1_12800:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_12800;
                case SHUTTER_SPEED_1_10000:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_10000;
                case SHUTTER_SPEED_1_8000:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_8000;
                case SHUTTER_SPEED_1_6400:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_6400;
                case SHUTTER_SPEED_1_6000:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_6000;
                case SHUTTER_SPEED_1_5000:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_5000;
                case SHUTTER_SPEED_1_4000:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_4000;
                case SHUTTER_SPEED_1_3200:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_3200;
                case SHUTTER_SPEED_1_3000:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_3000;
                case SHUTTER_SPEED_1_2500:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_2500;
                case SHUTTER_SPEED_1_2000:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_2000;
                case SHUTTER_SPEED_1_1600:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_1600;
                case SHUTTER_SPEED_1_1500:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_1500;
                case SHUTTER_SPEED_1_1250:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_1250;
                case SHUTTER_SPEED_1_1000:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_1000;
                case SHUTTER_SPEED_1_800:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_800;
                case SHUTTER_SPEED_1_725:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_725;
                case SHUTTER_SPEED_1_640:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_640;
                case SHUTTER_SPEED_1_500:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_500;
                case SHUTTER_SPEED_1_400:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_400;
                case SHUTTER_SPEED_1_350:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_350;
                case SHUTTER_SPEED_1_320:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_320;
                case SHUTTER_SPEED_1_250:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_250;
                case SHUTTER_SPEED_1_240:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_240;
                case SHUTTER_SPEED_1_200:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_200;
                case SHUTTER_SPEED_1_180:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_180;
                case SHUTTER_SPEED_1_160:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_160;
                case SHUTTER_SPEED_1_125:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_125;
                case SHUTTER_SPEED_1_120:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_120;
                case SHUTTER_SPEED_1_100:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_100;
                case SHUTTER_SPEED_1_90:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_90;
                case SHUTTER_SPEED_1_80:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_80;
                case SHUTTER_SPEED_1_60:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_60;
                case SHUTTER_SPEED_1_50:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_50;
                case SHUTTER_SPEED_1_40:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_40;
                case SHUTTER_SPEED_1_30:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_30;
                case SHUTTER_SPEED_1_25:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_25;
                case SHUTTER_SPEED_1_20:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_20;
                case SHUTTER_SPEED_1_15:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_15;
                case SHUTTER_SPEED_1_12_DOT_5:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_12_DOT_5;
                case SHUTTER_SPEED_1_10:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_10;
                case SHUTTER_SPEED_1_8:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_8;
                case SHUTTER_SPEED_1_6_DOT_25:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_6_DOT_25;
                case SHUTTER_SPEED_1_5:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_5;
                case SHUTTER_SPEED_1_4:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_4;
                case SHUTTER_SPEED_1_3:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_3;
                case SHUTTER_SPEED_1_2_DOT_5:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_2_DOT_5;
                case SHUTTER_SPEED_1_2:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_2;
                case SHUTTER_SPEED_1_1_DOT_67:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_1_DOT_67;
                case SHUTTER_SPEED_1_1_DOT_25:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_1_DOT_25;
                case SHUTTER_SPEED_1:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1;
                case SHUTTER_SPEED_1_DOT_3:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_DOT_3;
                case SHUTTER_SPEED_1_DOT_6:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_DOT_6;
                case SHUTTER_SPEED_2:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_2;
                case SHUTTER_SPEED_2_DOT_5:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_2_DOT_5;
                case SHUTTER_SPEED_3:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_3;
                case SHUTTER_SPEED_3_DOT_2:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_3_DOT_2;
                case SHUTTER_SPEED_4:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_4;
                case SHUTTER_SPEED_5:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_5;
                case SHUTTER_SPEED_6:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_6;
                case SHUTTER_SPEED_7:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_7;
                case SHUTTER_SPEED_8:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_8;
                case SHUTTER_SPEED_9:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_9;
                case SHUTTER_SPEED_10:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_10;
                case SHUTTER_SPEED_13:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_13;
                case SHUTTER_SPEED_15:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_15;
                case SHUTTER_SPEED_20:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_20;
                case SHUTTER_SPEED_25:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_25;
                case SHUTTER_SPEED_30:
                    return SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_30;
                case UNKNOWN:
                default:
                    return SettingsDefinitions.ShutterSpeed.UNKNOWN;
            }
        }

        public static VideoFileFormat videoFileFormat(SettingsDefinitions.VideoFileFormat value) {
            if (value == null) return null;
            switch (value) {
                case MOV:
                    return VideoFileFormat.MOV;
                case MP4:
                    return VideoFileFormat.MP4;
                case TIFF_SEQ:
                    return VideoFileFormat.TIFF_SEQ;
                case SEQ:
                    return VideoFileFormat.SEQ;
                case UNKNOWN:
                default:
                    return VideoFileFormat.UNKNOWN;
            }
        }

        public static SettingsDefinitions.VideoFileFormat sdkVideoFileFormat(VideoFileFormat value) {
            if (value == null) return null;
            switch (value) {
                case MOV:
                    return SettingsDefinitions.VideoFileFormat.MOV;
                case MP4:
                    return SettingsDefinitions.VideoFileFormat.MP4;
                case TIFF_SEQ:
                    return SettingsDefinitions.VideoFileFormat.TIFF_SEQ;
                case SEQ:
                    return SettingsDefinitions.VideoFileFormat.SEQ;
                case UNKNOWN:
                default:
                    return SettingsDefinitions.VideoFileFormat.UNKNOWN;
            }
        }

        public static VideoResolution videoResolution(SettingsDefinitions.VideoResolution value) {
            if (value == null) return null;
            switch (value) {
                case RESOLUTION_640x480:
                    return VideoResolution.RESOLUTION_640x480;
                case RESOLUTION_640x512:
                    return VideoResolution.RESOLUTION_640x512;
                case RESOLUTION_1280x720:
                    return VideoResolution.RESOLUTION_1280x720;
                case RESOLUTION_1920x1080:
                    return VideoResolution.RESOLUTION_1920x1080;
                case RESOLUTION_2704x1520:
                    return VideoResolution.RESOLUTION_2704x1520;
                case RESOLUTION_2720x1530:
                    return VideoResolution.RESOLUTION_2720x1530;
                case RESOLUTION_3840x1572:
                    return VideoResolution.RESOLUTION_3840x1572;
                case RESOLUTION_3840x2160:
                    return VideoResolution.RESOLUTION_3840x2160;
                case RESOLUTION_4096x2160:
                    return VideoResolution.RESOLUTION_4096x2160;
                case RESOLUTION_4608x2160:
                    return VideoResolution.RESOLUTION_4608x2160;
                case RESOLUTION_4608x2592:
                    return VideoResolution.RESOLUTION_4608x2592;
                case RESOLUTION_5280x2160:
                    return VideoResolution.RESOLUTION_5280x2160;
                case RESOLUTION_MAX:
                    return VideoResolution.RESOLUTION_MAX;
                case NO_SSD_VIDEO:
                    return VideoResolution.NO_SSD_VIDEO;
                case RESOLUTION_5760X3240:
                    return VideoResolution.RESOLUTION_5760X3240;
                case RESOLUTION_6016X3200:
                    return VideoResolution.RESOLUTION_6016X3200;
                case RESOLUTION_2048x1080:
                    return VideoResolution.RESOLUTION_2048x1080;
                case RESOLUTION_5280x2972:
                    return VideoResolution.RESOLUTION_5280x2972;
                case RESOLUTION_336x256:
                    return VideoResolution.RESOLUTION_336x256;
                case RESOLUTION_3712x2088:
                    return VideoResolution.RESOLUTION_3712x2088;
                case RESOLUTION_3944x2088:
                    return VideoResolution.RESOLUTION_3944x2088;
                case RESOLUTION_2688x1512:
                    return VideoResolution.RESOLUTION_2688x1512;
                case RESOLUTION_640x360:
                    return VideoResolution.RESOLUTION_640x360;
                case RESOLUTION_720x576:
                    return VideoResolution.RESOLUTION_720x576;
                case RESOLUTION_7680x4320:
                    return VideoResolution.RESOLUTION_7680x4320;
                case UNKNOWN:
                default:
                    return VideoResolution.UNKNOWN;
            }
        }

        public static SettingsDefinitions.VideoResolution sdkVideoResolution(VideoResolution value) {
            if (value == null) return null;
            switch (value) {
                case RESOLUTION_640x480:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_640x480;
                case RESOLUTION_640x512:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_640x512;
                case RESOLUTION_1280x720:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_1280x720;
                case RESOLUTION_1920x1080:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_1920x1080;
                case RESOLUTION_2704x1520:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_2704x1520;
                case RESOLUTION_2720x1530:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_2720x1530;
                case RESOLUTION_3840x1572:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_3840x1572;
                case RESOLUTION_3840x2160:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_3840x2160;
                case RESOLUTION_4096x2160:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_4096x2160;
                case RESOLUTION_4608x2160:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_4608x2160;
                case RESOLUTION_4608x2592:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_4608x2592;
                case RESOLUTION_5280x2160:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_5280x2160;
                case RESOLUTION_MAX:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_MAX;
                case NO_SSD_VIDEO:
                    return SettingsDefinitions.VideoResolution.NO_SSD_VIDEO;
                case RESOLUTION_5760X3240:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_5760X3240;
                case RESOLUTION_6016X3200:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_6016X3200;
                case RESOLUTION_2048x1080:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_2048x1080;
                case RESOLUTION_5280x2972:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_5280x2972;
                case RESOLUTION_336x256:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_336x256;
                case RESOLUTION_3712x2088:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_3712x2088;
                case RESOLUTION_3944x2088:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_3944x2088;
                case RESOLUTION_2688x1512:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_2688x1512;
                case RESOLUTION_640x360:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_640x360;
                case RESOLUTION_720x576:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_720x576;
                case RESOLUTION_7680x4320:
                    return SettingsDefinitions.VideoResolution.RESOLUTION_7680x4320;
                case UNKNOWN:
                default:
                    return SettingsDefinitions.VideoResolution.UNKNOWN;
            }
        }

        public static VideoFrameRate videoFrameRate(SettingsDefinitions.VideoFrameRate value) {
            if (value == null) return null;
            switch (value) {
                case FRAME_RATE_23_DOT_976_FPS:
                    return VideoFrameRate.FRAME_RATE_23_DOT_976_FPS;
                case FRAME_RATE_24_FPS:
                    return VideoFrameRate.FRAME_RATE_24_FPS;
                case FRAME_RATE_25_FPS:
                    return VideoFrameRate.FRAME_RATE_25_FPS;
                case FRAME_RATE_29_DOT_970_FPS:
                    return VideoFrameRate.FRAME_RATE_29_DOT_970_FPS;
                case FRAME_RATE_30_FPS:
                    return VideoFrameRate.FRAME_RATE_30_FPS;
                case FRAME_RATE_47_DOT_950_FPS:
                    return VideoFrameRate.FRAME_RATE_47_DOT_950_FPS;
                case FRAME_RATE_48_FPS:
                    return VideoFrameRate.FRAME_RATE_48_FPS;
                case FRAME_RATE_50_FPS:
                    return VideoFrameRate.FRAME_RATE_50_FPS;
                case FRAME_RATE_59_DOT_940_FPS:
                    return VideoFrameRate.FRAME_RATE_59_DOT_940_FPS;
                case FRAME_RATE_60_FPS:
                    return VideoFrameRate.FRAME_RATE_60_FPS;
                case FRAME_RATE_96_FPS:
                    return VideoFrameRate.FRAME_RATE_96_FPS;
                case FRAME_RATE_100_FPS:
                    return VideoFrameRate.FRAME_RATE_100_FPS;
                case FRAME_RATE_120_FPS:
                    return VideoFrameRate.FRAME_RATE_120_FPS;
                case FRAME_RATE_240_FPS:
                    return VideoFrameRate.FRAME_RATE_240_FPS;
                case FRAME_RATE_7_DOT_5_FPS:
                    return VideoFrameRate.FRAME_RATE_7_DOT_5_FPS;
                case FRAME_RATE_90_FPS:
                    return VideoFrameRate.FRAME_RATE_90_FPS;
                case FRAME_RATE_8_DOT_7_FPS:
                    return VideoFrameRate.FRAME_RATE_8_DOT_7_FPS;
                case UNKNOWN:
                default:
                    return VideoFrameRate.UNKNOWN;
            }
        }

        public static SettingsDefinitions.VideoFrameRate sdkVideoFrameRate(VideoFrameRate value) {
            if (value == null) return null;
            switch (value) {
                case FRAME_RATE_23_DOT_976_FPS:
                    return SettingsDefinitions.VideoFrameRate.FRAME_RATE_23_DOT_976_FPS;
                case FRAME_RATE_24_FPS:
                    return SettingsDefinitions.VideoFrameRate.FRAME_RATE_24_FPS;
                case FRAME_RATE_25_FPS:
                    return SettingsDefinitions.VideoFrameRate.FRAME_RATE_25_FPS;
                case FRAME_RATE_29_DOT_970_FPS:
                    return SettingsDefinitions.VideoFrameRate.FRAME_RATE_29_DOT_970_FPS;
                case FRAME_RATE_30_FPS:
                    return SettingsDefinitions.VideoFrameRate.FRAME_RATE_30_FPS;
                case FRAME_RATE_47_DOT_950_FPS:
                    return SettingsDefinitions.VideoFrameRate.FRAME_RATE_47_DOT_950_FPS;
                case FRAME_RATE_48_FPS:
                    return SettingsDefinitions.VideoFrameRate.FRAME_RATE_48_FPS;
                case FRAME_RATE_50_FPS:
                    return SettingsDefinitions.VideoFrameRate.FRAME_RATE_50_FPS;
                case FRAME_RATE_59_DOT_940_FPS:
                    return SettingsDefinitions.VideoFrameRate.FRAME_RATE_59_DOT_940_FPS;
                case FRAME_RATE_60_FPS:
                    return SettingsDefinitions.VideoFrameRate.FRAME_RATE_60_FPS;
                case FRAME_RATE_96_FPS:
                    return SettingsDefinitions.VideoFrameRate.FRAME_RATE_96_FPS;
                case FRAME_RATE_100_FPS:
                    return SettingsDefinitions.VideoFrameRate.FRAME_RATE_100_FPS;
                case FRAME_RATE_120_FPS:
                    return SettingsDefinitions.VideoFrameRate.FRAME_RATE_120_FPS;
                case FRAME_RATE_240_FPS:
                    return SettingsDefinitions.VideoFrameRate.FRAME_RATE_240_FPS;
                case FRAME_RATE_7_DOT_5_FPS:
                    return SettingsDefinitions.VideoFrameRate.FRAME_RATE_7_DOT_5_FPS;
                case FRAME_RATE_90_FPS:
                    return SettingsDefinitions.VideoFrameRate.FRAME_RATE_90_FPS;
                case FRAME_RATE_8_DOT_7_FPS:
                    return SettingsDefinitions.VideoFrameRate.FRAME_RATE_8_DOT_7_FPS;
                case UNKNOWN:
                default:
                    return SettingsDefinitions.VideoFrameRate.UNKNOWN;
            }
        }

        public static VideoStandard videoStandard(SettingsDefinitions.VideoStandard value) {
            if (value == null) return null;
            switch (value) {
                case PAL:
                    return VideoStandard.PAL;
                case NTSC:
                    return VideoStandard.NTSC;
                case UNKNOWN:
                default:
                    return VideoStandard.UNKNOWN;
            }
        }

        public static SettingsDefinitions.VideoStandard sdkVideoStandard(VideoStandard value) {
            if (value == null) return null;
            switch (value) {
                case PAL:
                    return SettingsDefinitions.VideoStandard.PAL;
                case NTSC:
                    return SettingsDefinitions.VideoStandard.NTSC;
                case UNKNOWN:
                default:
                    return SettingsDefinitions.VideoStandard.UNKNOWN;
            }
        }

        public static WhiteBalance whiteBalance(dji.common.camera.WhiteBalance value) {
            if (value == null) return null;
            WhiteBalance.WhiteBalancePreset present;
            switch (value.getWhiteBalancePreset()) {
                case AUTO:
                    present = WhiteBalance.WhiteBalancePreset.AUTO;
                    break;
                case SUNNY:
                    present = WhiteBalance.WhiteBalancePreset.SUNNY;
                    break;
                case CLOUDY:
                    present = WhiteBalance.WhiteBalancePreset.CLOUDY;
                    break;
                case WATER_SURFACE:
                    present = WhiteBalance.WhiteBalancePreset.WATER_SURFACE;
                    break;
                case INDOOR_INCANDESCENT:
                    present = WhiteBalance.WhiteBalancePreset.INDOOR_INCANDESCENT;
                    break;
                case INDOOR_FLUORESCENT:
                    present = WhiteBalance.WhiteBalancePreset.INDOOR_FLUORESCENT;
                    break;
                case CUSTOM:
                    present = WhiteBalance.WhiteBalancePreset.CUSTOM;
                    break;
                case PRESET_NEUTRAL:
                    present = WhiteBalance.WhiteBalancePreset.PRESET_NEUTRAL;
                    break;
                case UNKNOWN:
                default:
                    present = WhiteBalance.WhiteBalancePreset.UNKNOWN;
            }
            return new WhiteBalance(present, value.getColorTemperature());
        }

        public static dji.common.camera.WhiteBalance sdkWhiteBalance(WhiteBalance value) {
            if (value == null) return null;
            SettingsDefinitions.WhiteBalancePreset present;
            switch (value.getWhiteBalancePreset()) {
                case AUTO:
                    present = SettingsDefinitions.WhiteBalancePreset.AUTO;
                    break;
                case SUNNY:
                    present = SettingsDefinitions.WhiteBalancePreset.SUNNY;
                    break;
                case CLOUDY:
                    present = SettingsDefinitions.WhiteBalancePreset.CLOUDY;
                    break;
                case WATER_SURFACE:
                    present = SettingsDefinitions.WhiteBalancePreset.WATER_SURFACE;
                    break;
                case INDOOR_INCANDESCENT:
                    present = SettingsDefinitions.WhiteBalancePreset.INDOOR_INCANDESCENT;
                    break;
                case INDOOR_FLUORESCENT:
                    present = SettingsDefinitions.WhiteBalancePreset.INDOOR_FLUORESCENT;
                    break;
                case CUSTOM:
                    present = SettingsDefinitions.WhiteBalancePreset.CUSTOM;
                    break;
                case PRESET_NEUTRAL:
                    present = SettingsDefinitions.WhiteBalancePreset.PRESET_NEUTRAL;
                    break;
                case UNKNOWN:
                default:
                    present = SettingsDefinitions.WhiteBalancePreset.UNKNOWN;
            }
            return new dji.common.camera.WhiteBalance(present, value.getColorTemperature());
        }

        public static com.example.ugcssample.drone.camera.settings.lens.ResolutionAndFrameRate resolutionAndFrameRate(ResolutionAndFrameRate value) {
            if (value == null) return null;
            return new com.example.ugcssample.drone.camera.settings.lens.
                    ResolutionAndFrameRate(videoResolution(value.getResolution()), videoFrameRate(value.getFrameRate()));
        }

        public static ResolutionAndFrameRate sdkResolutionAndFrameRate(com.example.ugcssample.drone.camera.settings.lens.ResolutionAndFrameRate value) {
            if (value == null) return null;
            return new ResolutionAndFrameRate(sdkVideoResolution(value.getResolution()), sdkVideoFrameRate(value.getFrameRate()));
        }

        public static FocusAssistantSettings focusAssistantSettings(dji.common.camera.FocusAssistantSettings value) {
            if (value == null) return null;
            return new FocusAssistantSettings(value.isEnabledMF(), value.isEnabledAF());
        }

        public static dji.common.camera.FocusAssistantSettings sdkFocusAssistantSettings(FocusAssistantSettings value) {
            if (value == null) return null;
            return new dji.common.camera.FocusAssistantSettings(value.isEnabledMF(), value.isEnabledAF());
        }

        public static LensType lensType(SettingsDefinitions.LensType type) {
            if (type == null) return null;
            switch (type) {
                case ZOOM:
                    return LensType.ZOOM;
                case WIDE:
                    return LensType.WIDE;
                case INFRARED_THERMAL:
                    return LensType.THERMAL;
                case UNKNOWN:
                default:
                    return LensType.UNKNOWN;
            }
        }

        public static OpticalZoomSpec opticalZoomSpec(SettingsDefinitions.OpticalZoomSpec sdkOpticalZoomSpec) {
            if (sdkOpticalZoomSpec == null) return null;
            return new OpticalZoomSpec(sdkOpticalZoomSpec.getMaxFocalLength(), sdkOpticalZoomSpec.getMinFocalLength(),
                    sdkOpticalZoomSpec.getFocalLengthStep());
        }

        public static HybridZoomSpec hybridZoomSpec(SettingsDefinitions.HybridZoomSpec sdkHybridZoomSpec) {
            if (sdkHybridZoomSpec == null) return null;
            return new HybridZoomSpec(sdkHybridZoomSpec.getMaxHybridFocalLength(),
                    sdkHybridZoomSpec.getMinHybridFocalLength(), sdkHybridZoomSpec.getMaxOpticalFocalLength(),
                    sdkHybridZoomSpec.getMaxOpticalFocalLength(), sdkHybridZoomSpec.getFocalLengthStep());
        }

        public static SettingsDefinitions.ZoomDirection sdkZoomDirection(ZoomDirection zoomDirection) {
            if (zoomDirection == null) return null;
            switch (zoomDirection) {
                case ZOOM_IN:
                    return SettingsDefinitions.ZoomDirection.ZOOM_IN;
                case ZOOM_OUT:
                    return SettingsDefinitions.ZoomDirection.ZOOM_OUT;
                default:
                    throw new IllegalArgumentException("Illegal zoom direction");
            }
        }

        public static SettingsDefinitions.ZoomSpeed sdkZoomSpeed(double speed) {
            speed = Math.abs(speed);
            if (speed <= 0.1d)
                return SettingsDefinitions.ZoomSpeed.SLOWEST;
            if (speed <= 0.2d)
                return SettingsDefinitions.ZoomSpeed.SLOW;
            if (speed <= 0.4d)
                return SettingsDefinitions.ZoomSpeed.MODERATELY_SLOW;
            if (speed <= 0.6d)
                return SettingsDefinitions.ZoomSpeed.NORMAL;
            if (speed <= 0.8d)
                return SettingsDefinitions.ZoomSpeed.MODERATELY_FAST;
            if (speed <= 0.9d)
                return SettingsDefinitions.ZoomSpeed.FAST;
            return SettingsDefinitions.ZoomSpeed.FASTEST;
        }

        public static SettingsDefinitions.ISO intToSdkIso(int iso) {
            switch (iso) {
                case 50:
                    return SettingsDefinitions.ISO.ISO_50;
                case 100:
                    return SettingsDefinitions.ISO.ISO_100;
                case 200:
                    return SettingsDefinitions.ISO.ISO_200;
                case 400:
                    return SettingsDefinitions.ISO.ISO_400;
                case 800:
                    return SettingsDefinitions.ISO.ISO_800;
                case 1600:
                    return SettingsDefinitions.ISO.ISO_1600;
                case 3200:
                    return SettingsDefinitions.ISO.ISO_3200;
                case 6400:
                    return SettingsDefinitions.ISO.ISO_6400;
                case 12800:
                    return SettingsDefinitions.ISO.ISO_12800;
                case 25600:
                    return SettingsDefinitions.ISO.ISO_25600;
                default:
                    return SettingsDefinitions.ISO.AUTO;
            }
        }
    }
}
