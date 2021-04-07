package com.example.ugcssample.model.command;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;

public class MissionPause extends MissionItem implements MissionItem.Command, Parcelable {

    public static final Creator<MissionPause> CREATOR = new Creator<MissionPause>() {
        public MissionPause createFromParcel(Parcel source) {
            return new MissionPause(source);
        }

        public MissionPause[] newArray(int size) {
            return new MissionPause[size];
        }
    };

    // time in milliseconds
    private int delayTime = 0;

    public MissionPause() {
        super(MissionItemType.MISSION_PAUSE);
    }

    public MissionPause(int delayTime) {
        super(MissionItemType.MISSION_PAUSE);
        this.delayTime = delayTime;
    }

    public MissionPause(MissionPause copy) {
        super(MissionItemType.MISSION_PAUSE, copy.getIndexInSrcCmd());
        this.delayTime = copy.delayTime;
    }

    private MissionPause(Parcel in) {
        super(in);
        this.delayTime = in.readInt();
    }

    public int getDelayTime() {
        return delayTime;
    }

    public MissionPause setDelayTime(int delayTime) {
        this.delayTime = delayTime;
        return this;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.delayTime);
    }

    @Override
    public MissionItem cloneMe() {
        return new MissionPause(this);
    }

}
