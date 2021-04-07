package com.example.ugcssample.model.spatial;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;
import com.example.ugcssample.model.coordinate.LatLongAlt;


public abstract class BaseSpatialItem extends MissionItem implements MissionItem.SpatialItem, Parcelable {

    private LatLongAlt coordinate;

    protected BaseSpatialItem(MissionItemType type) {
        super(type);
    }

    protected BaseSpatialItem(BaseSpatialItem copy) {
        super(copy.getType(), copy.getIndexInSrcCmd());
        coordinate = copy.coordinate == null ? null : new LatLongAlt(copy.coordinate);
    }

    protected BaseSpatialItem(Parcel in) {
        super(in);
        this.coordinate = in.readParcelable(LatLongAlt.class.getClassLoader());
    }

    @Override
    public LatLongAlt getCoordinate() {
        return coordinate;
    }

    @Override
    public void setCoordinate(LatLongAlt coordinate) {
        this.coordinate = coordinate;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(this.coordinate, flags);
    }

    public double getLat() {
        return getCoordinate().getLatitude();
    }

    public double getLon() {
        return getCoordinate().getLongitude();
    }

    public double getAlt() {
        return getCoordinate().getAltitude();
    }
}
