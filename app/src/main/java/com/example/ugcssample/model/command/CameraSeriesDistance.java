package com.example.ugcssample.model.command;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;

public class CameraSeriesDistance extends MissionItem implements MissionItem.Command, Parcelable {

    public static final Creator<CameraSeriesDistance> CREATOR = new Creator<CameraSeriesDistance>() {
        public CameraSeriesDistance createFromParcel(Parcel source) {
            return new CameraSeriesDistance(source);
        }

        public CameraSeriesDistance[] newArray(int size) {
            return new CameraSeriesDistance[size];
        }
    };

    private double distance = 0.0; // Distance between two consequent shots in meters.
    private int qty = 0; // Total number of shots
    private int delay = 0; // Initial delay in milliseconds

    public CameraSeriesDistance() {
        super(MissionItemType.CAMERA_SERIES_DISTANCE);
    }

    public CameraSeriesDistance(CameraSeriesDistance copy) {
        super(MissionItemType.CAMERA_SERIES_DISTANCE, copy.getIndexInSrcCmd());
        distance = copy.distance;
        qty = copy.qty;
        delay = copy.delay;
    }

    private CameraSeriesDistance(Parcel in) {
        super(in);
        this.distance = in.readDouble();
        this.qty = in.readInt();
        this.delay = in.readInt();
    }

    public double getDistance() {
        return distance;
    }

    public CameraSeriesDistance setDistance(double distance) {
        this.distance = distance;
        return this;
    }

    public int getQty() {
        return qty;
    }

    public CameraSeriesDistance setQty(int qty) {
        this.qty = qty;
        return this;
    }

    public int getDelay() {
        return delay;
    }

    public CameraSeriesDistance setDelay(int delay) {
        this.delay = delay;
        return this;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeDouble(this.distance);
        dest.writeInt(this.qty);
        dest.writeInt(this.delay);
    }

    @Override
    public MissionItem cloneMe() {
        return new CameraSeriesDistance(this);
    }
}
