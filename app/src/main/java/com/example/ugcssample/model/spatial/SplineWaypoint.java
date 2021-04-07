package com.example.ugcssample.model.spatial;

import android.os.Parcel;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;
import com.example.ugcssample.model.coordinate.LatLongAlt;

public class SplineWaypoint extends BaseSpatialItem implements android.os.Parcelable {

    public static final Creator<SplineWaypoint> CREATOR = new Creator<SplineWaypoint>() {
        public SplineWaypoint createFromParcel(Parcel source) {
            return new SplineWaypoint(source);
        }

        public SplineWaypoint[] newArray(int size) {
            return new SplineWaypoint[size];
        }
    };

    /**
     * set to 0.0 if you want use system default
     */
    private double maxCornerRadius = 0.0;

    public SplineWaypoint() {
        super(MissionItemType.SPLINE_WAYPOINT);
    }

    public SplineWaypoint(double lat, double lon, double alt) {
        this();
        setCoordinate(new LatLongAlt(lat, lon, alt));
    }

    public SplineWaypoint(double lat, double lon, double alt, double maxCornerRadius) {
        this();
        setCoordinate(new LatLongAlt(lat, lon, alt));
        this.maxCornerRadius = maxCornerRadius;
    }

    public SplineWaypoint(LatLongAlt lla) {
        this();
        setCoordinate(lla);
    }

    public SplineWaypoint(SplineWaypoint copy) {
        super(copy);
        this.maxCornerRadius = copy.maxCornerRadius;
    }

    private SplineWaypoint(Parcel in) {
        super(in);
        this.maxCornerRadius = in.readDouble();
    }

    public double getMaxCornerRadius() {
        return maxCornerRadius;
    }

    public SplineWaypoint setMaxCornerRadius(double maxCornerRadius) {
        this.maxCornerRadius = maxCornerRadius;
        return this;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeDouble(this.maxCornerRadius);
    }

    @Override
    public MissionItem cloneMe() {
        return new SplineWaypoint(this);
    }

}
