package com.example.ugcssample.drone;

import com.example.ugcssample.utils.PermissionCheckResult;

import dji.sdk.products.Aircraft;

public interface DroneBridge {

    void submitLocationInit();

    void submitSdkInit();

    void onUsbAccessoryAttached();

    PermissionCheckResult getPermissionCheckResult();

    Aircraft getAircraftInstance();

    void startModelSimulator();

    void setMediaFileCustomInformation(String information);

    void onDestroy();

    void remoteController();

    void zoomCamera();

    void wideCamera();

    void remoteControllerBind();
}