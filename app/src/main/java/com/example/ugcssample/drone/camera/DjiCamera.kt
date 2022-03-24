package com.example.ugcssample.drone.camera

import android.content.Context
import android.graphics.PointF
import com.example.ugcssample.BuildConfig
import com.example.ugcssample.drone.camera.settings.DjiCameraSettings
import com.example.ugcssample.drone.camera.settings.DjiLens
import com.example.ugcssample.drone.camera.settings.camera.*
import com.example.ugcssample.drone.camera.settings.lens.LensType
import com.example.ugcssample.drone.camera.settings.lens.ThermalDigitalZoomFactor
import com.example.ugcssample.drone.suspendCoroutineCompletion
import com.example.ugcssample.drone.suspendCoroutineTwo
import com.example.ugcssample.drone.suspendCoroutine as DjiSuspend
import dji.common.camera.CameraVideoStreamSource
import dji.common.camera.SettingsDefinitions
import dji.common.camera.SystemState
import dji.common.error.DJIError
import dji.common.util.CommonCallbacks.CompletionCallbackWith
import dji.keysdk.CameraKey
import dji.keysdk.KeyManager
import dji.sdk.media.MediaManager
import timber.log.Timber
import java.lang.NullPointerException
import java.util.*

class DjiCamera(camera: dji.sdk.camera.Camera?, context: Context?) : Camera {
    private val djiCamera: dji.sdk.camera.Camera?
    private val isVideoRecordingChangeListeners: MutableSet<Camera.ValueChangeListener<Boolean?>?>? =
        HashSet()
    private val isShootingSinglePhotoChangeListener: MutableSet<Camera.ValueChangeListener<Boolean?>?>? =
        HashSet()
    private val isShootingBurstPhotoChangeListeners: MutableSet<Camera.ValueChangeListener<Boolean?>?>? =
        HashSet()
    private val isShootingIntervalPhotoChangeListeners: MutableSet<Camera.ValueChangeListener<Boolean?>?>? =
        HashSet()
    private val isStoringPhotoChangeListeners: MutableSet<Camera.ValueChangeListener<Boolean?>?>? =
        HashSet()
    private val currentVideoRecordingTimeInSecondsChangeListeners: MutableSet<Camera.ValueChangeListener<Int?>?>? =
        HashSet()
    private val cameraModeChangeListeners: MutableSet<Camera.ValueChangeListener<CameraMode?>?>? =
        HashSet()
    private val photoAebCountListeners: MutableSet<Camera.ValueChangeListener<PhotoAEBCount?>?>? =
        HashSet()
    private val photoBurstChangeListeners: MutableSet<Camera.ValueChangeListener<PhotoBurstCount?>?>? =
        HashSet()
    private val photoTimeIntervalSettingsChangeListeners: MutableSet<Camera.ValueChangeListener<PhotoTimeIntervalSettings?>?>? =
        HashSet()
    private val photoModeChangeListeners: MutableSet<Camera.ValueChangeListener<ShootPhotoMode?>?>? =
        HashSet()
    private val lenses: MutableMap<Int?, Lens?>?
    private var currentLensId = 0
    private var initialisationError: Exception? = null
    private val lensInitialisationListener: Lens.Callback = Lens.Callback { error ->
        if (error == null) {
            onSettingsComponentInitialised()
        } else initialisationError(error)
    }
    private val initialisedListeners: MutableSet<Camera.Callback?> = HashSet()
    private val activeLensChangeListeners: MutableSet<Camera.ValueChangeListener<Lens?>?> =
        HashSet()
    private val settings: DjiCameraSettings = DjiCameraSettings()
    private var isInitialised = false
    private fun initSettings() {
        djiCamera?.setSystemStateCallback(SystemState.Callback { state: SystemState ->
            settings.cameraModeInited = true
            setCameraModeInternal(state.getMode())
            setIsShootingSinglePhoto(state.isShootingSinglePhoto())
            settings.isShootingSinglePhotoInited = true
            setIsShootingBurstPhoto(state.isShootingBurstPhoto())
            settings.isShootingBurstPhotoInited = true
            setIsShootingIntervalPhoto(state.isShootingIntervalPhoto())
            settings.isShootingIntervalPhotoInited = true
            setIsStoringPhoto(state.isStoringPhoto())
            settings.isStoringPhotoInited = true
            setCurrentVideoRecordingTimeInSeconds(state.getCurrentVideoRecordingTimeInSeconds())
            settings.currentVideoRecordingTimeInSecondsInited = true
            setIsVideoRecording(state.isRecording())
            settings.isVideoRecordingInited = true
            if (!isInitialised) onSettingsComponentInitialised()
        })
        djiCamera?.getCameraVideoStreamSource(object :
                                                 CompletionCallbackWith<CameraVideoStreamSource?> {
            override fun onSuccess(cameraVideoStreamSource: CameraVideoStreamSource?) {
                settings.activeSourceInited = true
                settings.activeSource = cameraVideoStreamSource
                onSettingsComponentInitialised()
            }

            override fun onFailure(djiError: DJIError?) {
                settings.activeSourceInited = true
                Timber.e(
                    "An error is raised on receiving camera active source: %s",
                    djiError?.getDescription()
                        )
                onSettingsComponentInitialised()
            }
        })
        djiCamera?.getMode(object : CompletionCallbackWith<SettingsDefinitions.CameraMode?> {
            override fun onSuccess(cameraMode: SettingsDefinitions.CameraMode?) {
                settings.cameraModeInited = true
                setCameraModeInternal(cameraMode)
                onSettingsComponentInitialised()
            }

            override fun onFailure(djiError: DJIError?) {
                settings.cameraModeInited = true
                Timber.e("An error is raised on receiving cameraMode: %s",djiError?.description)
                onSettingsComponentInitialised()
            }
        })
        djiCamera?.getPhotoAEBCount(object :
                                       CompletionCallbackWith<SettingsDefinitions.PhotoAEBCount?> {
            override fun onSuccess(photoAEBCount: SettingsDefinitions.PhotoAEBCount?) {
                settings.photoAEBCountInited = true
                setAebCountInternal(photoAEBCount)
                onSettingsComponentInitialised()
            }

            override fun onFailure(djiError: DJIError?) {
                settings.photoAEBCountInited = true
                Timber.e(
                    "An error is raised on receiving photoAEBCount: %s",
                    djiError?.getDescription()
                        )
                onSettingsComponentInitialised()
            }
        })
        djiCamera?.getPhotoBurstCount(object :
                                         CompletionCallbackWith<SettingsDefinitions.PhotoBurstCount?> {
            override fun onSuccess(photoBurstCount: SettingsDefinitions.PhotoBurstCount?) {
                settings.photoBurstCountInited = true
                setPhotoBurstCountInternal(photoBurstCount)
                onSettingsComponentInitialised()
            }

            override fun onFailure(djiError: DJIError?) {
                settings.photoBurstCountInited = true
                Timber.e(
                    "An error is raised on receiving photoBurstCount: %s",
                    djiError?.getDescription()
                        )
                onSettingsComponentInitialised()
            }
        })
        djiCamera?.getPhotoTimeIntervalSettings(object :
                                                   CompletionCallbackWith<SettingsDefinitions.PhotoTimeIntervalSettings?> {
            override fun onSuccess(photoTimeIntervalSettings: SettingsDefinitions.PhotoTimeIntervalSettings?) {
                settings.photoTimeIntervalSettingsInited = true
                setPhotoTimeIntervalSettingsInternal(photoTimeIntervalSettings)
                onSettingsComponentInitialised()
            }

            override fun onFailure(djiError: DJIError?) {
                settings.photoTimeIntervalSettingsInited = true
                Timber.e(
                    "An error is raised on receiving photoTimeIntervalSettings: %s",
                    djiError?.getDescription()
                        )
                onSettingsComponentInitialised()
            }
        })
        djiCamera?.getShootPhotoMode(object :
                                        CompletionCallbackWith<SettingsDefinitions.ShootPhotoMode?> {
            override fun onSuccess(shootPhotoMode: SettingsDefinitions.ShootPhotoMode?) {
                settings.shootPhotoModeInited = true
                setPhotoModeInternal(shootPhotoMode)
                onSettingsComponentInitialised()
            }

            override fun onFailure(djiError: DJIError?) {
                settings.shootPhotoModeInited = true
                Timber.e(
                    "An error is raised on receiving shootPhotoMode: %s",
                    djiError?.getDescription()
                        )
                onSettingsComponentInitialised()
            }
        })
    }

