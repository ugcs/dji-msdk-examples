package com.example.ugcssample.model.command;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;


public class CameraSeriesTime extends MissionItem implements MissionItem.Command, Parcelable {

    public static final Creator<CameraSeriesTime> CREATOR = new Creator<CameraSeriesTime>() {
        public CameraSeriesTime createFromParcel(Parcel source) {
            return new CameraSeriesTime(source);
        }

        public CameraSeriesTime[] newArray(int size) {
            return new CameraSeriesTime[size];
        }
    };

    public boolean transientAtrForceOfBoard = false;
    public boolean transientAtrStartOnWpReached = false;
    // milliseconds.
    private int interval = 0; // (int) one.getParam1(); // Delay interval between two consequent shots in milliseconds.
    private int qty = 0; // (int) one.getParam3(); // Total number of shots
    private int delay = 0; //(int) one.getParam3(); // Initial delay in milliseconds

    public CameraSeriesTime() {
        super(MissionItemType.CAMERA_SERIES_TIME);
    }

    public CameraSeriesTime(int intervalMs) {
        super(MissionItemType.CAMERA_SERIES_TIME);
        interval = intervalMs;
    }

    public CameraSeriesTime(CameraSeriesTime copy) {
        super(MissionItemType.CAMERA_SERIES_TIME, copy.getIndexInSrcCmd());
        interval = copy.interval;
        qty = copy.qty;
        delay = copy.delay;
    }

    private CameraSeriesTime(Parcel in) {
        super(in);
        this.interval = in.readInt();
        this.qty = in.readInt();
        this.delay = in.readInt();
    }

    public int getInterval() {
        return interval;
    }

    public double getIntervalAsDouble() {
        return ((double)interval) / 1000;
    }

    public int getRoundedIntervalInSeconds() {
        return (int) Math.round(((double)interval) / 1000);
    }

    /**
     * Sets time interval between camera shots
     *
     * @param intervalMs between two consequent shots in milliseconds
     */
    public CameraSeriesTime setInterval(int intervalMs) {
        this.interval = intervalMs;
        return this;
    }

    public int getQty() {
        return qty;
    }

    public CameraSeriesTime setQty(int qty) {
        this.qty = qty;
        return this;
    }

    public int getDelay() {
        return delay;
    }

    public CameraSeriesTime setDelay(int delay) {
        this.delay = delay;
        return this;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.interval);
        dest.writeInt(this.qty);
        dest.writeInt(this.delay);
    }

    @Override
    public MissionItem cloneMe() {
        return new CameraSeriesTime(this);
    }

    public CameraSeriesTime setTransientAtrForceOfBoard(boolean transientAtrForceOfBoard) {
        this.transientAtrForceOfBoard = transientAtrForceOfBoard;
        return this;
    }

    public CameraSeriesTime setTransientAtrStartOnWpReached(boolean transientAtrStartOnWpReached) {
        this.transientAtrStartOnWpReached = transientAtrStartOnWpReached;
        return this;
    }
}
