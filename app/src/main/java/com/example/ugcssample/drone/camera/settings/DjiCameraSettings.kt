package com.example.ugcssample.drone.camera.settings

import dji.common.camera.CameraVideoStreamSource
import dji.common.camera.SettingsDefinitions

/**
 * Created by Dima on 15/08/2016.
 */
class DjiCameraSettings {
    var cameraModeInited = false
    var cameraMode: SettingsDefinitions.CameraMode? = null
    var photoAEBCountInited = false
    var photoAEBCount: SettingsDefinitions.PhotoAEBCount? = null
    var photoBurstCountInited = false
    var photoBurstCount: SettingsDefinitions.PhotoBurstCount? = null
    var photoTimeIntervalSettingsInited = false
    var photoTimeIntervalSettings: SettingsDefinitions.PhotoTimeIntervalSettings? = null
    var shootPhotoModeInited = false
    var shootPhotoMode: SettingsDefinitions.ShootPhotoMode? = null
    var activeSourceInited = false
    var activeSource: CameraVideoStreamSource? = null
    var isVideoRecordingInited = false
    var isVideoRecording: Boolean? = null
    var isShootingSinglePhotoInited = false
    var isShootingSinglePhoto: Boolean? = null
    var isShootingBurstPhotoInited = false
    var isShootingBurstPhoto: Boolean? = null
    var isShootingIntervalPhotoInited = false
    var isShootingIntervalPhoto: Boolean? = null
    var isStoringPhotoInited = false
    var isStoringPhoto: Boolean? = null
    var currentVideoRecordingTimeInSecondsInited = false
    var currentVideoRecordingTimeInSeconds: Int? = null
}