    private fun onSettingsComponentInitialised() {
        val last = isInitialised
        isInitialised = calculateIsInitialised()
        if (isInitialised && lenses?.values != null) {
            for (l in lenses.values) {
                l?.removeOnInitialisedListener(lensInitialisationListener)
                if (djiCamera?.isMultiLensCameraSupported() == true) {
                    val lensType = DjiCameraValueMapping.lensTypeFromSource(settings.activeSource)
                    if (lensType != null && l?.getType() == lensType) currentLensId = l.getId()
                }
            }
            if (BuildConfig.DEBUG && last) {
                throw AssertionError("Assertion failed. The initialisation happens two times")
            }
            onIsInitialisedChanged()
        }
    }

    override fun isSingleLens(): Boolean {
        return lenses?.size == 1
    }

    override fun getLenses(): MutableSet<Lens?> {
        return HashSet(lenses?.values)
    }

    override suspend fun setActiveLens(lens : Lens) {
        currentLensId = lens.id
        val sourceFromLensType = DjiCameraValueMapping.sourceFromLensType(lens.type)
        sourceFromLensType?.let {
            if (djiCamera != null) {
                suspendCoroutineCompletion(djiCamera::setCameraVideoStreamSource, it)
                synchronized(activeLensChangeListeners) {
                    for (listener in activeLensChangeListeners) {
                        listener?.run(lens)
                    }
                }
            }
        }
    }

    override fun getActiveLens(): Lens {
        return lenses?.get(currentLensId)!!
    }
    
    override fun getDjiCamera(): dji.sdk.camera.Camera? = djiCamera
    override fun getCameraMode(): CameraMode? {
        return DjiCameraValueMapping.cameraMode(settings.cameraMode)
    }

    override suspend fun setCameraMode(cameraMode: CameraMode) {
        DjiCameraValueMapping.sdkCameraMode(cameraMode)?.let {
            suspendCoroutineCompletion(djiCamera!!::setMode, it)
            
        }
    }

    override fun addCameraModeChangeListener(listener: Camera.ValueChangeListener<CameraMode?>) {
        if (cameraModeChangeListeners != null) {
            synchronized(cameraModeChangeListeners) { cameraModeChangeListeners.add(listener) }
        }
    }

    override fun removeCameraModeChangeListener(listener: Camera.ValueChangeListener<CameraMode?>) {
        if (cameraModeChangeListeners != null) {
            synchronized(cameraModeChangeListeners) { cameraModeChangeListeners.remove(listener) }
        }
    }

    override fun getSupportedCameraModes(): MutableList<CameraMode?> {
        val modes: MutableList<CameraMode?> = ArrayList()
        val sdkModes = djiCamera?.capabilities?.modeRange() ?: return modes
        for (sdkMode in sdkModes) {
            modes.add(DjiCameraValueMapping.cameraMode(sdkMode))
        }
        return modes
    }
    
