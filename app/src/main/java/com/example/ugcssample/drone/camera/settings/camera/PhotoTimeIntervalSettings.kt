package com.example.ugcssample.drone.camera.settings.camera

class PhotoTimeIntervalSettings(
    val captureCount: Int,
    val timeIntervalInSeconds: Int
                               ) {
    
    companion object {
        const val MAX_INTERVALED_CAPTURE_CNT = 255
    }
    
    override fun toString(): String = "captureCount: $captureCount, timeIntervalInSeconds: $timeIntervalInSeconds"
}