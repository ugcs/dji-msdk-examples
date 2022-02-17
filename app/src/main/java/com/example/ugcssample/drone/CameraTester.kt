package com.example.ugcssample.drone

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.ugcssample.DJIErrorException
import com.example.ugcssample.drone.camera.Camera
import com.example.ugcssample.drone.camera.settings.camera.*
import com.example.ugcssample.drone.camera.settings.lens.*
import timber.log.Timber
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import kotlin.reflect.KFunction0
import kotlin.reflect.KSuspendFunction0
import kotlin.reflect.KSuspendFunction1
import kotlin.reflect.KFunction1

class CameraTester(val camera : Camera) {
    val results = mutableListOf<CameraTestResult>()
    val cameraName = camera.getDisplayName()
    enum class Interface {CAMERA, LENS}
    @RequiresApi(Build.VERSION_CODES.N)
    suspend fun runTests() {
        results.clear()
    
        val supportedCameraModes = camera.getSupportedCameraModes()
        for (mode in CameraMode.values()) {
            setValue(Interface.CAMERA,camera::setCameraMode, mode, supportedCameraModes.contains(mode))
        }
        try {
            camera.setCameraMode(CameraMode.SHOOT_PHOTO)
        } catch (e: DJIErrorException) {
            Timber.e("Unable to set CameraMode SHOOT_PHOTO")
        }
    
    
        for (mode in PhotoAEBCount.values()) {
            setValue(Interface.CAMERA,camera::setPhotoAEBCount, mode)
        }
        for (mode in PhotoBurstCount.values()) {
            setValue(Interface.CAMERA,camera::setPhotoBurstCount, mode)
        }
    
        for (count in 0..15) {
            for (interval in 0..10) {
                setValue(Interface.CAMERA,camera::setPhotoTimeIntervalSettings, PhotoTimeIntervalSettings(count, interval))
            }
        }
    
        for (mode in ShootPhotoMode.values()) {
            setValue(Interface.CAMERA,camera::setShootPhotoMode, mode)
        }
        try {
                camera.setShootPhotoMode(ShootPhotoMode.SINGLE)
            } catch (e: DJIErrorException) {
                Timber.w("Unable to set CameraMode SHOOT_PHOTO. Error: ${e.description}")
            }
    
        getValue(Interface.CAMERA,camera::getThermalIsothermEnabled)
        getValue(Interface.CAMERA,camera::getThermalIsothermLowerValue)
        getValue(Interface.CAMERA,camera::getThermalIsothermUpperValue)
        getValue(Interface.CAMERA,camera::getFocusAssistantSettings)
        getValue(Interface.CAMERA,camera::getFocusMode)
        getValue(Interface.CAMERA,camera::getFocusRingValue)
        getValue(Interface.CAMERA,camera::getFocusTarget)
        camera.getLenses().forEach {  lens ->
            if (lens != null) {
                try {
                    camera.setActiveLens(lens)
                } catch (e: DJIErrorException) {
                    Timber.i("Failed to active lens $lens")
                }
                val displayModes = lens.supportedDisplayModes
                for (mode in DisplayMode.values()) {
                    setValueFeature(Interface.LENS, lens::setDisplayMode, mode, displayModes.contains(mode))
                }
    
                for (mode in AntiFlickerFrequency.values()) {
                    setValueFeature(Interface.LENS, lens::setAntiFlickerFrequency, mode)
                }
    
                val supportedApertures = lens.supportedApertures
                for (mode in Aperture.values()) {
                    setValueFeature(Interface.LENS, lens::setAperture, mode, supportedApertures.contains(mode))
                }
    
                val supportedExposureModes = lens.supportedExposureModes
                for (mode in ExposureMode.values()) {
                    setValueFeature(Interface.LENS, lens::setExposureMode, mode, supportedExposureModes.contains(mode))
                }
    
                val supportedExposureCompensations = lens.supportedExposureCompensations
                for (mode in ExposureCompensation.values()) {
                    setValueFeature(Interface.LENS, lens::setExposureCompensation, mode, supportedExposureCompensations.contains(mode))
        
                }
                setValueFeature(Interface.LENS, lens::setFocusAssistantSettings, FocusAssistantSettings(enabledMF = false, enabledAF = false))
                setValueFeature(Interface.LENS, lens::setFocusAssistantSettings, FocusAssistantSettings(enabledMF = true, enabledAF = false))
                setValueFeature(Interface.LENS, lens::setFocusAssistantSettings, FocusAssistantSettings(enabledMF = false, enabledAF = true))
                setValueFeature(Interface.LENS, lens::setFocusAssistantSettings, FocusAssistantSettings(enabledMF = true, enabledAF = true))
    
                val supportedISOs = lens.supportedISOs
                for (mode in ISO.values()) {
                    setValueFeature(Interface.LENS, lens::setISO, mode, supportedISOs.contains(mode))
                }
                for (mode in WhiteBalance.WhiteBalancePreset.values()) {
                    setValueFeature(Interface.LENS, lens::setWhiteBalance, WhiteBalance(mode))
                }
    
                getValueFeature(Interface.LENS, lens::getThermalIsothermEnabled)
    
                getValueFeature(Interface.LENS, lens::getThermalIsothermEnabled)
                getValueFeature(Interface.LENS, lens::getThermalIsothermLowerValue)
                getValueFeature(Interface.LENS, lens::getThermalIsothermUpperValue)
//        getValue(camera::getFocusAssistantSettings)
                getValueFeature(Interface.LENS, lens::getFocusMode)
                getValueFeature(Interface.LENS, lens::getFocusRingValue)
                getValueFeature(Interface.LENS, lens::getFocusTarget)
            }
        }
        
    
    }
    private suspend fun <P0> setValue(inter : Interface, method : KSuspendFunction1<P0, Unit>, arg0 : P0) = setValue(inter, method, arg0, false)
    private suspend fun <P0> setValue(inter : Interface, method : KSuspendFunction1<P0, Unit>, arg0 : P0, isSupported : Boolean) {
        try {
            method(arg0)
            results.add(
                CameraTestResult(
                    "$inter.${method.name}",
                    arg0.toString(),
                    true,
                    isSupported
                                )
                       )
        } catch (e: DJIErrorException) {
            results.add(
                CameraTestResult(
                    "$inter.${method.name}",
                    arg0.toString(),
                    false,
                    isSupported,
                    e.description
                                )
                       )
        }
    }
    private suspend fun <R> getValue(inter : Interface, method : KSuspendFunction0<R>, isSupported: Boolean){
        try {
            val res : R? = method()
            results.add(
                CameraTestResult(
                    "$inter.${method.name}",
                    res?.toString() ?: "UNKNOWN",
                    true,
                    isSupported
                                )
                       )
        } catch (e: DJIErrorException) {
            results.add(
                CameraTestResult(
                    "$inter.${method.name}",
                    "UNKNOWN",
                    false,
                    isSupported,
                    e.description
                                )
                       )
        }
    }
    private suspend fun <R> getValue(inter : Interface, method : KSuspendFunction0<R>) = getValue(inter, method, false)
    @RequiresApi(Build.VERSION_CODES.N)
    private fun <R> getValueFeature(inter : Interface, method : KFunction0<R>) = getValueFeature(inter,method,false)
    @RequiresApi(Build.VERSION_CODES.N)
    private fun <R> getValueFeature(inter : Interface, method : KFunction0<R>, isSupported: Boolean){
        try {
            val feature : R? = method()
            if (feature is CompletableFuture<*>) {
                val res = feature.get()
                results.add(
                    CameraTestResult(
                        "$inter.${method.name}",
                        res.toString(),
                        true,
                        isSupported
                                    )
                           )
            } else {
                results.add(
                    CameraTestResult(
                        "$inter.${method.name}",
                        feature.toString(),
                        true,
                        isSupported
                                    )
                           )
            }
        } catch (e: ExecutionException) {
            results.add(
                CameraTestResult(
                    "$inter.${method.name}",
                    "UNKNOWN",
                    false,
                    isSupported,
                    (e.cause as DJIErrorException).description
                                )
                       )
        }
    }
    @RequiresApi(Build.VERSION_CODES.N)
    private fun <P0> setValueFeature(inter : Interface, method : KFunction1<P0, CompletableFuture<Void>>, arg0 : P0) = setValueFeature(inter, method, arg0, false)
    @RequiresApi(Build.VERSION_CODES.N)
    private fun <P0> setValueFeature(inter : Interface, method : KFunction1<P0, CompletableFuture<Void>>, arg0 : P0, isSupported : Boolean) {
        try {
            val res = method(arg0)
            res.get()
            results.add(
                CameraTestResult(
                    "$inter.${method.name}",
                    arg0.toString(),
                    true,
                    isSupported
                                )
                       )
        } catch (e: ExecutionException) {
            
            results.add(
                CameraTestResult(
                    "$inter.${method.name}",
                    arg0.toString(),
                    false,
                    isSupported,
                    (e.cause as DJIErrorException).description
                                )
                       )
        }
    }
}
