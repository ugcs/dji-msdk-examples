package com.example.ugcssample.drone

import com.example.ugcssample.drone.camera.settings.camera.ShootPhotoMode
import com.example.ugcssample.drone.camera.settings.lens.DisplayMode
import com.google.gson.Gson
import kotlinx.serialization.*

@Serializable
data class CameraTestCaseReport(
        val setting: String,
        val value: String,
        val isSuccess: Boolean,
        val isDeclaredAsSupported: Boolean?,
        val state: CameraState,
        val error: String? = null
                               )

@Serializable
data class CameraResultsInfo (
        val reports: List<CameraTestCaseReport> = emptyList(),
        val cameraName : String,
        val cameraType : String
        )

@Serializable
data class CameraState (
        var lensDisplayMode : DisplayMode? = null,
        var shootPhotoMode : ShootPhotoMode? = null
        ) {
    fun copy() = Gson().fromJson(Gson().toJson(this), this.javaClass)
    
}