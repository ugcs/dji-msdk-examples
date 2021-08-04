package com.example.ugcssample.drone.domain

import kotlinx.serialization.Serializable

@Serializable
class Failsafe {
    var maxAltitude = 0.0
    var returnAltitude = 0.0
    var smartRth = false
    var lowBatPowerPercent = 0
    var criticalBatPowerPercent = 0
}
