package com.example.ugcssample.drone.camera.settings.camera

enum class PhotoAEBCount(private val `val`: Int) {
    AEB_COUNT_3(3), AEB_COUNT_5(5), AEB_COUNT_7(7), UNKNOWN(-1);

    fun getVal(): Int {
        return `val`
    }
}