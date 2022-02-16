package com.example.ugcssample.drone

import com.example.ugcssample.DJIErrorException
import com.example.ugcssample.drone.camera.Camera
import com.example.ugcssample.drone.camera.settings.camera.*
import com.example.ugcssample.drone.camera.settings.lens.*
import timber.log.Timber
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KFunction0
import kotlin.reflect.KSuspendFunction0
import kotlin.reflect.KSuspendFunction1
import kotlin.reflect.KFunction1

class CameraTester(val camera : Camera) {
    val results = mutableListOf<CameraTestResult>()
    val cameraName = camera.getDisplayName()
    suspend fun runTests() {
        results.clear()
    
        val supportedCameraModes = camera.getSupportedCameraModes()
        for (mode in CameraMode.values()) {
            setValue(camera::setCameraMode, mode, supportedCameraModes.contains(mode))
        }
        try {
            camera.setCameraMode(CameraMode.SHOOT_PHOTO)
        } catch (e: DJIErrorException) {
            Timber.e("Unable to set CameraMode SHOOT_PHOTO")
        }
    
    
        for (mode in PhotoAEBCount.values()) {
            setValue(camera::setPhotoAEBCount, mode)
        }
        for (mode in PhotoBurstCount.values()) {
            setValue(camera::setPhotoBurstCount, mode)
        }
    
        for (count in 0..15) {
            for (interval in 0..10) {
                setValue(camera::setPhotoTimeIntervalSettings, PhotoTimeIntervalSettings(count, interval))
            }
        }
    
        for (mode in ShootPhotoMode.values()) {
            setValue(camera::setShootPhotoMode, mode)
        }
        try {
                camera.setShootPhotoMode(ShootPhotoMode.SINGLE)
            } catch (e: DJIErrorException) {
                Timber.w("Unable to set CameraMode SHOOT_PHOTO. Error: ${e.description}")
            }
        val lens = camera.getActiveLens()
        val displayModes = lens.supportedDisplayModes
        for (mode in DisplayMode.values()) {
            setValueFeature(lens::setDisplayMode,mode,displayModes.contains(mode))
        }
    
        for (mode in AntiFlickerFrequency.values()) {
            setValueFeature(lens::setAntiFlickerFrequency, mode)
        }
    
        val supportedApertures = lens.supportedApertures
        for (mode in Aperture.values()) {
            setValueFeature(lens::setAperture, mode,supportedApertures.contains(mode))
        }
    
        val supportedExposureModes = lens.supportedExposureModes
        for (mode in ExposureMode.values()) {
            setValueFeature(lens::setExposureMode, mode,supportedExposureModes.contains(mode))
        }
    
        val supportedExposureCompensations = lens.supportedExposureCompensations
        for (mode in ExposureCompensation.values()) {
            setValueFeature(lens::setExposureCompensation, mode,supportedExposureCompensations.contains(mode))
            
        }
        setValueFeature(lens::setFocusAssistantSettings, FocusAssistantSettings(enabledMF = false, enabledAF = false))
        setValueFeature(lens::setFocusAssistantSettings, FocusAssistantSettings(enabledMF = true, enabledAF = false))
        setValueFeature(lens::setFocusAssistantSettings, FocusAssistantSettings(enabledMF = false, enabledAF = true))
        setValueFeature(lens::setFocusAssistantSettings, FocusAssistantSettings(enabledMF = true, enabledAF = true))
        
        val supportedISOs = lens.supportedISOs
        for (mode in ISO.values()) {
            setValueFeature(lens::setISO,mode,supportedISOs.contains(mode))
        }
        for (mode in WhiteBalance.WhiteBalancePreset.values()) {
            setValueFeature(lens::setWhiteBalance, WhiteBalance(mode))
        }
        getValue(camera::getThermalIsothermEnabled)
        getValue(camera::getThermalIsothermLowerValue)
        getValue(camera::getThermalIsothermUpperValue)
        getValue(camera::getFocusAssistantSettings)
        getValue(camera::getFocusMode)
        getValue(camera::getFocusRingValue)
        getValue(camera::getFocusTarget)
        getValueFeature(lens::getThermalIsothermEnabled)
    
        getValueFeature(lens::getThermalIsothermEnabled)
        getValueFeature(lens::getThermalIsothermLowerValue)
        getValueFeature(lens::getThermalIsothermUpperValue)
//        getValue(camera::getFocusAssistantSettings)
        getValueFeature(lens::getFocusMode)
        getValueFeature(lens::getFocusRingValue)
        getValueFeature(lens::getFocusTarget)
    
    }
    private suspend fun <P0> setValue(method : KSuspendFunction1<P0, Unit>, arg0 : P0) = setValue(method, arg0, false)
    private suspend fun <P0> setValue(method : KSuspendFunction1<P0, Unit>, arg0 : P0, isSupported : Boolean) {
        try {
            method(arg0)
            results.add(
                CameraTestResult(
                    cameraName,
                    "${method.javaClass}.${method.name}",
                    arg0.toString(),
                    true,
                    isSupported
                                )
                       )
        } catch (e: DJIErrorException) {
            results.add(
                CameraTestResult(
                    cameraName,
                    "${method.javaClass}.${method.name}",
                    arg0.toString(),
                    false,
                    isSupported,
                    e.description
                                )
                       )
        }
    }
    private suspend fun <R> getValue(method : KSuspendFunction0<R>, isSupported: Boolean){
        try {
            val res : R? = method()
            results.add(
                CameraTestResult(
                    cameraName,
                    method.name,
                    res?.toString() ?: "UNKNOWN",
                    true,
                    isSupported
                                )
                       )
        } catch (e: DJIErrorException) {
            results.add(
                CameraTestResult(
                    cameraName,
                    method.name,
                    "UNKNOWN",
                    false,
                    isSupported,
                    e.description
                                )
                       )
        }
    }
    private suspend fun <R> getValue(method : KSuspendFunction0<R>) = getValue(method, false)
    private fun <R> getValueFeature(method : KFunction0<R>){
        try {
            val res : R? = method()
            results.add(
                CameraTestResult(
                    cameraName,
                    method.name,
                    res?.toString() ?: "UNKNOWN",
                    true,
                    false
                                )
                       )
        } catch (e: DJIErrorException) {
            results.add(
                CameraTestResult(
                    cameraName,
                    method.name,
                    "UNKNOWN",
                    false,
                    false,
                    e.description
                                )
                       )
        }
    }
    private fun <P0> setValueFeature(method : KFunction1<P0, CompletableFuture<Void>>, arg0 : P0) = setValueFeature(method,arg0,false)
    private fun <P0> setValueFeature(method : KFunction1<P0, CompletableFuture<Void>>, arg0 : P0, isSupported : Boolean) {
        try {
            method(arg0)
            results.add(
                CameraTestResult(
                    cameraName,
                    "${method.javaClass}.${method.name}",
                    arg0.toString(),
                    true,
                    isSupported
                                )
                       )
        } catch (e: DJIErrorException) {
            results.add(
                CameraTestResult(
                    cameraName,
                    "${method.javaClass}.${method.name}",
                    arg0.toString(),
                    false,
                    isSupported,
                    e.description
                                )
                       )
        }
    }
}