    override fun getPhotoAEBCount(): PhotoAEBCount? {
        return DjiCameraValueMapping.photoAEBCount(settings.photoAEBCount)
    }

    override suspend fun setPhotoAEBCount(photoAEBCount: PhotoAEBCount) {
        DjiCameraValueMapping.sdkPhotoAEBCount(photoAEBCount)?.let {
            suspendCoroutineCompletion(djiCamera!!::setPhotoAEBCount, it)
            
        }
    }

    override fun addPhotoAEBCountChangeListener(photoAEBCountChangeListener: Camera.ValueChangeListener<PhotoAEBCount?>) {
        if (photoAebCountListeners != null) {
            synchronized(photoAebCountListeners) {
                photoAebCountListeners.add(
                    photoAEBCountChangeListener
                                          )
            }
        }
    }

    override fun removePhotoAEBCountChangeListener(photoAEBCountChangeListener: Camera.ValueChangeListener<PhotoAEBCount?>) {
        if (photoAebCountListeners != null) {
            synchronized(photoAebCountListeners) {
                photoAebCountListeners.remove(
                    photoAEBCountChangeListener
                                             )
            }
        }
    }

    override fun getPhotoBurstCount(): PhotoBurstCount? {
        return DjiCameraValueMapping.photoBurstCount(settings.photoBurstCount)
    }

    override suspend fun setPhotoBurstCount(photoBurstCount: PhotoBurstCount) {
        DjiCameraValueMapping.sdkPhotoBurstCount(photoBurstCount)?.let {
            if (djiCamera != null) {
                suspendCoroutineCompletion(djiCamera::setPhotoBurstCount, it)
            }
    
        }
    }

    override fun addPhotoBurstCountChangeListener(listener: Camera.ValueChangeListener<PhotoBurstCount?>) {
        if (photoBurstChangeListeners != null) {
            synchronized(photoBurstChangeListeners) { photoBurstChangeListeners.add(listener) }
        }
    }

    override fun removePhotoBurstCountChangeListener(listener: Camera.ValueChangeListener<PhotoBurstCount?>) {
        if (photoBurstChangeListeners != null) {
            synchronized(photoBurstChangeListeners) { photoBurstChangeListeners.remove(listener) }
        }
    }

    override fun getPhotoTimeIntervalSettings(): PhotoTimeIntervalSettings? {
        return DjiCameraValueMapping.photoTimeIntervalSettings(settings.photoTimeIntervalSettings)
    }

    override suspend fun setPhotoTimeIntervalSettings(value: PhotoTimeIntervalSettings) {
        DjiCameraValueMapping.sdkPhotoTimeIntervalSettings(value)?.let {
            if (djiCamera != null) {
                suspendCoroutineCompletion(djiCamera::setPhotoTimeIntervalSettings, it)
            }
        }
    }

    override fun addPhotoTimeIntervalChangeListener(listener: Camera.ValueChangeListener<PhotoTimeIntervalSettings?>) {
        if (photoTimeIntervalSettingsChangeListeners != null) {
            synchronized(photoTimeIntervalSettingsChangeListeners) {
                photoTimeIntervalSettingsChangeListeners.add(
                    listener
                                                            )
            }
        }
    }

    override fun removePhotoTimeIntervalChangeListener(listener: Camera.ValueChangeListener<PhotoTimeIntervalSettings?>) {
        if (photoTimeIntervalSettingsChangeListeners != null) {
            synchronized(photoTimeIntervalSettingsChangeListeners) {
                photoTimeIntervalSettingsChangeListeners.remove(
                    listener
                                                               )
            }
        }
    }

    override fun getShootPhotoMode(): ShootPhotoMode? {
        return DjiCameraValueMapping.shootPhotoMode(settings.shootPhotoMode)
    }

    override suspend fun setShootPhotoMode(mode: ShootPhotoMode) {
        DjiCameraValueMapping.sdkShootPhotoMode(mode)?.let {
            suspendCoroutineCompletion(djiCamera!!::setShootPhotoMode, it)
        } ?: throw NullPointerException("Shoot Photo Mode not found for $mode")
    }
    
    override fun addShootPhotoModeChangeListener(listener: Camera.ValueChangeListener<ShootPhotoMode?>) {
        if (photoModeChangeListeners != null) {
            synchronized(photoModeChangeListeners) { photoModeChangeListeners.add(listener) }
        }
    }

    override fun removeShootPhotoModeChangeListener(listener: Camera.ValueChangeListener<ShootPhotoMode?>) {
        if (photoModeChangeListeners != null) {
            synchronized(photoModeChangeListeners) { photoModeChangeListeners.remove(listener) }
        }
    }

    override fun getSupportedShootPhotoModes(): MutableList<ShootPhotoMode?> {
        if (settings.shootPhotoMode == null) // There is a strange DJI SDK behavior when there are some supported photo modes exists but selected photo mode is unsupported
            return ArrayList()
        val km = KeyManager.getInstance()
        val djiKey = CameraKey.create(CameraKey.SHOOT_PHOTO_MODE_RANGE)
        if (djiKey == null) {
            Timber.e("Unable get shoot photo mode. Can't create CameraKey SHOOT_PHOTO_MODE_RANGE.")
            return ArrayList()
        }
        val sdkModes = km.getValue(djiKey) as Array<SettingsDefinitions.ShootPhotoMode?>?
            ?: return ArrayList()
        val modes = ArrayList<ShootPhotoMode?>()
        for (mode in sdkModes) {
            modes.add(DjiCameraValueMapping.shootPhotoMode(mode))
        }
        return modes
    }

    override fun getDisplayName(): String {
        var name = djiCamera?.getDisplayName()
        val NO_NAME_DRONE = "NO_NAME"
        if (name == null) name = NO_NAME_DRONE
        return name
    }

