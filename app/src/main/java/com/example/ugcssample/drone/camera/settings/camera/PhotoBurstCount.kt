package com.example.ugcssample.drone.camera.settings.camera

enum class PhotoBurstCount(private val `val`: Int) {
    BURST_COUNT_2(2), BURST_COUNT_3(3), BURST_COUNT_5(5), BURST_COUNT_7(7), BURST_COUNT_10(10), BURST_COUNT_14(
        14
                                                                                                              ),
    CONTINUOUS(-1), UNKNOWN(-2);

    fun getVal(): Int {
        return `val`
    }
}