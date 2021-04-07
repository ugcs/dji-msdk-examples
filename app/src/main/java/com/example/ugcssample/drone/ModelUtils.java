package com.example.ugcssample.drone;

import com.example.ugcssample.model.coordinate.LatLong;

import dji.common.camera.SettingsDefinitions;
import dji.common.model.LocationCoordinate2D;

public class ModelUtils {

    public static boolean useNativePhotoByDistance() {
        return true;
    }

    public static boolean useNativeChangeSpeed() {
        return true;
    }

    public static boolean useNativePhotoByTime() {
        return true;
    }

    public static LocationCoordinate2D toDjiLocation(LatLong ll) {
        return new LocationCoordinate2D(ll.getLatitude(), ll.getLongitude());
    }
}
