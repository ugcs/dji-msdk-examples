package com.example.ugcssample.drone

import com.example.ugcssample.drone.camera.Camera

import java.lang.Exception
import kotlinx.serialization.*
import kotlinx.serialization.json.*
@Serializable
data class CameraTestResult(
        val cameraName : String,
        val setting : String,
        val value : String,
        val isSuccess : Boolean,
        val error : String
                           )
