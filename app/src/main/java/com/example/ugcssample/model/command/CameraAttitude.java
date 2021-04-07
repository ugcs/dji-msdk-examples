package com.example.ugcssample.model.command;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;

import static com.example.ugcssample.model.utils.ParcelableUtils.readBooleanFromParcel;
import static com.example.ugcssample.model.utils.ParcelableUtils.writeBooleanToParcel;


public class CameraAttitude extends MissionItem implements MissionItem.Command, Parcelable {

    public static final Creator<CameraAttitude> CREATOR = new Creator<CameraAttitude>() {
        public CameraAttitude createFromParcel(Parcel source) {
            return new CameraAttitude(source);
        }

        public CameraAttitude[] newArray(int size) {
            return new CameraAttitude[size];
        }
    };

    private double pitch = 0.0; // tilt angle in degrees, where -90 for full down, +90 for Up, 0 straight forward.
    private boolean pitchAvailable = false;
    private double roll = 0.0; // roll value in degrees, where -90 stands for left and 90 for right
    private boolean rollAvailable = false;
    private double yaw = 0.0; // yaw value in degrees, where -90 stands for left and 90 for right.
    private boolean yawAvailable = false;
    private int zoom = 0; //
    private boolean zoomAvailable = false;

    public CameraAttitude() {
        super(MissionItemType.CAMERA_ATTITUDE);
    }

    public CameraAttitude(CameraAttitude copy) {
        super(MissionItemType.CAMERA_ATTITUDE, copy.getIndexInSrcCmd());
        pitch = copy.pitch;
        pitchAvailable = copy.pitchAvailable;
        roll = copy.roll;
        rollAvailable = copy.rollAvailable;
        yaw = copy.yaw;
        yawAvailable = copy.yawAvailable;
        zoom = copy.zoom;
        zoomAvailable = copy.zoomAvailable;
    }

    private CameraAttitude(Parcel in) {
        super(in);
        this.pitch = in.readDouble();
        this.pitchAvailable = readBooleanFromParcel(in);
        this.roll = in.readDouble();
        this.rollAvailable = readBooleanFromParcel(in);
        this.yaw = in.readDouble();
        this.yawAvailable = readBooleanFromParcel(in);
        this.zoom = in.readInt();
        this.zoomAvailable = readBooleanFromParcel(in);
    }

    public double getPitch() {
        return pitch;
    }

    public boolean isPitchAvailable() {
        return pitchAvailable;
    }

    /**
     * Sets camera (gimbal) pitch angle
     *
     * @param pitch angle in degrees, where -90 for Nadir (full down), 0 straight forward
     */
    public CameraAttitude setPitch(double pitch) {
        this.pitch = pitch;
        this.pitchAvailable = true;
        return this;
    }

    public double getRoll() {
        return roll;
    }

    public boolean isRollAvailable() {
        return rollAvailable;
    }

    public CameraAttitude setRoll(double roll) {
        this.roll = roll;
        this.rollAvailable = true;
        return this;
    }

    public double getYaw() {
        return yaw;
    }

    public boolean isYawAvailable() {
        return yawAvailable;
    }

    public CameraAttitude setYaw(double yaw) {
        this.yaw = yaw;
        this.yawAvailable = true;
        return this;
    }

    public int getZoom() {
        return zoom;
    }

    public boolean isZoomAvailable() {
        return zoomAvailable;
    }

    public CameraAttitude setZoom(int zoom) {
        this.zoom = zoom;
        this.zoomAvailable = true;
        return this;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeDouble(this.pitch);
        writeBooleanToParcel(dest, this.pitchAvailable);
        dest.writeDouble(this.roll);
        writeBooleanToParcel(dest, this.rollAvailable);
        dest.writeDouble(this.yaw);
        writeBooleanToParcel(dest, this.yawAvailable);
        dest.writeDouble(this.zoom);
        writeBooleanToParcel(dest, this.zoomAvailable);
    }

    @Override
    public MissionItem cloneMe() {
        return new CameraAttitude(this);
    }

}
