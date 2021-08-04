package com.example.ugcssample.drone.domain

import dji.common.model.LocationCoordinate2D
import kotlinx.serialization.Serializable


data class AutopilotModel(
    val failsafe: Failsafe?,
    val goHomeAlt: Int,
    val homeLocation: LocationCoordinate2D?
)