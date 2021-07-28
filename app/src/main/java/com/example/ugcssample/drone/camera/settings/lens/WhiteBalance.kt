package com.example.ugcssample.drone.camera.settings.lens

class WhiteBalance {
    private var whiteBalancePreset: WhiteBalancePreset?
    private var temperature: Int

    constructor(whiteBalancePreset: WhiteBalancePreset?, temperature: Int) {
        this.whiteBalancePreset = whiteBalancePreset
        this.temperature = temperature
    }

    constructor(whiteBalancePreset: WhiteBalancePreset?) {
        this.whiteBalancePreset = whiteBalancePreset
        temperature = 0
    }

    constructor(colorTemperature: Int) {
        whiteBalancePreset = WhiteBalancePreset.AUTO
        temperature = colorTemperature
    }

    enum class WhiteBalancePreset {
        AUTO, SUNNY, CLOUDY, WATER_SURFACE, INDOOR_INCANDESCENT, INDOOR_FLUORESCENT, CUSTOM, PRESET_NEUTRAL, UNKNOWN
    }

    fun getWhiteBalancePreset(): WhiteBalancePreset? {
        return whiteBalancePreset
    }

    fun getColorTemperature(): Int {
        return temperature
    }
}