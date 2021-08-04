package com.example.ugcssample.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.example.ugcssample.drone.DroneBridge;
import com.example.ugcssample.drone.DroneBridgeImpl;

import java.io.File;

import timber.log.Timber;
public class DjiAppMainServiceImpl extends Service implements DjiAppMainService {

    private DjiAppMainServiceBinder binder;
    private DroneBridge droneBridge;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Timber.d("DjiAppMainServiceImpl onBind");
        return binder;
    }

    @Override
    public void onCreate() {
        Timber.d("DjiAppMainServiceImpl onCreate");
        super.onCreate();
        Context context = getApplicationContext();
        binder = new DjiAppMainServiceBinder(this);
        droneBridge = new DroneBridgeImpl(context);
    }

    @Override
    public void onDestroy() {
        Timber.d("DjiAppMainServiceImpl onDestroy");
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        final String action = intent.getAction();
        Timber.d("DjiAppMainServiceImpl onUnbind, action = %s", action);
        return super.onUnbind(intent);
    }

    @Override
    public void init() {
        droneBridge.submitSdkInit();
    }

    @Override
    public void startSimulator() {
        droneBridge.startModelSimulator();
    }
    @Override
    public void uploadMission() {
        droneBridge.uploadDemoMission();
    }
    @Override
    public void startMission() {
        droneBridge.startMission();
    }

    @Override
    public void land(boolean useKeyInterface) {
        droneBridge.land(useKeyInterface);
    }
    @Override
    public void uploadMission(File nativeRoute) {
        droneBridge.uploadMission(nativeRoute);
    }
    
    @Override
    public void takeOff() {
        droneBridge.takeOff();
    }

    private DroneBridge newDroneBridge(Context context) {
        return new DroneBridgeImpl(context);
    }
}
