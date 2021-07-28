package com.example.ugcssample.drone.camera

import com.example.ugcssample.drone.camera.settings.camera.*
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
    open fun setActiveLens(lensId: Int, onSet: Callback?)
    open fun getActiveLens(): Lens

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    open fun getCameraMode(): CameraMode?
    open fun setCameraMode(cameraMode: CameraMode, onSet: Callback?)
    open fun addCameraModeChangeListener(listener: ValueChangeListener<CameraMode?>)
    open fun removeCameraModeChangeListener(listener: ValueChangeListener<CameraMode?>)
    open fun getSupportedCameraModes(): MutableList<CameraMode?>

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    open fun getPhotoAEBCount(): PhotoAEBCount?
    open fun setPhotoAEBCount(photoAEBCount: PhotoAEBCount, onSet: Callback?)
    open fun addPhotoAEBCountChangeListener(photoAEBCountChangeListener: ValueChangeListener<PhotoAEBCount?>)
    open fun removePhotoAEBCountChangeListener(photoAEBCountChangeListener: ValueChangeListener<PhotoAEBCount?>)

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    open fun getPhotoBurstCount(): PhotoBurstCount?
    open fun setPhotoBurstCount(photoBurstCount: PhotoBurstCount, onSet: Callback?)
    open fun addPhotoBurstCountChangeListener(listener: ValueChangeListener<PhotoBurstCount?>)
    open fun removePhotoBurstCountChangeListener(listener: ValueChangeListener<PhotoBurstCount?>)

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    open fun getPhotoTimeIntervalSettings(): PhotoTimeIntervalSettings?
    open fun setPhotoTimeIntervalSettings(value: PhotoTimeIntervalSettings, onSet: Callback?)
    open fun addPhotoTimeIntervalChangeListener(listener: ValueChangeListener<PhotoTimeIntervalSettings?>)
    open fun removePhotoTimeIntervalChangeListener(listener: ValueChangeListener<PhotoTimeIntervalSettings?>)

    /**
     * Could return null if camera is not initialised.
     * If camera is initialised it returns null only if it is not supported by the camera.
     */
    open fun getShootPhotoMode(): ShootPhotoMode?
    open fun setShootPhotoMode(mode: ShootPhotoMode, onSet: Callback?)
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
}