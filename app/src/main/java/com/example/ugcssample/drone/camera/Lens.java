package com.example.ugcssample.drone.camera;

import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import com.example.ugcssample.drone.camera.settings.lens.ResolutionAndFrameRate;
import com.example.ugcssample.drone.camera.settings.lens.ShutterSpeed;
import com.example.ugcssample.drone.camera.settings.lens.VideoFileFormat;
import com.example.ugcssample.drone.camera.settings.lens.VideoStandard;
import com.example.ugcssample.drone.camera.settings.lens.WhiteBalance;
import com.example.ugcssample.drone.camera.settings.lens.ZoomDirection;

import java.util.List;

@SuppressWarnings("unused")
public interface Lens {

    interface Callback {
        void run(Exception error);
    }

    interface ValueChangeListener<T> {
        void onChange(T o);
    }

    @NonNull
    LensType getType();

    int getId();

    @NonNull
    String getName();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    AntiFlickerFrequency getAntiFlickerFrequency();

    void setAntiFlickerFrequency(@NonNull AntiFlickerFrequency antiFlickerFrequency, Callback onSet);

    void addAntiFlickerFrequencyListener(@NonNull ValueChangeListener<AntiFlickerFrequency> listener);

    void removeAntiFlickerFrequencyListener(@NonNull ValueChangeListener<AntiFlickerFrequency> listener);

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    Aperture getAperture();

    void setAperture(@NonNull Aperture aperture, Callback onSet);

    void addApertureChangeListener(@NonNull ValueChangeListener<Aperture> listener);

    void removeApertureChangeListener(@NonNull ValueChangeListener<Aperture> listener);

    @NonNull
    List<Aperture> getSupportedApertures();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    DisplayMode getDisplayMode();

    void setDisplayMode(@NonNull DisplayMode displayMode, Callback onSet);

    void addDisplayModeChangeListener(@NonNull ValueChangeListener<DisplayMode> listener);

    void removeDisplayModeChangeListener(@NonNull ValueChangeListener<DisplayMode> listener);

    @NonNull
    List<DisplayMode> getSupportedDisplayModes();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    ExposureCompensation getExposureCompensation();

    void setExposureCompensation(@NonNull ExposureCompensation exposureCompensation, Callback onSet);

    void addExposureCompensationChangeListener(@NonNull ValueChangeListener<ExposureCompensation> listener);

    void removeExposureCompensationChangeListener(@NonNull ValueChangeListener<ExposureCompensation> listener);

    @NonNull
    List<ExposureCompensation> getSupportedExposureCompensations();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    ExposureMode getExposureMode();

    void setExposureMode(@NonNull ExposureMode exposureMode, Callback onSet);

    void addExposureModeChangeListener(@NonNull ValueChangeListener<ExposureMode> listener);

    void removeExposureModeChangeListener(@NonNull ValueChangeListener<ExposureMode> listener);

    @NonNull
    List<ExposureMode> getSupportedExposureModes();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    FocusMode getFocusMode();

    void setFocusMode(@NonNull FocusMode focusMode, Callback onSet);

    void addFocusModeChangeListener(@NonNull ValueChangeListener<FocusMode> listener);

    void removeFocusModeChangeListener(@NonNull ValueChangeListener<FocusMode> listener);

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    ISO getISO();

    void setISO(@NonNull ISO iso, Callback onSet);

    void addISOChangeListener(@NonNull ValueChangeListener<ISO> listener);

    void removeISOChangeListener(@NonNull ValueChangeListener<ISO> listener);

    @NonNull
    List<ISO> getSupportedISOs();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    MeteringMode getMeteringMode();

    void setMeteringMode(@NonNull MeteringMode meteringMode, Callback onSet);

    void addMeteringModeChangeListener(@NonNull ValueChangeListener<MeteringMode> listener);

    void removeMeteringModeChangeListener(@NonNull ValueChangeListener<MeteringMode> listener);

    @NonNull
    List<MeteringMode> getSupportedMeteringModes();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    PhotoAspectRatio getPhotoAspectRatio();

    void setPhotoAspectRatio(@NonNull PhotoAspectRatio photoAspectRatio, Callback onSet);

    void addPhotoAspectRatioChangeListener(@NonNull ValueChangeListener<PhotoAspectRatio> listener);

    void removePhotoAspectRatioChangeListener(@NonNull ValueChangeListener<PhotoAspectRatio> listener);

    @NonNull
    List<PhotoAspectRatio> getSupportedPhotoAspectRatio();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    PhotoFileFormat getPhotoFileFormat();

    void setPhotoFileFormat(@NonNull PhotoFileFormat photoFileFormat, Callback onSet);

    void addPhotoFileFormatChangeListener(@NonNull ValueChangeListener<PhotoFileFormat> listener);

    void removePhotoFileFormatChangeListener(@NonNull ValueChangeListener<PhotoFileFormat> listener);

