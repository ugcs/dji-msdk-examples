package com.example.ugcssample.model.coordinate;

import android.os.Parcel;

/**
 * Stores latitude, longitude, and altitude information for a coordinate.
 */
public class LatLongAlt extends LatLong {

    public static final Creator<LatLongAlt> CREATOR = new Creator<LatLongAlt>() {
        public LatLongAlt createFromParcel(Parcel source) {
            return new LatLongAlt(source);
        }

        public LatLongAlt[] newArray(int size) {
            return new LatLongAlt[size];
        }
    };

    /**
     * Stores the altitude in meters.
     */
    protected double altitude = 0.0;

    public LatLongAlt() {
    }

    private LatLongAlt(Parcel in) {
        this.latitude = in.readDouble();
        this.longitude = in.readDouble();
        this.altitude = in.readDouble();
    }

    public LatLongAlt(double latitude, double longitude, double altitude) {
        super(latitude, longitude);
        this.altitude = altitude;
    }

    public LatLongAlt(LatLong location, double altitude) {
        super(location);
        this.altitude = altitude;
    }

    public LatLongAlt(LatLongAlt copy) {
        this(copy.getLatitude(), copy.getLongitude(), copy.getAltitude());
    }

    public void set(LatLongAlt source) {
        super.set(source);
        this.altitude = source.altitude;
    }

    /**
     * @return the altitude in meters
     */
    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LatLongAlt)) return false;
        if (!super.equals(o)) return false;

        LatLongAlt that = (LatLongAlt)o;

        if (Double.compare(that.altitude, altitude) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(altitude);
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        final String superToString = super.toString();
        return "LatLongAlt{" + superToString
            + ", mAltitude=" + altitude + '}';
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(this.latitude);
        dest.writeDouble(this.longitude);
        dest.writeDouble(this.altitude);
    }

    public static LatLongAlt copyMe(LatLongAlt val) {
        if (val == null)
            return null;
        return new LatLongAlt(val.latitude, val.longitude, val.altitude);
    }

}
