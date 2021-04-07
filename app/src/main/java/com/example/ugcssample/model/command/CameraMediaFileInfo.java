package com.example.ugcssample.model.command;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;

public class CameraMediaFileInfo extends MissionItem implements MissionItem.Command, Parcelable {

    public static final Creator<CameraMediaFileInfo> CREATOR = new Creator<CameraMediaFileInfo>() {
        public CameraMediaFileInfo createFromParcel(Parcel source) {
            return new CameraMediaFileInfo(source);
        }

        public CameraMediaFileInfo[] newArray(int size) {
            return new CameraMediaFileInfo[size];
        }
    };

    // time in milliseconds
    private int delayTime = 0;
    private String mediaFileInfo = "";

    public CameraMediaFileInfo() {
        super(MissionItemType.CAMERA_MEDIA_FILE_INFO);
    }

    public CameraMediaFileInfo(String mediaFileInfo) {
        super(MissionItemType.CAMERA_MEDIA_FILE_INFO);
        this.mediaFileInfo = mediaFileInfo;
    }

    public CameraMediaFileInfo(CameraMediaFileInfo copy) {
        super(MissionItemType.CAMERA_MEDIA_FILE_INFO, copy.getIndexInSrcCmd());
        this.mediaFileInfo = copy.mediaFileInfo;
        this.delayTime = copy.delayTime;
    }

    private CameraMediaFileInfo(Parcel in) {
        super(in);
        this.mediaFileInfo = in.readString();
        this.delayTime = in.readInt();
    }

    public String getMediaFileInfo() {
        return mediaFileInfo;
    }

    public CameraMediaFileInfo setMediaFileInfo(String mediaFileInfo) {
        this.mediaFileInfo = mediaFileInfo;
        return this;
    }

    public int getDelayTime() {
        return delayTime;
    }

    public CameraMediaFileInfo setDelayTime(int delayTime) {
        this.delayTime = delayTime;
        return this;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.mediaFileInfo);
        dest.writeInt(this.delayTime);
    }

    @Override
    public MissionItem cloneMe() {
        return new CameraMediaFileInfo(this);
    }

}
