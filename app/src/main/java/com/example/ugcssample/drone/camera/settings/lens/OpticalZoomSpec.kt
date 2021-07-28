package com.example.ugcssample.drone.camera.settings.lens

class OpticalZoomSpec(
    private val maxFocalLength: Int,
    private val minFocalLength: Int,
    private val focalLengthStep: Int
                     ) {
    fun getMaxFocalLength(): Int {
        return maxFocalLength
    }

    fun getMinFocalLength(): Int {
        return minFocalLength
    }

    fun getFocalLengthStep(): Int {
        return focalLengthStep
    }
}