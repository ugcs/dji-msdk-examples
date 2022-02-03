package com.example.ugcssample.drone

import com.example.ugcssample.drone.camera.Camera
import com.example.ugcssample.drone.camera.settings.camera.*
import com.example.ugcssample.drone.camera.settings.lens.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.EnumSet.range
import kotlin.coroutines.suspendCoroutine

class CameraTester(val camera : Camera, val onFinishCallback :  (List<CameraTestResult>)->Unit) {

    init {
        GlobalScope.launch {
            runTests()
        }
    }

    private suspend fun runTests() {
        val results = mutableListOf<CameraTestResult>()
        val cameraName = camera.getDisplayName()
        val supportedCameraModes = camera.getSupportedCameraModes()
        for (mode in CameraMode.values()) {
            suspendCoroutine<Boolean> {
                camera.setCameraMode(mode, object : Camera.Callback {
                    override fun run(error: Exception?) {
                        results.add(
                            CameraTestResult(
                                cameraName,
                                "setCameraMode",
                                mode.name,
                                error == null,
                                supportedCameraModes.contains(mode),
                                error.toString()
                                            )
                                   )
                        Result.success(true)
                    }
                })
            }
        }
        for (mode in PhotoAEBCount.values()) {
            suspendCoroutine<Boolean> {
                camera.setPhotoAEBCount(mode, object : Camera.Callback {
                    override fun run(error: Exception?) {
                        results.add(
                            CameraTestResult(
                                cameraName,
                                "setPhotoAEBCount",
                                mode.name,
                                error == null,
                                false,
                                error.toString()
                                            )
                                   )
                        Result.success(true)
                    }
                })
            }
        }
        for (mode in PhotoBurstCount.values()) {
            suspendCoroutine<Boolean> {
                camera.setPhotoBurstCount(mode, object : Camera.Callback {
                    override fun run(error: Exception?) {
                        results.add(
                            CameraTestResult(
                                cameraName,
                                "setPhotoBurstCount",
                                mode.name,
                                error == null,
                                false,
                                error.toString()
                                            )
                                   )
                        Result.success(true)
                    }
                })
            }
        }
        for (count in 0..15) {
            for (interval in 0..10) {
                suspendCoroutine<Boolean> {
                    camera.setPhotoTimeIntervalSettings(PhotoTimeIntervalSettings(count, interval),
                                                        object : Camera.Callback {
                                                            override fun run(error: Exception?) {
                                                                results.add(
                                                                    CameraTestResult(
                                                                        cameraName,
                                                                        "setPhotoBurstCount",
                                                                        "count=$count,interval=$interval",
                                                                        error == null,
                                                                        false,
                                                                        error.toString()
                                                                                    )
                                                                           )
                                                                Result.success(true)
                                                            }
                                                        })
                }
            }
        }
    
        for (mode in ShootPhotoMode.values()) {
            suspendCoroutine<Boolean> {
                camera.setShootPhotoMode(mode, object : Camera.Callback {
                    override fun run(error: Exception?) {
                        results.add(
                            CameraTestResult(
                                cameraName,
                                "setShootPhotoMode",
                                mode.name,
                                error == null,
                                false,
                                error.toString()
                                            )
                                   )
                        Result.success(true)
                    }
                })
            }
        }
    
        val lens = camera.getActiveLens()
        val displayModes = lens.supportedDisplayModes
        for (mode in DisplayMode.values()) {
            suspendCoroutine<Boolean> {
                lens.setDisplayMode(mode) {
                    results.add(
                        CameraTestResult(
                            cameraName,
                            "Lens.setDisplayMode",
                            mode.name,
                            it == null,
                            displayModes.contains(mode),
                            it.toString()
                                        )
                               )
                    Result.success(true)
                }
            }
        }
    
        for (mode in AntiFlickerFrequency.values()) {
            suspendCoroutine<Boolean> {
                lens.setAntiFlickerFrequency(mode) {
                    results.add(
                        CameraTestResult(
                            cameraName,
                            "Lens.setAntiFlickerFrequency",
                            mode.name,
                            it == null,
                            false,
                            it.toString()
                                        )
                               )
                    Result.success(true)
                }
            }
        }
    
        val supportedApertures = lens.supportedApertures
        for (mode in Aperture.values()) {
            suspendCoroutine<Boolean> {
                lens.setAperture(mode) {
                    results.add(
                        CameraTestResult(
                            cameraName,
                            "Lens.setAperture",
                            mode.name,
                            it == null,
                            supportedApertures.contains(mode),
                            it.toString()
                                        )
                               )
                    Result.success(true)
                }
            }
        }
    
        val supportedExposureModes = lens.supportedExposureModes
        for (mode in ExposureMode.values()) {
            suspendCoroutine<Boolean> {
                lens.setExposureMode(mode) {
                    results.add(
                        CameraTestResult(
                            cameraName,
                            "Lens.setExposureMode",
                            mode.name,
                            it == null,
                            supportedExposureModes.contains(mode),
                            it.toString()
                                        )
                               )
                    Result.success(true)
                }
            }
        }
    
        val supportedExposureCompensations = lens.supportedExposureCompensations
        for (mode in ExposureCompensation.values()) {
            suspendCoroutine<Boolean> {
                lens.setExposureCompensation(mode) {
                    results.add(
                        CameraTestResult(
                            cameraName,
                            "Lens.setExposureCompensation",
                            mode.name,
                            it == null,
                            supportedExposureCompensations.contains(mode),
                            it.toString()
                                        )
                               )
                    Result.success(true)
                }
            }
        }
        suspendCoroutine<Boolean> {
            lens.setFocusAssistantSettings(FocusAssistantSettings(enabledMF = false, enabledAF = false)) {
                results.add(
                    CameraTestResult(
                        cameraName,
                        "Lens.setFocusAssistantSettings",
                        "enabledMF=false,enabledAF=false",
                        it == null,
                        false,
                        it.toString()
                                    )
                           )
                Result.success(true)
            }
        }
        suspendCoroutine<Boolean> {
            lens.setFocusAssistantSettings(FocusAssistantSettings(enabledMF = true, enabledAF = false)) {
                results.add(
                    CameraTestResult(
                        cameraName,
                        "Lens.setFocusAssistantSettings",
                        "enabledMF=true,enabledAF=false",
                        it == null,
                        false,
                        it.toString()
                                    )
                           )
                Result.success(true)
            }
        }
        suspendCoroutine<Boolean> {
            lens.setFocusAssistantSettings(FocusAssistantSettings(enabledMF = false, enabledAF = true)) {
                results.add(
                    CameraTestResult(
                        cameraName,
                        "Lens.setFocusAssistantSettings",
                        "enabledMF=false,enabledAF=true",
                        it == null,
                        false,
                        it.toString()
                                    )
                           )
                Result.success(true)
            }
        }
        suspendCoroutine<Boolean> {
            lens.setFocusAssistantSettings(FocusAssistantSettings(enabledMF = true, enabledAF = true)) {
                results.add(
                    CameraTestResult(
                        cameraName,
                        "Lens.setFocusAssistantSettings",
                        "enabledMF=true,enabledAF=true",
                        it == null,
                        false,
                        it.toString()
                                    )
                           )
                Result.success(true)
            }
        }
    
        val supportedISOs = lens.supportedISOs
        for (mode in ISO.values()) {
            suspendCoroutine<Boolean> {
                lens.setISO(mode) {
                    results.add(
                        CameraTestResult(
                            cameraName,
                            "Lens.setISO",
                            mode.name,
                            it == null,
                            supportedISOs.contains(mode),
                            it.toString()
                                        )
                               )
                    Result.success(true)
                }
            }
        }
        
        for (mode in WhiteBalance.WhiteBalancePreset.values()) {
            suspendCoroutine<Boolean> {
                lens.setWhiteBalance(WhiteBalance(mode)) {
                    results.add(
                        CameraTestResult(
                            cameraName,
                            "Lens.setWhiteBalance",
                            mode.name,
                            it == null,
                            false,
                            it.toString()
                                        )
                               )
                    Result.success(true)
                }
            }
        }
        
    }

}