    private fun calculateIsInitialised(): Boolean {
        var inited = settings.cameraModeInited
        inited = inited and settings.activeSourceInited
        inited = inited and settings.photoAEBCountInited
        inited = inited and settings.photoBurstCountInited
        inited = inited and settings.shootPhotoModeInited
        inited = inited and settings.photoTimeIntervalSettingsInited
        inited = inited and settings.isVideoRecordingInited
        inited = inited and settings.isShootingSinglePhotoInited
        inited = inited and settings.isShootingBurstPhotoInited
        inited = inited and settings.isShootingIntervalPhotoInited
        inited = inited and settings.isStoringPhotoInited
        inited = inited and settings.currentVideoRecordingTimeInSecondsInited
        if (lenses?.values != null) {
            for (l in lenses.values) {
                inited = inited and (l?.isInitialised() == true)
            }
        }
        return inited
    }

    override fun isInitialised(): Boolean {
        return isInitialised
    }

    override fun addOnInitialisedListener(listener: Camera.Callback) {
        if (isInitialised) listener.run(null) else if (initialisationError != null) listener.run(
            initialisationError
                                                                                                ) else {
            synchronized(initialisedListeners) { initialisedListeners.add(listener) }
        }
    }

    override fun removeOnInitialisedListener(listener: Camera.Callback) {
        synchronized(initialisedListeners) { initialisedListeners.remove(listener) }
    }

    override fun addActiveLensChangeListener(listener: Camera.ValueChangeListener<Lens?>) {
        synchronized(activeLensChangeListeners) { activeLensChangeListeners.add(listener) }
    }

    override fun removeActiveLensChangeListener(listener: Camera.ValueChangeListener<Lens?>) {
        synchronized(activeLensChangeListeners) { activeLensChangeListeners.remove(listener) }
    }

    override fun startShootPhoto(onStart: Camera.Callback?) {
        djiCamera?.startShootPhoto { djiError: DJIError? ->
            if (djiError != null) {
                onStart?.run(Exception(djiError.description))
            } else {
                onStart?.run(null)
            }
        }
    }

    override fun stopShootPhoto(onStop: Camera.Callback?) {
        djiCamera?.stopShootPhoto { djiError: DJIError? ->
            if (djiError != null) {
                onStop?.run(Exception(djiError.description))
            } else {
                onStop?.run(null)
            }
        }
    }

    override fun startRecordVideo(onStart: Camera.Callback?) {
        djiCamera?.startRecordVideo { djiError: DJIError? ->
            if (djiError != null) {
                onStart?.run(Exception(djiError.description))
            } else {
                onStart?.run(null)
            }
        }
    }

    override fun stopRecordVideo(onStop: Camera.Callback?) {
        djiCamera?.stopRecordVideo { djiError: DJIError? ->
            if (djiError != null) {
                onStop?.run(Exception(djiError.description))
            } else {
                onStop?.run(null)
            }
        }
    }

    override fun isVideoRecording(): Boolean? {
        return settings.isVideoRecording
    }

    private fun setIsVideoRecording(value: Boolean) {
        if (settings.isVideoRecording != null && value == settings.isVideoRecording) {
            return
        }
        settings.isVideoRecording = value
        if (isVideoRecordingChangeListeners != null) {
            synchronized(isVideoRecordingChangeListeners) {
                for (listener in isVideoRecordingChangeListeners) {
                    listener?.run(value)
                }
            }
        }
    }

    override fun addVideoRecordingChangeListener(listener: Camera.ValueChangeListener<Boolean?>?) {
        if (isVideoRecordingChangeListeners != null) {
            synchronized(isVideoRecordingChangeListeners) { isVideoRecordingChangeListeners.add(listener) }
        }
    }

    override fun removeVideoRecordingChangeListener(listener: Camera.ValueChangeListener<Boolean?>?) {
        if (isVideoRecordingChangeListeners != null) {
            synchronized(isVideoRecordingChangeListeners) {
                if (isVideoRecordingChangeListeners.contains(
                        listener
                                                            )
                ) isVideoRecordingChangeListeners.add(listener)
            }
        }
    }

    override fun isShootingSinglePhoto(): Boolean? {
        return settings.isShootingSinglePhoto
    }

    private fun setIsShootingSinglePhoto(value: Boolean) {
        if (settings.isShootingSinglePhoto != null && value == settings.isShootingSinglePhoto) {
            return
        }
        settings.isShootingSinglePhoto = value
        if (isShootingSinglePhotoChangeListener != null) {
            synchronized(isShootingSinglePhotoChangeListener) {
                for (listener in isShootingSinglePhotoChangeListener) {
                    listener?.run(value)
                }
            }
        }
    }

    override fun addShootingSinglePhotoChangeListener(listener: Camera.ValueChangeListener<Boolean?>?) {
        if (isShootingSinglePhotoChangeListener != null) {
            synchronized(isShootingSinglePhotoChangeListener) {
                isShootingSinglePhotoChangeListener.add(
                    listener
                                                       )
            }
        }
    }

    override fun removeShootingSinglePhotoChangeListener(listener: Camera.ValueChangeListener<Boolean?>?) {
        if (isShootingSinglePhotoChangeListener != null) {
            synchronized(isShootingSinglePhotoChangeListener) {
                if (isShootingSinglePhotoChangeListener.contains(
                        listener
                                                                )
                ) isShootingSinglePhotoChangeListener.add(listener)
            }
        }
    }

    override fun isShootingBurstPhoto(): Boolean? {
        return settings.isShootingBurstPhoto
    }

