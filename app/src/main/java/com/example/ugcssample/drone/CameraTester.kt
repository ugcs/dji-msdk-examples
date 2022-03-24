package com.example.ugcssample.drone

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.ugcssample.DJIErrorException
import com.example.ugcssample.drone.camera.Camera
import com.example.ugcssample.drone.camera.Lens
import com.example.ugcssample.drone.camera.settings.camera.*
import com.example.ugcssample.drone.camera.settings.lens.*
import com.example.ugcssample.drone.camera.settings.lens.ResolutionAndFrameRate as ResolutionAndFrameRateModel
import dji.common.camera.ResolutionAndFrameRate
import dji.keysdk.CameraKey
import dji.sdk.sdkmanager.DJISDKManager
import timber.log.Timber
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KSuspendFunction0
import kotlin.reflect.KSuspendFunction1

class CameraTester(val camera : Camera, val onProgressUpdate : (Int) -> Unit) {
    private val cameraResults = mutableListOf<CameraTestCaseReport>()
    private val lenses = mutableListOf<LensTestInfo>()
    val cameraName = camera.getDisplayName()
    enum class Interface {CAMERA, LENS}
    val currentState = CameraState()
    var testIndex = 0
    @RequiresApi(Build.VERSION_CODES.N)
    suspend fun runTests() : CameraResultsInfo {
        cameraResults.clear()
        for (shootPhotoMode in ShootPhotoMode.values()) {
            currentState.shootPhotoMode = shootPhotoMode
            cameraResults.add(setValue(Interface.CAMERA, camera::setShootPhotoMode, shootPhotoMode))
            cameraResults.addAll(testCamera())
        }
        camera.getLenses().forEach { lens ->
            if (lens != null) {
                try {
                    camera.setActiveLens(lens)
                } catch (e: DJIErrorException) {
                    Timber.i("Failed to activate lens $lens")
                }
                val displayModes = lens.supportedDisplayModes
                val lensTests = mutableListOf<CameraTestCaseReport>()
                for (lensDisplayMode in DisplayMode.values()) {
                    currentState.lensDisplayMode = lensDisplayMode
                    lensTests.add(setValueFeature(Interface.LENS, lens::setDisplayMode, lensDisplayMode, displayModes.contains(lensDisplayMode)))
                    lensTests.addAll(testLens(lens))
//                    setValueFeature(Interface.LENS, lens::setDisplayMode, DisplayMode.VISUAL_ONLY, displayModes.contains(DisplayMode.VISUAL_ONLY))
                }
                lenses.add(
                    LensTestInfo(type = lens.type,
                                 name = lens.name,
                                 index = lens.id,
                                 lensTests)
                          )
            }
        }
        testKeymanagerData(CameraKey.VIDEO_FILE_FORMAT_RANGE)
        testKeymanagerData(CameraKey.PHOTO_FILE_FORMAT_RANGE)
        testKeymanagerData(CameraKey.VIDEO_STANDARD_RANGE)
        testKeymanagerData(CameraKey.PHOTO_ASPECT_RATIO_RANGE)
        return CameraResultsInfo(cameraName, cameraResults, lenses)
    }
    
    private fun testKeymanagerData(keyName: String) {
        val km = DJISDKManager.getInstance().keyManager
        val djiKey = CameraKey.create(keyName)
        if (djiKey == null) {
            Timber.e("Can't create CameraKey ${djiKey.toString()}.")
        }
        val sdkModes = km?.getValue(djiKey) as Array<*>?
        val modes = sdkModes?.joinToString(separator = ",") { it.toString() }
        cameraResults.add(
            CameraTestCaseReport(
                testIndex,
                djiKey.toString(),
                modes ?: "",
                modes != null,
                false,
                currentState.copy()
                                )
                         )
        testSupportedResolutionAndFrameRate()
    }
    
