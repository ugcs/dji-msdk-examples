package com.example.ugcssample.model.command;

import android.os.Parcel;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;


public class ReturnToHome extends MissionItem implements MissionItem.Command, android.os.Parcelable {

    public static final Creator<ReturnToHome> CREATOR = new Creator<ReturnToHome>() {
        public ReturnToHome createFromParcel(Parcel source) {
            return new ReturnToHome(source);
        }

        public ReturnToHome[] newArray(int size) {
            return new ReturnToHome[size];
        }
    };

    public ReturnToHome() {
        super(MissionItemType.RETURN_TO_HOME);
    }

    private ReturnToHome(Parcel in) {
        super(in);
    }

    public ReturnToHome(ReturnToHome copy) {
        super(MissionItemType.RETURN_TO_HOME, copy.getIndexInSrcCmd());
    }

    @Override
    public MissionItem cloneMe() {
        return new ReturnToHome(this);
    }

}