    private fun setIsShootingBurstPhoto(value: Boolean) {
        if (settings.isShootingBurstPhoto != null && value == settings.isShootingBurstPhoto) {
            return
        }
        settings.isShootingBurstPhoto = value
        if (isShootingBurstPhotoChangeListeners != null) {
            synchronized(isShootingBurstPhotoChangeListeners) {
                for (listener in isShootingBurstPhotoChangeListeners) {
                    listener?.run(value)
                }
            }
        }
    }

    override fun addShootingBurstPhotoChangeListener(listener: Camera.ValueChangeListener<Boolean?>?) {
        if (isShootingBurstPhotoChangeListeners != null) {
            synchronized(isShootingBurstPhotoChangeListeners) {
                isShootingBurstPhotoChangeListeners.add(
                    listener
                                                       )
            }
        }
    }

    override fun removeShootingBurstPhotoChangeListener(listener: Camera.ValueChangeListener<Boolean?>?) {
        if (isShootingBurstPhotoChangeListeners != null) {
            synchronized(isShootingBurstPhotoChangeListeners) {
                if (isShootingBurstPhotoChangeListeners.contains(
                        listener
                                                                )
                ) isShootingBurstPhotoChangeListeners.add(listener)
            }
        }
    }

    override fun isShootingIntervalPhoto(): Boolean? {
        return settings.isShootingIntervalPhoto
    }

    private fun setIsShootingIntervalPhoto(value: Boolean) {
        if (settings.isShootingIntervalPhoto != null && value == settings.isShootingIntervalPhoto) {
            return
        }
        settings.isShootingIntervalPhoto = value
        if (isShootingIntervalPhotoChangeListeners != null) {
            synchronized(isShootingIntervalPhotoChangeListeners) {
                for (listener in isShootingIntervalPhotoChangeListeners) {
                    listener?.run(value)
                }
            }
        }
    }

    override fun addShootingIntervalPhotoChangeListener(listener: Camera.ValueChangeListener<Boolean?>?) {
        if (isShootingIntervalPhotoChangeListeners != null) {
            synchronized(isShootingIntervalPhotoChangeListeners) {
                isShootingIntervalPhotoChangeListeners.add(
                    listener
                                                          )
            }
        }
    }

    override fun removeShootingIntervalPhotoChangeListener(listener: Camera.ValueChangeListener<Boolean?>?) {
        if (isShootingIntervalPhotoChangeListeners != null) {
            synchronized(isShootingIntervalPhotoChangeListeners) {
                if (isShootingIntervalPhotoChangeListeners.contains(
                        listener
                                                                   )
                ) isShootingIntervalPhotoChangeListeners.add(listener)
            }
        }
    }

    override fun isStoringPhoto(): Boolean? {
        return settings.isStoringPhoto
    }

    private fun setIsStoringPhoto(value: Boolean) {
        if (settings.isStoringPhoto != null && value == settings.isStoringPhoto) {
            return
        }
        settings.isStoringPhoto = value
        if (isStoringPhotoChangeListeners != null) {
            synchronized(isStoringPhotoChangeListeners) {
                for (listener in isStoringPhotoChangeListeners) {
                    listener?.run(value)
                }
            }
        }
    }

    override fun addStoringPhotoChangeListener(listener: Camera.ValueChangeListener<Boolean?>?) {
        if (isStoringPhotoChangeListeners != null) {
            synchronized(isStoringPhotoChangeListeners) { isStoringPhotoChangeListeners.add(listener) }
        }
    }

    override fun removeStoringPhotoChangeListener(listener: Camera.ValueChangeListener<Boolean?>?) {
        if (isStoringPhotoChangeListeners != null) {
            synchronized(isStoringPhotoChangeListeners) {
                if (isStoringPhotoChangeListeners.contains(
                        listener
                                                          )
                ) isStoringPhotoChangeListeners.add(listener)
            }
        }
    }

    override fun getCurrentVideoRecordingTimeInSeconds(): Int? {
        return settings.currentVideoRecordingTimeInSeconds
    }

    private fun setCurrentVideoRecordingTimeInSeconds(value: Int) {
        if (settings.currentVideoRecordingTimeInSeconds != null && value == settings.currentVideoRecordingTimeInSeconds) {
            return
        }
        settings.currentVideoRecordingTimeInSeconds = value
        if (currentVideoRecordingTimeInSecondsChangeListeners != null) {
            synchronized(currentVideoRecordingTimeInSecondsChangeListeners) {
                for (listener in currentVideoRecordingTimeInSecondsChangeListeners) {
                    listener?.run(value)
                }
            }
        }
    }

    override fun addCurrentVideoRecordingTimeInSecondsChangeListener(listener: Camera.ValueChangeListener<Int?>?) {
        if (currentVideoRecordingTimeInSecondsChangeListeners != null) {
            synchronized(currentVideoRecordingTimeInSecondsChangeListeners) {
                currentVideoRecordingTimeInSecondsChangeListeners.add(
                    listener
                                                                     )
            }
        }
    }

    override fun removeCurrentVideoRecordingTimeInSecondsChangeListener(listener: Camera.ValueChangeListener<Int?>?) {
        if (currentVideoRecordingTimeInSecondsChangeListeners != null) {
            synchronized(currentVideoRecordingTimeInSecondsChangeListeners) {
                if (currentVideoRecordingTimeInSecondsChangeListeners.contains(
                        listener
                                                                              )
                ) currentVideoRecordingTimeInSecondsChangeListeners.add(listener)
            }
        }
    }

    override fun getMediaManager(): MediaManager? {
        return null
    }

    override fun setXmpInformation(information: String?, onSet: Camera.Callback?) {
        if (information != null) {
            djiCamera?.setMediaFileCustomInformation(information) { djiError: DJIError? ->
                if (djiError == null)
                    onSet?.run(null)
                else
                    onSet?.run(Exception(djiError.description))
            }
        }
    }

