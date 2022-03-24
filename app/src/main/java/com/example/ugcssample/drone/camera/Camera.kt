package com.example.ugcssample.drone.camera

import android.graphics.PointF
import com.example.ugcssample.drone.camera.settings.camera.*
import com.example.ugcssample.drone.camera.settings.lens.ThermalDigitalZoomFactor
import dji.common.camera.SettingsDefinitions
import dji.sdk.camera.Camera
import dji.sdk.media.MediaManager

interface Camera {
    interface Callback {
        /**
         *
         * @param error Null if process is executed successfully
         */
        fun run(error: Exception?)
    }

    interface ValueChangeListener<T> {
        open fun run(newValue: T?)
    }

    open fun isSingleLens(): Boolean
    open fun getLenses(): MutableSet<Lens?>
    open suspend fun setActiveLens(lens : Lens)
    open fun getActiveLens(): Lens
    open fun getDjiCamera() : Camera?
    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    open fun getCameraMode(): CameraMode?
    open suspend fun setCameraMode(cameraMode: CameraMode)
    open fun addCameraModeChangeListener(listener: ValueChangeListener<CameraMode?>)
    open fun removeCameraModeChangeListener(listener: ValueChangeListener<CameraMode?>)
    open fun getSupportedCameraModes(): MutableList<CameraMode?>

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    open fun getPhotoAEBCount(): PhotoAEBCount?
    open suspend fun setPhotoAEBCount(photoAEBCount: PhotoAEBCount)
    open fun addPhotoAEBCountChangeListener(photoAEBCountChangeListener: ValueChangeListener<PhotoAEBCount?>)
    open fun removePhotoAEBCountChangeListener(photoAEBCountChangeListener: ValueChangeListener<PhotoAEBCount?>)

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    open fun getPhotoBurstCount(): PhotoBurstCount?
    suspend fun setPhotoBurstCount(photoBurstCount: PhotoBurstCount)
    open fun addPhotoBurstCountChangeListener(listener: ValueChangeListener<PhotoBurstCount?>)
    open fun removePhotoBurstCountChangeListener(listener: ValueChangeListener<PhotoBurstCount?>)

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    open fun getPhotoTimeIntervalSettings(): PhotoTimeIntervalSettings?
    suspend fun setPhotoTimeIntervalSettings(value: PhotoTimeIntervalSettings)
    open fun addPhotoTimeIntervalChangeListener(listener: ValueChangeListener<PhotoTimeIntervalSettings?>)
    open fun removePhotoTimeIntervalChangeListener(listener: ValueChangeListener<PhotoTimeIntervalSettings?>)

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    open fun getShootPhotoMode(): ShootPhotoMode?
    open suspend fun setShootPhotoMode(mode: ShootPhotoMode)
    open fun addShootPhotoModeChangeListener(listener: ValueChangeListener<ShootPhotoMode?>)
    open fun removeShootPhotoModeChangeListener(listener: ValueChangeListener<ShootPhotoMode?>)
    open fun getSupportedShootPhotoModes(): MutableList<ShootPhotoMode?>
    open fun getDisplayName(): String
    open fun isInitialised(): Boolean

    /**
     * Callback is called immediately if camera is already initialised
     */
    open fun addOnInitialisedListener(listener: Callback)
    open fun removeOnInitialisedListener(listener: Callback)
    open fun addActiveLensChangeListener(listener: ValueChangeListener<Lens?>)
    open fun removeActiveLensChangeListener(listener: ValueChangeListener<Lens?>)

    /**
     * In single photo mode shooting photo is ended after take one photo
     */
    open fun startShootPhoto(onStart: Callback?)
    open fun stopShootPhoto(onStop: Callback?)
    open fun startRecordVideo(onStart: Callback?)
    open fun stopRecordVideo(onStop: Callback?)

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    open fun isVideoRecording(): Boolean?
    open fun addVideoRecordingChangeListener(listener: ValueChangeListener<Boolean?>?)
    open fun removeVideoRecordingChangeListener(listener: ValueChangeListener<Boolean?>?)

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    open fun isShootingSinglePhoto(): Boolean?
    open fun addShootingSinglePhotoChangeListener(listener: ValueChangeListener<Boolean?>?)
    open fun removeShootingSinglePhotoChangeListener(listener: ValueChangeListener<Boolean?>?)

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    open fun isShootingBurstPhoto(): Boolean?
    open fun addShootingBurstPhotoChangeListener(listener: ValueChangeListener<Boolean?>?)
    open fun removeShootingBurstPhotoChangeListener(listener: ValueChangeListener<Boolean?>?)

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    open fun isShootingIntervalPhoto(): Boolean?
    open fun addShootingIntervalPhotoChangeListener(listener: ValueChangeListener<Boolean?>?)
    open fun removeShootingIntervalPhotoChangeListener(listener: ValueChangeListener<Boolean?>?)

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    open fun isStoringPhoto(): Boolean?
    open fun addStoringPhotoChangeListener(listener: ValueChangeListener<Boolean?>?)
    open fun removeStoringPhotoChangeListener(listener: ValueChangeListener<Boolean?>?)

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    open fun getCurrentVideoRecordingTimeInSeconds(): Int?
    open fun addCurrentVideoRecordingTimeInSecondsChangeListener(listener: ValueChangeListener<Int?>?)
    open fun removeCurrentVideoRecordingTimeInSecondsChangeListener(listener: ValueChangeListener<Int?>?)

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    open fun getMediaManager(): MediaManager?
    open fun setXmpInformation(information: String?, onSet: Callback?)
    
    suspend fun getThermalIsothermEnabled() : Boolean
    suspend fun getThermalIsothermLowerValue(): Int
    suspend fun getThermalIsothermUpperValue(): Int
    suspend fun getFocusAssistantSettings(): Pair<Boolean, Boolean>
    suspend fun getFocusMode(): SettingsDefinitions.FocusMode?
    suspend fun getFocusRingValue(): Int
    suspend fun getFocusTarget(): PointF?
    suspend fun isFlatCameraModeSupported(): Boolean?
    suspend fun setFlatMode(flatCameraMode: FlatCameraMode)
    suspend fun isThermalCamera(): Boolean?
    suspend fun setThermalDigitalZoomFactor(thermalDigitalZoomFactor: ThermalDigitalZoomFactor)
}