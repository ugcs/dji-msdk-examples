package com.example.ugcssample.drone

import kotlinx.serialization.*

@Serializable
data class CameraTestResult(
        val setting: String,
        val value: String,
        val isSuccess: Boolean,
        val isDeclaredAsSupported: Boolean?,
        val error: String? = null
                           )