    private fun onIsInitialisedChanged() {
        synchronized(initialisedListeners) {
            for (listener in initialisedListeners) {
                listener?.run(null)
            }
            initialisedListeners.clear()
        }
    }

    private fun initialisationError(error: Exception?) {
        initialisationError = error
        synchronized(initialisedListeners) {
            for (listener in initialisedListeners) {
                listener?.run(error)
            }
        }
    }

    private fun setCameraModeInternal(cameraMode: SettingsDefinitions.CameraMode?) {
        if (settings.cameraMode == cameraMode) return
        settings.cameraMode = cameraMode
        if (cameraModeChangeListeners != null) {
            synchronized(cameraModeChangeListeners) {
                for (listener in cameraModeChangeListeners) {
                    listener?.run(DjiCameraValueMapping.cameraMode(cameraMode))
                }
            }
        }
    }

    private fun setAebCountInternal(photoAEBCount: SettingsDefinitions.PhotoAEBCount?) {
        if (settings.photoAEBCount == photoAEBCount) return
        settings.photoAEBCount = photoAEBCount
        if (photoAebCountListeners != null) {
            synchronized(photoAebCountListeners) {
                for (listener in photoAebCountListeners) {
                    listener?.run(DjiCameraValueMapping.photoAEBCount(photoAEBCount))
                }
            }
        }
    }

    private fun setPhotoBurstCountInternal(photoBurstCount: SettingsDefinitions.PhotoBurstCount?) {
        if (settings.photoBurstCount == photoBurstCount) return
        settings.photoBurstCount = photoBurstCount
        if (photoBurstChangeListeners != null) {
            synchronized(photoBurstChangeListeners) {
                for (listener in photoBurstChangeListeners) {
                    listener?.run(DjiCameraValueMapping.photoBurstCount(photoBurstCount))
                }
            }
        }
    }

    private fun setPhotoTimeIntervalSettingsInternal(sdkValue: SettingsDefinitions.PhotoTimeIntervalSettings?) {
        if (settings.photoTimeIntervalSettings === sdkValue) return
        settings.photoTimeIntervalSettings = sdkValue
        if (photoTimeIntervalSettingsChangeListeners != null) {
            synchronized(photoTimeIntervalSettingsChangeListeners) {
                for (listener in photoTimeIntervalSettingsChangeListeners) {
                    listener?.run(DjiCameraValueMapping.photoTimeIntervalSettings(sdkValue))
                }
            }
        }
    }

    private fun setPhotoModeInternal(sdkValue: SettingsDefinitions.ShootPhotoMode?) {
        if (settings.shootPhotoMode == sdkValue) return
        settings.shootPhotoMode = sdkValue
        if (photoModeChangeListeners != null) {
            synchronized(photoModeChangeListeners) {
                for (listener in photoModeChangeListeners) {
                    listener?.run(DjiCameraValueMapping.shootPhotoMode(sdkValue))
                }
            }
        }
    }

    private object DjiCameraValueMapping {
        fun sdkCameraMode(mode: CameraMode?): SettingsDefinitions.CameraMode? {
            return if (mode == null) null else when (mode) {
                CameraMode.SHOOT_PHOTO -> SettingsDefinitions.CameraMode.SHOOT_PHOTO
                CameraMode.RECORD_VIDEO -> SettingsDefinitions.CameraMode.RECORD_VIDEO
                CameraMode.PLAYBACK -> SettingsDefinitions.CameraMode.PLAYBACK
                CameraMode.MEDIA_DOWNLOAD -> SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD
                CameraMode.BROADCAST -> SettingsDefinitions.CameraMode.BROADCAST
                CameraMode.UNKNOWN -> SettingsDefinitions.CameraMode.UNKNOWN
                else -> SettingsDefinitions.CameraMode.UNKNOWN
            }
        }

        fun cameraMode(mode: SettingsDefinitions.CameraMode?): CameraMode? {
            return if (mode == null) null else when (mode) {
                SettingsDefinitions.CameraMode.SHOOT_PHOTO -> CameraMode.SHOOT_PHOTO
                SettingsDefinitions.CameraMode.RECORD_VIDEO -> CameraMode.RECORD_VIDEO
                SettingsDefinitions.CameraMode.PLAYBACK -> CameraMode.PLAYBACK
                SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD -> CameraMode.MEDIA_DOWNLOAD
                SettingsDefinitions.CameraMode.BROADCAST -> CameraMode.BROADCAST
                SettingsDefinitions.CameraMode.UNKNOWN -> CameraMode.UNKNOWN
                else -> CameraMode.UNKNOWN
            }
        }

        fun sdkPhotoAEBCount(value: PhotoAEBCount?): SettingsDefinitions.PhotoAEBCount? {
            return if (value == null) null else when (value) {
                PhotoAEBCount.AEB_COUNT_3 -> SettingsDefinitions.PhotoAEBCount.AEB_COUNT_3
                PhotoAEBCount.AEB_COUNT_5 -> SettingsDefinitions.PhotoAEBCount.AEB_COUNT_5
                PhotoAEBCount.AEB_COUNT_7 -> SettingsDefinitions.PhotoAEBCount.AEB_COUNT_7
                PhotoAEBCount.UNKNOWN -> SettingsDefinitions.PhotoAEBCount.UNKNOWN
                else -> SettingsDefinitions.PhotoAEBCount.UNKNOWN
            }
        }

