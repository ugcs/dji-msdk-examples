package com.example.ugcssample.drone.camera.settings;

import android.graphics.PointF;

import dji.common.camera.FocusAssistantSettings;
import dji.common.camera.ResolutionAndFrameRate;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.WhiteBalance;

/**
 * Created by Dima on 15/08/2016.
 */
public class DjiLensSettings {
    public boolean focusModeInited;
    public SettingsDefinitions.FocusMode focusMode;
    public boolean photoAspectRatioInited;
    public SettingsDefinitions.PhotoAspectRatio photoAspectRatio;
    public boolean photoFileFormatInited;
    public SettingsDefinitions.PhotoFileFormat photoFileFormat;
    public boolean displayModeInited;
    public SettingsDefinitions.DisplayMode displayMode;

    public boolean videoResolutionAndFrameRateInited;
    public ResolutionAndFrameRate videoResolutionAndFrameRate;
    public boolean videoFileFormatInited;
    public SettingsDefinitions.VideoFileFormat videoFileFormat;  // MOV, MP4
    public boolean videoStandardInited;
    public SettingsDefinitions.VideoStandard videoStandard;  //PAL, NTSC

    public boolean whiteBalanceInited;
    public WhiteBalance whiteBalance;  // Auto, Sunny, Cloudy...

    public boolean meteringModeInited;
    public SettingsDefinitions.MeteringMode meteringMode; // CENTER, AVERAGE, SPOT
    public boolean exposureModeInited;
    public SettingsDefinitions.ExposureMode exposureMode; //Program, ShutterPriority, AperturePriority, Manual
    public boolean isoInited;
    public SettingsDefinitions.ISO iso; // Auto, ISO_100, ISO_200,
    public boolean apertureInited;
    public SettingsDefinitions.Aperture aperture; //  ... F_6p8, F_7p1 ...
    public boolean shutterSpeedInited;
    public SettingsDefinitions.ShutterSpeed shutterSpeed;

    public boolean exposureCompensationInited;
    public SettingsDefinitions.ExposureCompensation exposureCompensation; // EV [-5.0 .. +5.0]

    // Auto, 60Hz, 50Hz
    public boolean antiFlickerInited;
    public SettingsDefinitions.AntiFlickerFrequency antiFlicker;

    public boolean focusTargetInited;
    public PointF focusTarget;
    public boolean focusRingValueInited;
    public Integer focusRingValue;
    public boolean focusRingValueMaxInited;
    public Integer focusRingValueMax;
    public boolean focusAssistantSettingsInited;
    public FocusAssistantSettings focusAssistantSettings;

    public boolean thermalIsothermLowerValueInited;
    public Integer thermalIsothermLowerValue;
    public boolean thermalIsothermUpperValueInited;
    public Integer thermalIsothermUpperValue;
    public boolean thermalIsothermEnabledInited;
    public Boolean thermalIsothermEnabled;

    public boolean opticalZoomSpecInited;
    public SettingsDefinitions.OpticalZoomSpec opticalZoomSpec;
    public boolean hybridZoomSpecInited;
    public SettingsDefinitions.HybridZoomSpec hybridZoomSpec;
    public boolean opticalZoomFocalLengthInited;
    public Integer opticalZoomFocalLength;
    public boolean hybridZoomFocalLengthInited;
    public Integer hybridZoomFocalLength;

    public Integer thermalIsothermMaxValue;
    public Integer thermalIsothermMinValue;
}
