package com.example.ugcssample.drone.camera.settings.camera

import dji.common.camera.SettingsDefinitions

enum class ShootPhotoMode {
    SINGLE, HDR, BURST, AEB, INTERVAL, TIME_LAPSE, PANORAMA, RAW_BURST, SHALLOW_FOCUS, EHDR, HYPER_LIGHT, HYPER_LAPSE, SUPER_RESOLUTION, UNKNOWN;
    fun toDji() = when (this) {
        SINGLE -> SettingsDefinitions.ShootPhotoMode.SINGLE
        HDR -> SettingsDefinitions.ShootPhotoMode.HDR
        BURST -> SettingsDefinitions.ShootPhotoMode.BURST
        AEB -> SettingsDefinitions.ShootPhotoMode.AEB
        INTERVAL -> SettingsDefinitions.ShootPhotoMode.INTERVAL
        TIME_LAPSE -> SettingsDefinitions.ShootPhotoMode.TIME_LAPSE
        PANORAMA -> SettingsDefinitions.ShootPhotoMode.PANORAMA
        RAW_BURST -> SettingsDefinitions.ShootPhotoMode.RAW_BURST
        SHALLOW_FOCUS -> SettingsDefinitions.ShootPhotoMode.SHALLOW_FOCUS
        EHDR -> SettingsDefinitions.ShootPhotoMode.EHDR
        HYPER_LIGHT -> SettingsDefinitions.ShootPhotoMode.HYPER_LIGHT
        HYPER_LAPSE -> SettingsDefinitions.ShootPhotoMode.HYPER_LAPSE
        SUPER_RESOLUTION -> SettingsDefinitions.ShootPhotoMode.SUPER_RESOLUTION
        UNKNOWN -> SettingsDefinitions.ShootPhotoMode.UNKNOWN
    }
}