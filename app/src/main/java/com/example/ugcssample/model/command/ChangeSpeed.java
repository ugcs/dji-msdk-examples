package com.example.ugcssample.model.command;

import android.os.Parcel;
import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;

public class ChangeSpeed extends MissionItem implements MissionItem.Command, android.os.Parcelable {

    public static final Creator<ChangeSpeed> CREATOR = new Creator<ChangeSpeed>() {
        public ChangeSpeed createFromParcel(Parcel source) {
            return new ChangeSpeed(source);
        }

        public ChangeSpeed[] newArray(int size) {
            return new ChangeSpeed[size];
        }
    };
    private double speed;

    public ChangeSpeed() {
        super(MissionItemType.CHANGE_SPEED);
    }

    public ChangeSpeed(double speed) {
        super(MissionItemType.CHANGE_SPEED);
        this.speed = speed;
    }

    public ChangeSpeed(ChangeSpeed copy) {
        super(MissionItemType.CHANGE_SPEED, copy.getIndexInSrcCmd());
        this.speed = copy.speed;
    }

    private ChangeSpeed(Parcel in) {
        super(in);
        this.speed = in.readDouble();
    }

    public double getSpeed() {
        return speed;
    }

    public ChangeSpeed setSpeed(double speed) {
        this.speed = speed;
        return this;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeDouble(this.speed);
    }

    @Override
    public MissionItem cloneMe() {
        return new ChangeSpeed(this);
    }

    public static double getSpeed(MissionItem missionItem) {
        return ((ChangeSpeed)missionItem).getSpeed();
    }

}
