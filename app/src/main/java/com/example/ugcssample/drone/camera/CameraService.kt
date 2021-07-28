package com.example.ugcssample.drone.camera

interface CameraService {
    interface ValueChangeListener<T> {
        open fun onChange(camera: T?)
    }

    open fun getCameras(): MutableSet<Camera?>?
    open fun getMainCamera(): Camera?
    open fun addMainCameraChangeListeners(listener: ValueChangeListener<Camera?>?)
    open fun removeMainCameraChangeListeners(listener: ValueChangeListener<Camera?>?)
    open fun updateCamerasForCurrentProduct()
}