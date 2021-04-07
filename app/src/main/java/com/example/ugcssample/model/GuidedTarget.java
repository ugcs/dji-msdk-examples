package com.example.ugcssample.model;

import android.os.Parcel;

import com.example.ugcssample.model.coordinate.LatLongAlt;

public class GuidedTarget implements android.os.Parcelable {

    public static final Creator<GuidedTarget> CREATOR = new Creator<GuidedTarget>() {
        public GuidedTarget createFromParcel(Parcel source) {
            return new GuidedTarget(source);
        }

        public GuidedTarget[] newArray(int size) {
            return new GuidedTarget[size];
        }
    };

    public LatLongAlt coordinate;
    public double altitudeAmsl;
    public double speed;
    public Double heading;
    public double radius;

    public GuidedTarget() {
    }

    public GuidedTarget(LatLongAlt coordinate, double altitudeAmsl, double speed) {
        this.coordinate = coordinate;
        this.altitudeAmsl = altitudeAmsl;
        this.speed = speed;
        this.heading = null;
        this.radius = 1d;
    }

    private GuidedTarget(Parcel in) {
        this.coordinate = in.readParcelable(LatLongAlt.class.getClassLoader());
        this.altitudeAmsl = in.readDouble();
        this.speed = in.readDouble();
        this.heading = in.readDouble();
        this.radius = in.readDouble();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.coordinate, flags);
        dest.writeDouble(this.altitudeAmsl);
        dest.writeDouble(this.speed);
        dest.writeDouble(this.heading);
        dest.writeDouble(this.radius);
    }

}
