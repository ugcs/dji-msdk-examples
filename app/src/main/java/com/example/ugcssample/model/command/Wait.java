package com.example.ugcssample.model.command;

import android.os.Parcel;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;

public class Wait extends MissionItem implements MissionItem.Command, android.os.Parcelable {

    public static final Creator<Wait> CREATOR = new Creator<Wait>() {
        public Wait createFromParcel(Parcel source) {
            return new Wait(source);
        }

        public Wait[] newArray(int size) {
            return new Wait[size];
        }
    };

    // time in milliseconds
    private int time = 0;

    public Wait() {
        super(MissionItemType.WAIT);
    }

    public Wait(int time) {
        super(MissionItemType.WAIT);
        this.time = time;
    }

    public Wait(Wait copy) {
        super(MissionItemType.WAIT, copy.getIndexInSrcCmd());
        time = copy.time;
    }

    private Wait(Parcel in) {
        super(in);
        this.time = in.readInt();
    }

    /**
     * Keeps the vehicle at the waypoint's location.
     * I.e. determine how much time in milliseconds the aircraft will stay at the location.
     *
     * @return time in milliseconds
     */
    public int getTime() {
        return time;
    }

    /**
     * Sets wait time in milliseconds
     *
     * @param time time in milliseconds
     */
    public Wait setTime(int time) {
        this.time = time;
        return this;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.time);
    }

    @Override
    public MissionItem cloneMe() {
        return new Wait(this);
    }

}
