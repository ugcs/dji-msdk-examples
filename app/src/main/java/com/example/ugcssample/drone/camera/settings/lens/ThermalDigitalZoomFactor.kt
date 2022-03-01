package com.example.ugcssample.drone.camera.settings.lens

import dji.common.camera.SettingsDefinitions

enum class ThermalDigitalZoomFactor {
    X_1, X_2, X_4, X_8, UNKNOWN;
    fun toDji() = when (this) {
        X_1 -> SettingsDefinitions.ThermalDigitalZoomFactor.X_1
        X_2 -> SettingsDefinitions.ThermalDigitalZoomFactor.X_2
        X_4 -> SettingsDefinitions.ThermalDigitalZoomFactor.X_4
        X_8 -> SettingsDefinitions.ThermalDigitalZoomFactor.X_8
        UNKNOWN -> SettingsDefinitions.ThermalDigitalZoomFactor.UNKNOWN
    }
}