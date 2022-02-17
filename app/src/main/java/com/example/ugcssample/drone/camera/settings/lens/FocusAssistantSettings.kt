package com.example.ugcssample.drone.camera.settings.lens

class FocusAssistantSettings(private val enabledMF: Boolean, private val enabledAF: Boolean) {
    fun isEnabledAF(): Boolean {
        return enabledAF
    }

    fun isEnabledMF(): Boolean {
        return enabledMF
    }
    
    override fun toString(): String = "enabledMF: $enabledMF, enabledAF: $enabledAF"
}