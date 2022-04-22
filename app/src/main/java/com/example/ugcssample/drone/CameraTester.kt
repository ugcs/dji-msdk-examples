package com.example.ugcssample.drone

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.ugcssample.DJIErrorException
import com.example.ugcssample.drone.camera.Camera
import com.example.ugcssample.drone.camera.Lens
import com.example.ugcssample.drone.camera.settings.camera.*
import com.example.ugcssample.drone.camera.settings.lens.*
import dji.common.camera.ResolutionAndFrameRate
import dji.common.util.CommonCallbacks
import dji.keysdk.CameraKey
import dji.sdk.sdkmanager.DJISDKManager
import timber.log.Timber
import java8.util.concurrent.CompletableFuture
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KSuspendFunction0
import kotlin.reflect.KSuspendFunction1
import dji.common.error.DJIError
import java.util.concurrent.ExecutionException
import com.example.ugcssample.drone.suspendCoroutine as DjiSuspend

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
        list.apply {
            val supportedCameraModes = camera.getSupportedCameraModes()
            for (mode in CameraMode.values())
            {
                add(setValue(Interface.CAMERA, camera::setCameraMode, mode, supportedCameraModes.contains(mode)))
            }
            add(setValue(Interface.CAMERA, camera::setCameraMode, CameraMode.SHOOT_PHOTO, supportedCameraModes.contains(CameraMode.SHOOT_PHOTO)))
            for (mode in PhotoAEBCount.values())
            {
                add(setValue(Interface.CAMERA, camera::setPhotoAEBCount, mode))
            }
            for (mode in PhotoBurstCount.values())
            {
                add(setValue(Interface.CAMERA, camera::setPhotoBurstCount, mode))
            }
    
            for (count in 0..15)
            {
                for (interval in 0..10)
                {
                    add(setValue(Interface.CAMERA, camera::setPhotoTimeIntervalSettings, PhotoTimeIntervalSettings(count, interval)))
                }
            }
            add(getValue(Interface.CAMERA, camera::isThermalCamera))
            add(getValue(Interface.CAMERA, camera::getThermalIsothermEnabled))
            add(getValue(Interface.CAMERA, camera::getThermalIsothermLowerValue))
            add(getValue(Interface.CAMERA, camera::getThermalIsothermUpperValue))
            add(getValue(Interface.CAMERA, camera::getFocusAssistantSettings))
            add(getValue(Interface.CAMERA, camera::getFocusMode))
            add(getValue(Interface.CAMERA, camera::getFocusRingValue))
            add(getValue(Interface.CAMERA, camera::getFocusTarget))
            add(getValue(Interface.CAMERA, camera::isFlatCameraModeSupported))
            val djiCamera = camera.getDjiCamera()!!
            add(getValueDirect(Interface.CAMERA, djiCamera::getAELock))
            add(getValueDirect(Interface.CAMERA, djiCamera::getAntiFlickerFrequency))
            add(getValueDirect(Interface.CAMERA, djiCamera::getAperture))
            add(getValueDirect(Interface.CAMERA, djiCamera::getAudioGain))
            add(getValueDirect(Interface.CAMERA, djiCamera::getAudioRecordingEnabled))
            add(getValueDirect(Interface.CAMERA, djiCamera::getAutoLockGimbalEnabled))
            add(getValueDirect(Interface.CAMERA, djiCamera::getBeaconAutoTurnOffEnabled))
            add(getValueDirect(Interface.CAMERA, djiCamera::getCameraVideoStreamSource))
            add(getValueDirect(Interface.CAMERA, djiCamera::getCaptureCameraStreamSettings))
            add(getValueDirect(Interface.CAMERA, djiCamera::getColor))
            add(getValueDirect(Interface.CAMERA, djiCamera::getContrast))
            add(getValueDirect(Interface.CAMERA, djiCamera::getCustomExpandDirectoryName))
            add(getValueDirect(Interface.CAMERA, djiCamera::getCustomExpandFileName))
            add(getValueDirect(Interface.CAMERA, djiCamera::getDefogEnabled))
            add(getValueDirect(Interface.CAMERA, djiCamera::getDewarpingEnabled))
            add(getValueDirect(Interface.CAMERA, djiCamera::getDigitalZoomFactor))
            add(getValueFeature(Interface.CAMERA, djiCamera::isSuperResolutionSupported))
            add(getValueFeature(Interface.CAMERA, djiCamera::isAFCSupported))
            add(getValueFeature(Interface.CAMERA, djiCamera::isAdjustableApertureSupported))
            add(getValueFeature(Interface.CAMERA, djiCamera::isAdjustableApertureSupported))
            for (mode in FlatCameraMode.values())
            {
                add(setValue(Interface.CAMERA, camera::setFlatMode, mode))
            }
            setValue(Interface.CAMERA, camera::setFlatMode, FlatCameraMode.PHOTO_SINGLE)
            for (mode in ThermalDigitalZoomFactor.values())
            {
                add(setValue(Interface.CAMERA, camera::setThermalDigitalZoomFactor, mode))
            }
            add(setValue(Interface.CAMERA, camera::setThermalDigitalZoomFactor, ThermalDigitalZoomFactor.X_1))
        }
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
    private suspend fun testLens(lens: Lens) : List<CameraTestCaseReport> {
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
        val djiLens = lens.djiLens
        list.add(getValueFeature(Interface.LENS, djiLens::isAdjustableApertureSupported))
        list.add(getValueFeature(Interface.LENS, djiLens::isAdjustableFocalPointSupported))
        list.add(getValueFeature(Interface.LENS, djiLens::isDigitalZoomSupported))
        list.add(getValueFeature(Interface.LENS, djiLens::isHybridZoomSupported))
        list.add(getValueFeature(Interface.LENS, djiLens::isMechanicalShutterSupported))
        list.add(getValueFeature(Interface.LENS, djiLens::isOpticalZoomSupported))
        list.add(getValueFeature(Interface.LENS, djiLens::isThermalLens))
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
    private suspend fun <R> getValue(inter : Interface, method : KSuspendFunction0<R>) = getValue(inter, method, false)
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
    private suspend fun <R> getValueDirect(inter : Interface, method : KFunction1<CommonCallbacks.CompletionCallbackWith<R>, Unit>) = getValueDirect(inter, method, false)
    private suspend fun <R> getValueDirect(inter : Interface, method : KFunction1<CommonCallbacks.CompletionCallbackWith<R>, Unit>, isSupported: Boolean) : CameraTestCaseReport {
        onProgressUpdate.invoke(++testIndex)
        try {
            val res : R? = DjiSuspend(method)
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
    private fun <R> getValueFeature(inter : Interface, method : KFunction0<R>) = getValueFeature(inter,method,false)
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
    private fun <P0> setValueFeature(inter : Interface, method : KFunction1<P0, CompletableFuture<Void>>, arg0 : P0) = setValueFeature(inter, method, arg0, false)
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
    private suspend fun <P0> setValueDirect(inter : Interface, method : KFunction2<P0, CommonCallbacks.CompletionCallback<DJIError>?, Unit>, arg0 : P0) = setValueDirect(inter, method, arg0, false)
    private suspend fun <P0> setValueDirect(inter : Interface, method : KFunction2<P0, CommonCallbacks.CompletionCallback<DJIError>?, Unit>, arg0 : P0, isSupported: Boolean) : CameraTestCaseReport {
        onProgressUpdate.invoke(++testIndex)
        try {
            val res = suspendCoroutineCompletion(method, arg0)
            return CameraTestCaseReport(
                    testIndex,
                    "$inter.${method.name}",
                    res.toString() ?: "UNKNOWN",
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
    
}
