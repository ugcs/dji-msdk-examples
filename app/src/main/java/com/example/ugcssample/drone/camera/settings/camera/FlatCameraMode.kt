package com.example.ugcssample.drone.camera.settings.camera

import dji.common.camera.SettingsDefinitions

enum class FlatCameraMode {
    SLOW_MOTION,
    VIDEO_NORMAL,
    PHOTO_TIME_LAPSE,
    VIDEO_HDR,
    PHOTO_AEB,
    PHOTO_SINGLE,
    PHOTO_BURST,
    PHOTO_HDR,
    PHOTO_INTERVAL,
    PHOTO_COUNTDOWN,
    PHOTO_HYPER_LAPSE,
    PHOTO_HYPER_LIGHT,
    PHOTO_PANORAMA,
    VIDEO_ASTEROID,
    PHOTO_EHDR,
    VIDEO_ROCKET,
    VIDEO_DRONIE,
    VIDEO_CIRCLE,
    VIDEO_HELIX,
    VIDEO_BOOMERANG,
    VIDEO_DOLLY_ZOOM,
    PHOTO_HIGH_RESOLUTION,
    PHOTO_SUPER_RESOLUTION,
    PHOTO_SMART,
    INTERNAL_AI_SPOT_CHECKING,
    UNKNOWN;
    fun toDji() = when (this) {
        SLOW_MOTION -> SettingsDefinitions.FlatCameraMode.SLOW_MOTION
        VIDEO_NORMAL -> SettingsDefinitions.FlatCameraMode.VIDEO_NORMAL
        PHOTO_TIME_LAPSE -> SettingsDefinitions.FlatCameraMode.PHOTO_TIME_LAPSE
        VIDEO_HDR -> SettingsDefinitions.FlatCameraMode.VIDEO_HDR
        PHOTO_AEB -> SettingsDefinitions.FlatCameraMode.PHOTO_AEB
        PHOTO_SINGLE -> SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE
        PHOTO_BURST -> SettingsDefinitions.FlatCameraMode.PHOTO_BURST
        PHOTO_HDR -> SettingsDefinitions.FlatCameraMode.PHOTO_HDR
        PHOTO_INTERVAL -> SettingsDefinitions.FlatCameraMode.PHOTO_INTERVAL
        PHOTO_COUNTDOWN -> SettingsDefinitions.FlatCameraMode.PHOTO_COUNTDOWN
        PHOTO_HYPER_LAPSE -> SettingsDefinitions.FlatCameraMode.PHOTO_HYPER_LAPSE
        PHOTO_HYPER_LIGHT -> SettingsDefinitions.FlatCameraMode.PHOTO_HYPER_LIGHT
        PHOTO_PANORAMA -> SettingsDefinitions.FlatCameraMode.PHOTO_PANORAMA
        VIDEO_ASTEROID -> SettingsDefinitions.FlatCameraMode.VIDEO_ASTEROID
        PHOTO_EHDR -> SettingsDefinitions.FlatCameraMode.PHOTO_EHDR
        VIDEO_ROCKET -> SettingsDefinitions.FlatCameraMode.VIDEO_ROCKET
        VIDEO_DRONIE -> SettingsDefinitions.FlatCameraMode.VIDEO_DRONIE
        VIDEO_CIRCLE -> SettingsDefinitions.FlatCameraMode.VIDEO_CIRCLE
        VIDEO_HELIX -> SettingsDefinitions.FlatCameraMode.VIDEO_HELIX
        VIDEO_BOOMERANG -> SettingsDefinitions.FlatCameraMode.VIDEO_BOOMERANG
        VIDEO_DOLLY_ZOOM -> SettingsDefinitions.FlatCameraMode.VIDEO_DOLLY_ZOOM
        PHOTO_HIGH_RESOLUTION -> SettingsDefinitions.FlatCameraMode.PHOTO_HIGH_RESOLUTION
        PHOTO_SUPER_RESOLUTION -> SettingsDefinitions.FlatCameraMode.PHOTO_SUPER_RESOLUTION
        PHOTO_SMART -> SettingsDefinitions.FlatCameraMode.PHOTO_SMART
        INTERNAL_AI_SPOT_CHECKING -> SettingsDefinitions.FlatCameraMode.INTERNAL_AI_SPOT_CHECKING
        UNKNOWN -> SettingsDefinitions.FlatCameraMode.UNKNOWN
    }
    companion object {
        fun fromDji(mode: SettingsDefinitions.FlatCameraMode) = when (mode) {
            SettingsDefinitions.FlatCameraMode.SLOW_MOTION	->	SLOW_MOTION
            SettingsDefinitions.FlatCameraMode.VIDEO_NORMAL	->	VIDEO_NORMAL
            SettingsDefinitions.FlatCameraMode.PHOTO_TIME_LAPSE	->	PHOTO_TIME_LAPSE
            SettingsDefinitions.FlatCameraMode.VIDEO_HDR	->	VIDEO_HDR
            SettingsDefinitions.FlatCameraMode.PHOTO_AEB	->	PHOTO_AEB
            SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE	->	PHOTO_SINGLE
            SettingsDefinitions.FlatCameraMode.PHOTO_BURST	->	PHOTO_BURST
            SettingsDefinitions.FlatCameraMode.PHOTO_HDR	->	PHOTO_HDR
            SettingsDefinitions.FlatCameraMode.PHOTO_INTERVAL	->	PHOTO_INTERVAL
            SettingsDefinitions.FlatCameraMode.PHOTO_COUNTDOWN	->	PHOTO_COUNTDOWN
            SettingsDefinitions.FlatCameraMode.PHOTO_HYPER_LAPSE	->	PHOTO_HYPER_LAPSE
            SettingsDefinitions.FlatCameraMode.PHOTO_HYPER_LIGHT	->	PHOTO_HYPER_LIGHT
            SettingsDefinitions.FlatCameraMode.PHOTO_PANORAMA	->	PHOTO_PANORAMA
            SettingsDefinitions.FlatCameraMode.VIDEO_ASTEROID	->	VIDEO_ASTEROID
            SettingsDefinitions.FlatCameraMode.PHOTO_EHDR	->	PHOTO_EHDR
            SettingsDefinitions.FlatCameraMode.VIDEO_ROCKET	->	VIDEO_ROCKET
            SettingsDefinitions.FlatCameraMode.VIDEO_DRONIE	->	VIDEO_DRONIE
            SettingsDefinitions.FlatCameraMode.VIDEO_CIRCLE	->	VIDEO_CIRCLE
            SettingsDefinitions.FlatCameraMode.VIDEO_HELIX	->	VIDEO_HELIX
            SettingsDefinitions.FlatCameraMode.VIDEO_BOOMERANG	->	VIDEO_BOOMERANG
            SettingsDefinitions.FlatCameraMode.VIDEO_DOLLY_ZOOM	->	VIDEO_DOLLY_ZOOM
            SettingsDefinitions.FlatCameraMode.PHOTO_HIGH_RESOLUTION	->	PHOTO_HIGH_RESOLUTION
            SettingsDefinitions.FlatCameraMode.PHOTO_SUPER_RESOLUTION	->	PHOTO_SUPER_RESOLUTION
            SettingsDefinitions.FlatCameraMode.PHOTO_SMART	->	PHOTO_SMART
            SettingsDefinitions.FlatCameraMode.INTERNAL_AI_SPOT_CHECKING	->	INTERNAL_AI_SPOT_CHECKING
            SettingsDefinitions.FlatCameraMode.UNKNOWN	->	UNKNOWN
    
        }
    }
}