    private suspend fun testCamera() : List<CameraTestCaseReport> {
        val list = mutableListOf<CameraTestCaseReport>()
        val supportedCameraModes = camera.getSupportedCameraModes()
        for (mode in CameraMode.values()) {
            list.add(setValue(Interface.CAMERA, camera::setCameraMode, mode, supportedCameraModes.contains(mode)))
        }
        list.add(setValue(Interface.CAMERA, camera::setCameraMode, CameraMode.SHOOT_PHOTO, supportedCameraModes.contains(CameraMode.SHOOT_PHOTO)))
        for (mode in PhotoAEBCount.values()) {
            list.add(setValue(Interface.CAMERA, camera::setPhotoAEBCount, mode))
        }
        for (mode in PhotoBurstCount.values()) {
            list.add(setValue(Interface.CAMERA, camera::setPhotoBurstCount, mode))
        }
    
        for (count in 0..15) {
            for (interval in 0..10) {
                list.add(setValue(Interface.CAMERA, camera::setPhotoTimeIntervalSettings, PhotoTimeIntervalSettings(count, interval)))
            }
        }
        list.add(getValue(Interface.CAMERA, camera::isThermalCamera))
        list.add(getValue(Interface.CAMERA, camera::getThermalIsothermEnabled))
        list.add(getValue(Interface.CAMERA, camera::getThermalIsothermLowerValue))
        list.add(getValue(Interface.CAMERA, camera::getThermalIsothermUpperValue))
        list.add(getValue(Interface.CAMERA, camera::getFocusAssistantSettings))
        list.add(getValue(Interface.CAMERA, camera::getFocusMode))
        list.add(getValue(Interface.CAMERA, camera::getFocusRingValue))
        list.add(getValue(Interface.CAMERA, camera::getFocusTarget))
        list.add(getValue(Interface.CAMERA, camera::isFlatCameraModeSupported))
        for (mode in FlatCameraMode.values()) {
            list.add(setValue(Interface.CAMERA, camera::setFlatMode, mode))
        }
        setValue(Interface.CAMERA, camera::setFlatMode, FlatCameraMode.PHOTO_SINGLE)
        for (mode in ThermalDigitalZoomFactor.values()) {
            list.add(setValue(Interface.CAMERA, camera::setThermalDigitalZoomFactor, mode))
        }
        list.add(setValue(Interface.CAMERA, camera::setThermalDigitalZoomFactor, ThermalDigitalZoomFactor.X_1))
        return list
    }
    private fun testSupportedResolutionAndFrameRate() {
        val sdkRange: Array<out ResolutionAndFrameRate>? = camera.getDjiCamera()?.capabilities?.videoResolutionAndFrameRateRange()
        if (sdkRange == null) {
            Timber.w("Video resolution and frame rate range is null")
        }
        val data = sdkRange?.joinToString(separator = ",") { "fov:${it.fov} frameRate:${it.frameRate} resolution:${it.resolution}" }
        cameraResults.add(
            CameraTestCaseReport(
                testIndex,
                "Camera.videoResolutionAndFrameRateRange",
                data ?: "",
                data != null,
                false,
                currentState.copy()
                                )
                         )
    }
    @RequiresApi(Build.VERSION_CODES.N)
    private fun testLens(lens: Lens) : List<CameraTestCaseReport> {
        val list = mutableListOf<CameraTestCaseReport>()
        for (mode in AntiFlickerFrequency.values()) {
            list.add(setValueFeature(Interface.LENS, lens::setAntiFlickerFrequency, mode))
        }
        list.add(setValueFeature(Interface.LENS, lens::setAntiFlickerFrequency, AntiFlickerFrequency.AUTO))
    
        val supportedApertures = lens.supportedApertures
        for (mode in Aperture.values()) {
            list.add(setValueFeature(Interface.LENS, lens::setAperture, mode, supportedApertures.contains(mode)))
        }
        list.add(setValueFeature(Interface.LENS, lens::setAperture, supportedApertures.firstOrNull() ?: Aperture.F_10))
    
        val supportedExposureModes = lens.supportedExposureModes
        for (mode in ExposureMode.values()) {
            list.add(setValueFeature(Interface.LENS, lens::setExposureMode, mode, supportedExposureModes.contains(mode)))
        }
        list.add(setValueFeature(Interface.LENS, lens::setExposureMode, supportedExposureModes.firstOrNull() ?: ExposureMode.CINE))
    
        val supportedExposureCompensations = lens.supportedExposureCompensations
        for (mode in ExposureCompensation.values()) {
            list.add(setValueFeature(Interface.LENS, lens::setExposureCompensation, mode, supportedExposureCompensations.contains(mode)))
        }
        list.add(setValueFeature(Interface.LENS, lens::setExposureCompensation, supportedExposureCompensations.firstOrNull() ?: ExposureCompensation.FIXED))
        
        list.add(setValueFeature(Interface.LENS, lens::setFocusAssistantSettings, FocusAssistantSettings(enabledMF = false, enabledAF = false)))
        list.add(setValueFeature(Interface.LENS, lens::setFocusAssistantSettings, FocusAssistantSettings(enabledMF = true, enabledAF = false)))
        list.add(setValueFeature(Interface.LENS, lens::setFocusAssistantSettings, FocusAssistantSettings(enabledMF = false, enabledAF = true)))
        list.add(setValueFeature(Interface.LENS, lens::setFocusAssistantSettings, FocusAssistantSettings(enabledMF = true, enabledAF = true)))
    
        val supportedISOs = lens.supportedISOs
        for (mode in ISO.values()) {
            list.add(setValueFeature(Interface.LENS, lens::setISO, mode, supportedISOs.contains(mode)))
        }
        list.add(setValueFeature(Interface.LENS, lens::setISO, supportedISOs.firstOrNull() ?: ISO.AUTO))
        for (mode in WhiteBalance.WhiteBalancePreset.values()) {
            list.add(setValueFeature(Interface.LENS, lens::setWhiteBalance, WhiteBalance(mode)))
        }
        list.add(setValueFeature(Interface.LENS, lens::setWhiteBalance, WhiteBalance(WhiteBalance.WhiteBalancePreset.AUTO)))
    
        list.add(getValueFeature(Interface.LENS, lens::getThermalIsothermEnabled))
    
        list.add(getValueFeature(Interface.LENS, lens::getThermalIsothermEnabled))
        list.add(getValueFeature(Interface.LENS, lens::getThermalIsothermLowerValue))
        list.add(getValueFeature(Interface.LENS, lens::getThermalIsothermUpperValue))
        //        getValue(camera::getFocusAssistantSettings)
        list.add(getValueFeature(Interface.LENS, lens::getFocusMode))
        list.add(getValueFeature(Interface.LENS, lens::getFocusRingValue))
        list.add(getValueFeature(Interface.LENS, lens::getFocusTarget))
        list.add(getValueFeature(Interface.LENS, lens::isThermalLens))
        list.add(getValueFeature(Interface.LENS, lens::getSupportedFlatCameraModes))
    
    
        for (mode in ThermalDigitalZoomFactor.values()) {
            list.add(setValueFeature(Interface.LENS, lens::setThermalDigitalZoomFactor, mode))
        }
        return list
    }
    
