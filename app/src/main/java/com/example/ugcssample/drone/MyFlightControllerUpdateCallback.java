package com.example.ugcssample.drone;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.ugcssample.model.utils.AppUtils;

import java.util.Locale;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.flightcontroller.imu.IMUState;
import dji.keysdk.DJIKey;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.keysdk.ProductKey;
import timber.log.Timber;

public class MyFlightControllerUpdateCallback extends GenericUpdateCallback implements DjiSdkKeyGroup.Listener {

    public volatile Double latitude = null;
    public volatile Double longitude = null;
    public volatile Float altitude = null;
    private static final String[] KEYS = {
            // Position
            FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE,
            FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE,
            FlightControllerKey.ALTITUDE,
            FlightControllerKey.GPS_SIGNAL_LEVEL,
            FlightControllerKey.SATELLITE_COUNT
    };

    private static final Class<?>[] EXPECTED_TYPES = {
            // Position
            Double.class,
            Double.class,
            Float.class,
            GPSSignalLevel.class,
            Integer.class
    };

    private final DjiSdkKeyGroup flightControlKeyGroup;
    private ScheduledFuture<?> gpsUpdateFuture;
    private boolean firstTime = true;
    private ScheduledFuture<?> homeUpdateFuture = null;

    private FlightMode oldMode = FlightMode.UNKNOWN;
    private String oldModeString = null;

    public MyFlightControllerUpdateCallback(LocalBroadcastManager lbm) {
        super(lbm);
        flightControlKeyGroup = new DjiSdkKeyGroup(KEYS, EXPECTED_TYPES, this) {
            @Override
            public DJIKey create(String key) {
                if (ProductKey.FIRMWARE_PACKAGE_VERSION.equals(key)) {
                    return ProductKey.create(key);
                } else if (FlightControllerKey.IMU_STATE.equals(key)) {
                    return FlightControllerKey.create(key);
                } else {
                    // FlightControllerKey.AIRCRAFT_NAME, FlightControllerKey.FIRMWARE_VERSION
                    return FlightControllerKey.create(key);
                }
            }
        };
    }

    @Override
    public void setUpKeyListeners() {
        oldModeString = null;
        oldMode = FlightMode.UNKNOWN;
        firstTime = true;
        tearDownKeyListeners();

        KeyManager km = KeyManager.getInstance();
        if (km == null) {
            return;
        }

        Timber.i(DjiSdkKeyGroup.defaultLogMsg(KEYS));
        flightControlKeyGroup.setUpKeyListeners(km);
    }

    @Override
    public void tearDownKeyListeners() {
        oldModeString = null;
        oldMode = FlightMode.UNKNOWN;
        flightControlKeyGroup.tearDownKeyListeners();
    }

    @Override
    public void onValueChange(String key, @NonNull Object newValue) {

        if (FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE.equals(key)) {
            Double lat = (Double)newValue;
            if (!Double.isNaN(lat) && !lat.equals(latitude)) {
                latitude = lat;
            }
            Timber.i("onValueChange lat %s", lat);
        } else if (FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE.equals(key)) {
            Double lng = (Double)newValue;
            if (!Double.isNaN(lng) && !lng.equals(longitude)) {
                longitude = lng;
            }
            Timber.i("onValueChange lng %s", lng);
        } else if (FlightControllerKey.ALTITUDE.equals(key)) {
            Float alt = (float)newValue;
            if (!Float.isNaN(alt) && !alt.equals(altitude)) {
                altitude = alt;
            }
            Timber.i("onValueChange alt %s", alt);
        } else if (FlightControllerKey.GPS_SIGNAL_LEVEL.equals(key)) {

        } else if (FlightControllerKey.SATELLITE_COUNT.equals(key)) {

        } else {
            AppUtils.unhandledSwitch(key);
        }
    }

    @Override
    public void onFailure(String key, String errorDescription) {
        Timber.w(DjiSdkKeyGroup.defaultErrorMsg(key, errorDescription));
    }




}