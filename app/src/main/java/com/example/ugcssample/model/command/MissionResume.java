package com.example.ugcssample.model.command;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;


public class MissionResume extends MissionItem implements MissionItem.Command, Parcelable {

    public static final Creator<MissionResume> CREATOR = new Creator<MissionResume>() {
        public MissionResume createFromParcel(Parcel source) {
            return new MissionResume(source);
        }

        public MissionResume[] newArray(int size) {
            return new MissionResume[size];
        }
    };

    // time in milliseconds
    private int delayTime = 0;

    public MissionResume() {
        super(MissionItemType.MISSION_PAUSE);
    }

    public MissionResume(int delayTime) {
        super(MissionItemType.MISSION_PAUSE);
        this.delayTime = delayTime;
    }

    public MissionResume(MissionResume copy) {
        super(MissionItemType.MISSION_PAUSE, copy.getIndexInSrcCmd());
        this.delayTime = copy.delayTime;
    }

    private MissionResume(Parcel in) {
        super(in);
        this.delayTime = in.readInt();
    }

    public int getDelayTime() {
        return delayTime;
    }

    public MissionResume setDelayTime(int delayTime) {
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
        return new MissionResume(this);
    }

}
