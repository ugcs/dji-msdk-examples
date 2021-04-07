package com.example.ugcssample.model.command;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;

public class CameraZoom extends MissionItem implements MissionItem.Command, Parcelable {

    public static final Creator<CameraZoom> CREATOR = new Creator<CameraZoom>() {
        public CameraZoom createFromParcel(Parcel source) {
            return new CameraZoom(source);
        }

        public CameraZoom[] newArray(int size) {
            return new CameraZoom[size];
        }
    };

    private int zoom = 0;

    public CameraZoom() {
        super(MissionItemType.CAMERA_ZOOM);
    }

    public CameraZoom(CameraZoom copy) {
        super(MissionItemType.CAMERA_ZOOM, copy.getIndexInSrcCmd());
        zoom = copy.zoom;
    }

    private CameraZoom(Parcel in) {
        super(in);
        this.zoom = in.readInt();
    }

    public int getZoom() {
        return zoom;
    }

    public CameraZoom setZoom(int zoom) {
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
        return new CameraZoom(this);
    }

}