    @NonNull
    List<PhotoFileFormat> getSupportedPhotoFileFormats();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    ShutterSpeed getShutterSpeed();

    void setShutterSpeed(@NonNull ShutterSpeed shutterSpeed, Callback onSet);

    void addShutterSpeedChangeListener(@NonNull ValueChangeListener<ShutterSpeed> listener);

    void removeShutterSpeedChangeListener(@NonNull ValueChangeListener<ShutterSpeed> listener);

    @NonNull
    List<ShutterSpeed> getSupportedShutterSpeeds();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    VideoFileFormat getVideoFileFormat();

    void setVideoFileFormat(@NonNull VideoFileFormat videoFileFormat, Callback onSet);

    void addVideoFileFormatChangeListener(@NonNull ValueChangeListener<VideoFileFormat> listener);

    void removeVideoFileFormatChangeListener(@NonNull ValueChangeListener<VideoFileFormat> listener);

    @NonNull
    List<VideoFileFormat> getSupportedVideoFileFormats();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    ResolutionAndFrameRate getVideoResolutionAndFrameRate();

    void setVideoResolutionAndFrameRate(ResolutionAndFrameRate resolutionAndFrameRate, Callback onSet);

    void addResolutionAndFrameRateChangeListener(@NonNull ValueChangeListener<ResolutionAndFrameRate> listener);

    void removeResolutionAndFrameRateChangeListener(@NonNull ValueChangeListener<ResolutionAndFrameRate> listener);

    @NonNull
    List<ResolutionAndFrameRate> getSupportedVideoResolutionAndFrameRate();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    VideoStandard getVideoStandard();

    void setVideoStandard(@NonNull VideoStandard videoStandard, Callback onSet);

    void addVideoStandardChangeListener(@NonNull ValueChangeListener<VideoStandard> listener);

    void removeVideoStandardChangeListener(@NonNull ValueChangeListener<VideoStandard> listener);

    @NonNull
    List<VideoStandard> getSupportedVideoStandards();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    WhiteBalance getWhiteBalance();

    void setWhiteBalance(@NonNull WhiteBalance whiteBalance, Callback onSet);

    void addWhiteBalanceListener(@NonNull ValueChangeListener<WhiteBalance> listener);

    void removeWhiteBalanceListener(@NonNull ValueChangeListener<WhiteBalance> listener);

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    PointF getFocusTarget();

    void setFocusTarget(@NonNull PointF focusTarget, Callback onSet);

    void addFocusTargetListener(@NonNull ValueChangeListener<PointF> listener);

    void removeFocusTargetListener(@NonNull ValueChangeListener<PointF> listener);

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    Integer getFocusRingValue();

    void setFocusRingValue(@NonNull Integer focusRingValue, Callback onSet);

    void addFocusRingListener(@NonNull ValueChangeListener<Integer> listener);

    void removeFocusRingListener(@NonNull ValueChangeListener<Integer> listener);

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    Integer getFocusRingMax();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    FocusAssistantSettings getFocusAssistantSettings();

    void setFocusAssistantSettings(@NonNull FocusAssistantSettings focusAssistantSettings, Callback onSet);

    void addFocusAssistantSettingsListener(@NonNull ValueChangeListener<FocusAssistantSettings> listener);

    void removeFocusAssistantSettingsListener(@NonNull ValueChangeListener<FocusAssistantSettings> listener);

    boolean isAdjustableFocalPointCanBeChanged();

    boolean isFocusAssistantEnabled();

    void setThermalIsothermLowerValue(int val, Callback onSet);

    void setThermalIsothermUpperValue(int val, Callback onSet);

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    Integer getThermalIsothermLowerValue();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    Integer getThermalIsothermUpperValue();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    Integer getThermalIsothermMaxValue();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    Integer getThermalIsothermMinValue();

    void setThermalIsothermEnabled(boolean enabled, Callback onSet);

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    Boolean getThermalIsothermEnabled();

    /**
     * Callback is called immediately if camera is already initialised
     */
    void addOnInitialisedListener(@NonNull Callback listener);

    void removeOnInitialisedListener(@NonNull Callback listener);

    boolean isInitialised();

    boolean isOpticalZoomSupported();

    boolean isHybridZoomSupported();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    OpticalZoomSpec getOpticalZoomSpec();

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    HybridZoomSpec getHybridZoomSpec();

    void startContiniousOpticalZoom(ZoomDirection zoomDirection, double zoomSpeed, Callback onStart);

    void stopContiniousOpticalZoom(Callback onStop);

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    Integer getOpticalZoomFocalLength();

    void setOpticalZoomFocalLength(int value, Callback onSet);

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    @Nullable
    Integer getHybridZoomFocalLength();

    void setHybridZoomFocalLength(int value, Callback onSet);

    void zoomAtTarget(PointF point, Callback onZoom);

    void addOpticalFocalLengthListener(ValueChangeListener<Integer> valueChangeListener);

}