        fun photoAEBCount(value: SettingsDefinitions.PhotoAEBCount?): PhotoAEBCount? {
            return if (value == null) null else when (value) {
                SettingsDefinitions.PhotoAEBCount.AEB_COUNT_3 -> PhotoAEBCount.AEB_COUNT_3
                SettingsDefinitions.PhotoAEBCount.AEB_COUNT_5 -> PhotoAEBCount.AEB_COUNT_5
                SettingsDefinitions.PhotoAEBCount.AEB_COUNT_7 -> PhotoAEBCount.AEB_COUNT_7
                SettingsDefinitions.PhotoAEBCount.UNKNOWN -> PhotoAEBCount.UNKNOWN
                else -> PhotoAEBCount.UNKNOWN
            }
        }

        fun sdkPhotoBurstCount(value: PhotoBurstCount?): SettingsDefinitions.PhotoBurstCount? {
            return if (value == null) null else when (value) {
                PhotoBurstCount.BURST_COUNT_2 -> SettingsDefinitions.PhotoBurstCount.BURST_COUNT_2
                PhotoBurstCount.BURST_COUNT_3 -> SettingsDefinitions.PhotoBurstCount.BURST_COUNT_3
                PhotoBurstCount.BURST_COUNT_5 -> SettingsDefinitions.PhotoBurstCount.BURST_COUNT_5
                PhotoBurstCount.BURST_COUNT_7 -> SettingsDefinitions.PhotoBurstCount.BURST_COUNT_7
                PhotoBurstCount.BURST_COUNT_10 -> SettingsDefinitions.PhotoBurstCount.BURST_COUNT_10
                PhotoBurstCount.BURST_COUNT_14 -> SettingsDefinitions.PhotoBurstCount.BURST_COUNT_14
                PhotoBurstCount.CONTINUOUS -> SettingsDefinitions.PhotoBurstCount.CONTINUOUS
                PhotoBurstCount.UNKNOWN -> SettingsDefinitions.PhotoBurstCount.UNKNOWN
                else -> SettingsDefinitions.PhotoBurstCount.UNKNOWN
            }
        }

        fun photoBurstCount(value: SettingsDefinitions.PhotoBurstCount?): PhotoBurstCount? {
            return if (value == null) null else when (value) {
                SettingsDefinitions.PhotoBurstCount.BURST_COUNT_2 -> PhotoBurstCount.BURST_COUNT_2
                SettingsDefinitions.PhotoBurstCount.BURST_COUNT_3 -> PhotoBurstCount.BURST_COUNT_3
                SettingsDefinitions.PhotoBurstCount.BURST_COUNT_5 -> PhotoBurstCount.BURST_COUNT_5
                SettingsDefinitions.PhotoBurstCount.BURST_COUNT_7 -> PhotoBurstCount.BURST_COUNT_7
                SettingsDefinitions.PhotoBurstCount.BURST_COUNT_10 -> PhotoBurstCount.BURST_COUNT_10
                SettingsDefinitions.PhotoBurstCount.BURST_COUNT_14 -> PhotoBurstCount.BURST_COUNT_14
                SettingsDefinitions.PhotoBurstCount.CONTINUOUS -> PhotoBurstCount.CONTINUOUS
                SettingsDefinitions.PhotoBurstCount.UNKNOWN -> PhotoBurstCount.UNKNOWN
                else -> PhotoBurstCount.UNKNOWN
            }
        }

        fun sdkPhotoTimeIntervalSettings(value: PhotoTimeIntervalSettings?): SettingsDefinitions.PhotoTimeIntervalSettings? {
            return if (value == null) null else SettingsDefinitions.PhotoTimeIntervalSettings(
                value.captureCount,
                value.timeIntervalInSeconds
                                                                                             )
        }

        fun photoTimeIntervalSettings(value: SettingsDefinitions.PhotoTimeIntervalSettings?): PhotoTimeIntervalSettings? {
            return if (value == null) null else PhotoTimeIntervalSettings(
                value.captureCount,
                value.timeIntervalInSeconds
                                                                         )
        }

        fun sdkShootPhotoMode(value: ShootPhotoMode?): SettingsDefinitions.ShootPhotoMode? {
            return if (value == null) null else when (value) {
                ShootPhotoMode.SINGLE -> SettingsDefinitions.ShootPhotoMode.SINGLE
                ShootPhotoMode.HDR -> SettingsDefinitions.ShootPhotoMode.HDR
                ShootPhotoMode.BURST -> SettingsDefinitions.ShootPhotoMode.BURST
                ShootPhotoMode.AEB -> SettingsDefinitions.ShootPhotoMode.AEB
                ShootPhotoMode.INTERVAL -> SettingsDefinitions.ShootPhotoMode.INTERVAL
                ShootPhotoMode.TIME_LAPSE -> SettingsDefinitions.ShootPhotoMode.TIME_LAPSE
                ShootPhotoMode.PANORAMA -> SettingsDefinitions.ShootPhotoMode.PANORAMA
                ShootPhotoMode.RAW_BURST -> SettingsDefinitions.ShootPhotoMode.RAW_BURST
                ShootPhotoMode.SHALLOW_FOCUS -> SettingsDefinitions.ShootPhotoMode.SHALLOW_FOCUS
                ShootPhotoMode.EHDR -> SettingsDefinitions.ShootPhotoMode.EHDR
                ShootPhotoMode.HYPER_LIGHT -> SettingsDefinitions.ShootPhotoMode.HYPER_LIGHT
                ShootPhotoMode.HYPER_LAPSE -> SettingsDefinitions.ShootPhotoMode.HYPER_LAPSE
                ShootPhotoMode.SUPER_RESOLUTION -> SettingsDefinitions.ShootPhotoMode.SUPER_RESOLUTION
                ShootPhotoMode.UNKNOWN -> SettingsDefinitions.ShootPhotoMode.UNKNOWN
                else -> SettingsDefinitions.ShootPhotoMode.UNKNOWN
            }
        }

