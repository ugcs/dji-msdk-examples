package com.example.ugcssample.model.command;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;

public class CameraFocus extends MissionItem implements MissionItem.Command, Parcelable {

    public static final Creator<CameraFocus> CREATOR = new Creator<CameraFocus>() {
        public CameraFocus createFromParcel(Parcel source) {
            return new CameraFocus(source);
        }

        public CameraFocus[] newArray(int size) {
            return new CameraFocus[size];
        }
    };

    private int zoom = 0;

    public CameraFocus() {
        super(MissionItemType.CAMERA_FOCUS);
    }

    public CameraFocus(CameraFocus copy) {
        super(MissionItemType.CAMERA_FOCUS, copy.getIndexInSrcCmd());
        zoom = copy.zoom;
    }

    private CameraFocus(Parcel in) {
        super(in);
        this.zoom = in.readInt();
    }

    public int getZoom() {
        return zoom;
    }

    public CameraFocus setZoom(int zoom) {
        this.zoom = zoom;
        return this;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeDouble(this.zoom);
    }

    @Override
    public MissionItem cloneMe() {
        return new CameraFocus(this);
    }

}
