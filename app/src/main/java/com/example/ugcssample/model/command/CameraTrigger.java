package com.example.ugcssample.model.command;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;


public class CameraTrigger extends MissionItem implements MissionItem.Command, Parcelable {

    public static final Creator<CameraTrigger> CREATOR = new Creator<CameraTrigger>() {
        public CameraTrigger createFromParcel(Parcel source) {
            return new CameraTrigger(source);
        }

        public CameraTrigger[] newArray(int size) {
            return new CameraTrigger[size];
        }
    };

    public enum CameraTriggerModeType {
        SINGLE_SHOT,
        START_RECORDING,
        STOP_RECORDING
    }

    private CameraTriggerModeType mode = CameraTriggerModeType.SINGLE_SHOT;

    public CameraTrigger() {
        super(MissionItemType.CAMERA_TRIGGER);
    }

    public CameraTrigger(CameraTrigger copy) {
        super(MissionItemType.CAMERA_TRIGGER, copy.getIndexInSrcCmd());
        mode = copy.mode;
    }

    private CameraTrigger(Parcel in) {
        super(in);
        this.mode = CameraTriggerModeType.values()[in.readInt()];
    }

    public CameraTriggerModeType getMode() {
        return mode;
    }

    /**
     * Sets what camera action should be performed
     *
     * @param mode is one of SINGLE_SHOT, START_RECORDING or STOP_RECORDING
     */
    public CameraTrigger setMode(CameraTriggerModeType mode) {
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
        return new CameraTrigger(this);
    }

}
