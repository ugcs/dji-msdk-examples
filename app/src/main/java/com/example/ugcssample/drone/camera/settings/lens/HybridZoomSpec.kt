package com.example.ugcssample.drone.camera.settings.lens

class HybridZoomSpec(
    private val maxHybridFocalLength: Int, private val minHybridFocalLength: Int,
    private val maxOpticalFocalLength: Int, private val minOpticalFocalLength: Int,
    private val focalLengthStep: Int
                    ) {
    fun getMaxHybridFocalLength(): Int {
        return maxHybridFocalLength
    }

    fun getMinHybridFocalLength(): Int {
        return minHybridFocalLength
    }

    fun getMaxOpticalFocalLength(): Int {
        return maxOpticalFocalLength
    }

    fun getMinOpticalFocalLength(): Int {
        return minOpticalFocalLength
    }

    fun getFocalLengthStep(): Int {
        return focalLengthStep
    }
}