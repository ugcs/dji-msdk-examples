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

    void onDestroy();

    void takeOff();

    void land(boolean useKeyInterface);

    void uploadDemoMission();
    void startMission();
}