package com.example.ugcssample;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.BluetoothProductConnector;
import dji.sdk.sdkmanager.DJISDKManager;
import timber.log.Timber;

/**
 * Main application
 */
public class UgcsSampleApplication extends Application {

    public static final String TAG = UgcsSampleApplication.class.getName();

    private static BaseProduct product;
    private static BluetoothProductConnector bluetoothConnector = null;
    private static Application app = null;

    /**
     * Gets instance of the specific product connected after the
     * API KEY is successfully validated. Please make sure the
     * API_KEY has been added in the Manifest
     */
    public static Application getInstance() {
        return UgcsSampleApplication.app;
    }


    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        MultiDex.install(this);
        com.secneo.sdk.Helper.install(this);
        app = this;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        // This will initialise Timber
        Timber.plant(new Timber.DebugTree());
    }
}