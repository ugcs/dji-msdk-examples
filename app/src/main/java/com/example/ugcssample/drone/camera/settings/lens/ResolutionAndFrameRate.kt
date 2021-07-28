package com.example.ugcssample.drone.camera.settings.lens

class ResolutionAndFrameRate(
    private val videoResolution: VideoResolution?,
    private val videoFrameRate: VideoFrameRate?
                            ) {
    fun getFrameRate(): VideoFrameRate? {
        return videoFrameRate
    }

    fun getResolution(): VideoResolution? {
        return videoResolution
    }
}