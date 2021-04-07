package com.example.ugcssample.model.command;

import android.os.Parcel;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;

public class Yaw extends MissionItem implements MissionItem.Command, android.os.Parcelable {

    public static final Creator<Yaw> CREATOR = new Creator<Yaw>() {
        public Yaw createFromParcel(Parcel source) {
            return new Yaw(source);
        }

        public Yaw[] newArray(int size) {
            return new Yaw[size];
        }
    };

    private double angle = 0.0;

    public Yaw() {
        super(MissionItemType.YAW);
    }

    public Yaw(Yaw copy) {
        super(MissionItemType.YAW, copy.getIndexInSrcCmd());
        angle = copy.angle;
    }

    private Yaw(Parcel in) {
        super(in);
        this.angle = in.readDouble();
    }

    public double getAngle() {
        return angle;
    }

    public Yaw setAngle(double angle) {
        this.angle = angle;
        return this;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeDouble(this.angle);
    }

    @Override
    public MissionItem cloneMe() {
        return new Yaw(this);
    }

}