    private suspend fun <P0> setValue(inter : Interface, method : KSuspendFunction1<P0, Unit>, arg0 : P0) = setValue(inter, method, arg0, false)
    private suspend fun <P0> setValue(inter : Interface, method : KSuspendFunction1<P0, Unit>, arg0 : P0, isSupported : Boolean): CameraTestCaseReport {
        onProgressUpdate.invoke(++testIndex)
        try {
            method(arg0)
            return CameraTestCaseReport(
                    testIndex,
                    "$inter.${method.name}",
                    arg0.toString(),
                    true,
                    isSupported,
                    currentState.copy()
                                    )
        } catch (e: DJIErrorException) {
            return CameraTestCaseReport(
                    testIndex,
                    "$inter.${method.name}",
                    arg0.toString(),
                    false,
                    isSupported,
                    currentState.copy(),
                    e.description
                                    )
        }
    }
    private suspend fun <R> getValue(inter : Interface, method : KSuspendFunction0<R>, isSupported: Boolean): CameraTestCaseReport {
        onProgressUpdate.invoke(++testIndex)
        try {
            val res : R? = method()
            return CameraTestCaseReport(
                    testIndex,
                    "$inter.${method.name}",
                    res?.toString() ?: "UNKNOWN",
                    true,
                    isSupported,
                    currentState.copy()
                                    )
        } catch (e: DJIErrorException) {
            return CameraTestCaseReport(
                    testIndex,
                    "$inter.${method.name}",
                    "UNKNOWN",
                    false,
                    isSupported,
                    currentState.copy(),
                    e.description
                                    )
        }
    }
    private suspend fun <R> getValue(inter : Interface, method : KSuspendFunction0<R>) = getValue(inter, method, false)
    @RequiresApi(Build.VERSION_CODES.N)
    private fun <R> getValueFeature(inter : Interface, method : KFunction0<R>) = getValueFeature(inter,method,false)
    @RequiresApi(Build.VERSION_CODES.N)
    private fun <R> getValueFeature(inter : Interface, method : KFunction0<R>, isSupported: Boolean): CameraTestCaseReport {
        onProgressUpdate.invoke(++testIndex)
        try {
            val feature : R? = method()
            if (feature is CompletableFuture<*>) {
                val res = feature.get()
                return    CameraTestCaseReport(
                        testIndex,
                        "$inter.${method.name}",
                        res.toString(),
                        true,
                        isSupported,
                        currentState.copy()
                                        )
            } else {
                return    CameraTestCaseReport(
                        testIndex,
                        "$inter.${method.name}",
                        feature.toString(),
                        true,
                        isSupported,
                        currentState.copy()
                                        )
            }
        } catch (e: ExecutionException) {
            return    CameraTestCaseReport(
                    testIndex,
                    "$inter.${method.name}",
                    "UNKNOWN",
                    false,
                    isSupported,
                    currentState.copy(),
                    (e.cause as DJIErrorException).description
                                    )
        }
    }
    @RequiresApi(Build.VERSION_CODES.N)
    private fun <P0> setValueFeature(inter : Interface, method : KFunction1<P0, CompletableFuture<Void>>, arg0 : P0) = setValueFeature(inter, method, arg0, false)
    @RequiresApi(Build.VERSION_CODES.N)
    private fun <P0> setValueFeature(inter : Interface, method : KFunction1<P0, CompletableFuture<Void>>, arg0 : P0, isSupported : Boolean): CameraTestCaseReport {
        onProgressUpdate.invoke(++testIndex)
        try {
            val res = method(arg0)
            res.get()
            return CameraTestCaseReport(
                    testIndex,
                    "$inter.${method.name}",
                    arg0.toString(),
                    true,
                    isSupported,
                    currentState.copy()
                                    )
                       
        } catch (e: ExecutionException) {
            
            return CameraTestCaseReport(
                    testIndex,
                    "$inter.${method.name}",
                    arg0.toString(),
                    false,
                    isSupported,
                    currentState.copy(),
                    (e.cause as DJIErrorException).description
                                    )
        }
    }
}
