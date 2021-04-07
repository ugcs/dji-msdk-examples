package com.example.ugcssample.model.coordinate;

import android.os.Parcel;

/**
 * Stores latitude, longitude, and altitude information for a coordinate.
 */
public class LatLongAltYaw extends LatLongAlt {

    public static final Creator<LatLongAltYaw> CREATOR = new Creator<LatLongAltYaw>() {
        public LatLongAltYaw createFromParcel(Parcel source) {
            return new LatLongAltYaw(source);
        }

        public LatLongAltYaw[] newArray(int size) {
            return new LatLongAltYaw[size];
        }
    };

    /**
     * Stores the altitude in meters.
     */
    private double yaw = 0.0;

    public LatLongAltYaw() {
    }

    public LatLongAltYaw(double lat, double lon, double alt, double yaw) {
        super(lat, lon, alt);
        this.yaw = yaw;
    }

    public LatLongAltYaw(LatLong ll, double alt, double yaw) {
        super(ll, alt);
        this.yaw = yaw;
    }

    public LatLongAltYaw(LatLongAlt lla, double yaw) {
        super(lla);
        this.yaw = yaw;
    }

    private LatLongAltYaw(Parcel in) {
        this.latitude = in.readDouble();
        this.longitude = in.readDouble();
        this.altitude = in.readDouble();
        this.yaw = in.readDouble();
    }

    public double getYaw() {
        return yaw;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(this.latitude);
        dest.writeDouble(this.longitude);
        dest.writeDouble(this.altitude);
        dest.writeDouble(this.yaw);
    }

    public static LatLongAltYaw copyMe(LatLongAltYaw val) {
        if (val == null)
            return null;
        return new LatLongAltYaw(val.latitude, val.longitude, val.altitude, val.yaw);
    }

}
