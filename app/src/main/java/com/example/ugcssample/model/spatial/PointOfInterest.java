package com.example.ugcssample.model.spatial;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;
import com.example.ugcssample.model.coordinate.LatLongAlt;

public class PointOfInterest extends BaseSpatialItem implements Parcelable {

    public static final Creator<PointOfInterest> CREATOR = new Creator<PointOfInterest>() {
        public PointOfInterest createFromParcel(Parcel source) {
            return new PointOfInterest(source);
        }

        public PointOfInterest[] newArray(int size) {
            return new PointOfInterest[size];
        }
    };

    public enum RegionOfInterestType {
        NONE,
        LOCATION
    }

    private RegionOfInterestType mode = RegionOfInterestType.LOCATION;

    public PointOfInterest() {
        super(MissionItemType.POINT_OF_INTEREST);
    }

    public PointOfInterest(LatLongAlt lla) {
        this();
        setCoordinate(lla);
    }

    public PointOfInterest(LatLongAlt lla, boolean visibleOnMap) {
        this();
        setCoordinate(lla);
        this.visibleOnMap = visibleOnMap;
    }

    public PointOfInterest(PointOfInterest copy) {
        super(copy);
        this.mode = copy.mode;
    }

    private PointOfInterest(Parcel in) {
        super(in);
        this.mode = RegionOfInterestType.values()[in.readInt()];
    }

    public RegionOfInterestType getMode() {
        return mode;
    }

    public PointOfInterest setMode(RegionOfInterestType mode) {
        this.mode = mode;
        return this;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.mode.ordinal());
    }

    @Override
    public MissionItem cloneMe() {
        return new PointOfInterest(this);
    }

}
