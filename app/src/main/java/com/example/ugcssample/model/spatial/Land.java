package com.example.ugcssample.model.spatial;

import android.os.Parcel;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;
import com.example.ugcssample.model.coordinate.LatLongAlt;

public class Land extends BaseSpatialItem implements android.os.Parcelable {

    public static final Creator<Land> CREATOR = new Creator<Land>() {
        public Land createFromParcel(Parcel source) {
            return new Land(source);
        }

        public Land[] newArray(int size) {
            return new Land[size];
        }
    };

    public Land() {
        super(MissionItemType.LAND);
    }

    public Land(LatLongAlt lla) {
        super(MissionItemType.LAND);
        setCoordinate(lla);
    }

    public Land(Land copy) {
        super(copy);
    }

    private Land(Parcel in) {
        super(in);
    }

    @Override
    public MissionItem cloneMe() {
        return new Land(this);
    }

}
