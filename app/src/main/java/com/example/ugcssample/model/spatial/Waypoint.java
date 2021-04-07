package com.example.ugcssample.model.spatial;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;
import com.example.ugcssample.model.coordinate.LatLongAlt;

public class Waypoint extends BaseSpatialItem implements Parcelable {

    public static final Creator<Waypoint> CREATOR = new Creator<Waypoint>() {
        public Waypoint createFromParcel(Parcel source) {
            return new Waypoint(source);
        }

        public Waypoint[] newArray(int size) {
            return new Waypoint[size];
        }
    };

    public Waypoint() {
        super(MissionItemType.WAYPOINT);
    }

    public Waypoint(double lat, double lon, double alt) {
        this();
        setCoordinate(new LatLongAlt(lat, lon, alt));
    }

    public Waypoint(LatLongAlt lla) {
        this();
        setCoordinate(lla);
    }

    public Waypoint(Waypoint copy) {
        super(copy);
    }

    private Waypoint(Parcel in) {
        super(in);
        // for boolean this.orbitCCW = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        // for boolean dest.writeByte(orbitCCW ? (byte) 1 : (byte) 0);
    }

    @Override
    public MissionItem cloneMe() {
        return new Waypoint(this);
    }

}
