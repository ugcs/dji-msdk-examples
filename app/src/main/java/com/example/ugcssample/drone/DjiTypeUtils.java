package com.example.ugcssample.drone;

import androidx.annotation.NonNull;

import com.example.ugcssample.model.EmergencyActionType;
import com.example.ugcssample.model.coordinate.LatLong;
import com.example.ugcssample.model.coordinate.LatLongAlt;
import com.example.ugcssample.model.type.AngleType;
import com.example.ugcssample.model.utils.AppUtils;

import dji.common.flightcontroller.ConnectionFailSafeBehavior;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.flightcontroller.flyzone.FlyZoneCategory;
import dji.common.flightcontroller.flyzone.FlyZoneReason;
import dji.common.flightcontroller.imu.IMUState;
import dji.common.flightcontroller.imu.SensorState;
import dji.common.gimbal.GimbalMode;
import dji.common.gimbal.RotationMode;
import dji.common.model.LocationCoordinate2D;

public class DjiTypeUtils {

    private DjiTypeUtils() {
    }

    public static EmergencyActionType toEmergencyActionType(ConnectionFailSafeBehavior behavior) {
        if (behavior != null) {
            switch (behavior) {
                case GO_HOME:
                    return EmergencyActionType.RTH;
                case LANDING:
                    return EmergencyActionType.LAND;
                case HOVER:
                    return EmergencyActionType.WAIT;
                default:
                    return null;
            }
        }
        return null;
    }

    public static ConnectionFailSafeBehavior fromEmergencyActionType(EmergencyActionType actionType) {
        if (actionType != null) {
            switch (actionType) {
                case RTH:
                    return ConnectionFailSafeBehavior.GO_HOME;
                case LAND:
                    return ConnectionFailSafeBehavior.LANDING;
                case WAIT:
                    return ConnectionFailSafeBehavior.HOVER;
                case CONTINUE:
                    return null;
                default:
                    AppUtils.unhandledSwitch(actionType.name());
                    return null;
            }
        }
        return null;
    }

    public static int getGimbalModeTypeName(GimbalMode mode) {
        switch (mode) {
            case FREE:
                return 1;
            case FPV:
                return 2;
            case YAW_FOLLOW:
                return 3;
            default:
                AppUtils.unhandledSwitch(mode.name());
                return -1;
        }
    }


    public static LatLong toLatLong(LocationCoordinate2D src) {
        if (src == null)
            return null;
        return new LatLong(src.getLatitude(), src.getLongitude());
    }

    public static LatLongAlt toLatLongAlt(dji.common.mission.waypoint.Waypoint wp) {
        if (wp == null)
            return null;
        return new LatLongAlt(wp.coordinate.getLatitude(), wp.coordinate.getLongitude(), wp.altitude);
    }

    public static RotationMode toRotationMode(@NonNull AngleType angleType) {
        switch (angleType) {
            case ABSOLUTE_ANGLE:
                return RotationMode.ABSOLUTE_ANGLE;
            case RELATIVE_ANGLE:
                return RotationMode.RELATIVE_ANGLE;
            default:
                AppUtils.unhandledSwitch(angleType.name());
                return null;
        }
    }

}