        fun shootPhotoMode(value: SettingsDefinitions.ShootPhotoMode?): ShootPhotoMode? {
            return if (value == null) null else when (value) {
                SettingsDefinitions.ShootPhotoMode.SINGLE -> ShootPhotoMode.SINGLE
                SettingsDefinitions.ShootPhotoMode.HDR -> ShootPhotoMode.HDR
                SettingsDefinitions.ShootPhotoMode.BURST -> ShootPhotoMode.BURST
                SettingsDefinitions.ShootPhotoMode.AEB -> ShootPhotoMode.AEB
                SettingsDefinitions.ShootPhotoMode.INTERVAL -> ShootPhotoMode.INTERVAL
                SettingsDefinitions.ShootPhotoMode.TIME_LAPSE -> ShootPhotoMode.TIME_LAPSE
                SettingsDefinitions.ShootPhotoMode.PANORAMA -> ShootPhotoMode.PANORAMA
                SettingsDefinitions.ShootPhotoMode.RAW_BURST -> ShootPhotoMode.RAW_BURST
                SettingsDefinitions.ShootPhotoMode.SHALLOW_FOCUS -> ShootPhotoMode.SHALLOW_FOCUS
                SettingsDefinitions.ShootPhotoMode.EHDR -> ShootPhotoMode.EHDR
                SettingsDefinitions.ShootPhotoMode.HYPER_LIGHT -> ShootPhotoMode.HYPER_LIGHT
                SettingsDefinitions.ShootPhotoMode.HYPER_LAPSE -> ShootPhotoMode.HYPER_LAPSE
                SettingsDefinitions.ShootPhotoMode.SUPER_RESOLUTION -> ShootPhotoMode.SUPER_RESOLUTION
                SettingsDefinitions.ShootPhotoMode.UNKNOWN -> ShootPhotoMode.UNKNOWN
                else -> ShootPhotoMode.UNKNOWN
            }
        }

        fun lensTypeFromSource(source: CameraVideoStreamSource?): LensType? {
            return if (source == null) null else when (source) {
                CameraVideoStreamSource.DEFAULT, CameraVideoStreamSource.WIDE -> LensType.WIDE
                CameraVideoStreamSource.ZOOM -> LensType.ZOOM
                CameraVideoStreamSource.INFRARED_THERMAL -> LensType.THERMAL
                CameraVideoStreamSource.UNKNOWN -> LensType.UNKNOWN
                else -> LensType.UNKNOWN
            }
        }

        fun sourceFromLensType(type: LensType?): CameraVideoStreamSource? {
            return if (type == null) null else when (type) {
                LensType.THERMAL -> CameraVideoStreamSource.INFRARED_THERMAL
                LensType.WIDE -> CameraVideoStreamSource.WIDE
                LensType.ZOOM -> CameraVideoStreamSource.ZOOM
                LensType.UNKNOWN -> CameraVideoStreamSource.UNKNOWN
                else -> CameraVideoStreamSource.UNKNOWN
            }
        }
    }

    init {
        requireNotNull(camera) { "Camera can not be null" }
        requireNotNull(context) { "Context is not bound to app" }
        djiCamera = camera
        lenses = HashMap()
        if (camera.isMultiLensCameraSupported) {
            val lensesList = camera.lenses
            if (lensesList != null) {
                for (lens in lensesList) {
                    val l: Lens = DjiLens(lens, djiCamera, context)
                    lenses[l.getId()] = l
                    l.addOnInitialisedListener(lensInitialisationListener)
                }
            } else {
                Timber.w("Lenses list is null")
            }
        } else {
            val singleLens: Lens = DjiLens(djiCamera, context)
            lenses[singleLens.getId()] = singleLens
            singleLens.addOnInitialisedListener(lensInitialisationListener)
        }
        initSettings()
    }
    
    override suspend fun getThermalIsothermEnabled() : Boolean = DjiSuspend(djiCamera!!::getThermalIsothermEnabled)
    override suspend fun getThermalIsothermLowerValue() : Int = DjiSuspend(djiCamera!!::getThermalIsothermLowerValue)
    override suspend fun getThermalIsothermUpperValue() : Int = DjiSuspend(djiCamera!!::getThermalIsothermUpperValue)
    override suspend fun getFocusAssistantSettings() : Pair<Boolean, Boolean> = suspendCoroutineTwo(djiCamera!!::getFocusAssistantSettings)
    override suspend fun getFocusMode() : SettingsDefinitions.FocusMode? = DjiSuspend(djiCamera!!::getFocusMode)
    override suspend fun getFocusRingValue() : Int = DjiSuspend(djiCamera!!::getFocusRingValue)
    override suspend fun getFocusTarget() : PointF? = DjiSuspend(djiCamera!!::getFocusTarget)
    override suspend fun isFlatCameraModeSupported() : Boolean? = djiCamera?.isFlatCameraModeSupported
    override suspend fun isThermalCamera() : Boolean? = djiCamera?.isThermalCamera
    override suspend fun setFlatMode(flatCameraMode: FlatCameraMode) {
        val sdkFlatCameraMode = flatCameraMode.toDji()
        if (djiCamera != null) {
            suspendCoroutineCompletion(djiCamera::setFlatMode, sdkFlatCameraMode)
        }
    }
    override suspend fun setThermalDigitalZoomFactor(thermalDigitalZoomFactor: ThermalDigitalZoomFactor) {
        val sdkThermalDigitalZoomFactor = thermalDigitalZoomFactor.toDji()
        if (djiCamera != null) {
            suspendCoroutineCompletion(djiCamera::setThermalDigitalZoomFactor, sdkThermalDigitalZoomFactor)
        }
    }
}