package com.example.ugcssample.model.command;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;

public class Panorama extends MissionItem implements MissionItem.Command, Parcelable {

    public static final Creator<Panorama> CREATOR = new Creator<Panorama>() {
        public Panorama createFromParcel(Parcel source) {
            return new Panorama(source);
        }

        public Panorama[] newArray(int size) {
            return new Panorama[size];
        }
    };

    public enum PanoramaModeType {
        MODE_PHOTO,
        MODE_VIDEO
    }

    private PanoramaModeType mode = PanoramaModeType.MODE_PHOTO;
    private double angle = 0.0; // pan. angle [-360, 360], positive = clockwise
    private double step = 0.0;  //step angle, zero stands for a continuous rotation.
    private double velocity = 0.0; // rotation speed - deg per second
    private int delay = 0; // Delay interval between two steps in milliseconds.

    public Panorama() {
        super(MissionItemType.PANORAMA);
    }

    public Panorama(Panorama copy) {
        super(MissionItemType.CAMERA_TRIGGER, copy.getIndexInSrcCmd());
        mode = copy.mode;
        angle = copy.angle;
        step = copy.step;
        velocity = copy.velocity;
        delay = copy.delay;
    }

    private Panorama(Parcel in) {
        super(in);
        this.mode = PanoramaModeType.values()[in.readInt()];
        this.angle = in.readDouble();
        this.step = in.readDouble();
        this.velocity = in.readDouble();
        this.delay = in.readInt();
    }

    public PanoramaModeType getMode() {
        return mode;
    }

    /**
     * Set what panorama type will be performed: photo or video
     *
     * @param mode is MODE_PHOTO or MODE_VIDEO
     */
    public Panorama setMode(PanoramaModeType mode) {
        this.mode = mode;
        return this;
    }

    public double getAngle() {
        return angle;
    }

    /**
     * Sets panorama angle. Positive angle means camera rotation clockwise.
     *
     * @param angle normally must be [-360, 360],
     *              however could exceed range if user wants to perform more than 1 revolution
     */
    public Panorama setAngle(double angle) {
        this.angle = angle;
        return this;
    }

    public double getStep() {
        return step;
    }

    public Panorama setStep(double step) {
        this.step = step;
        return this;
    }

    public double getVelocity() {
        return velocity;
    }

    public Panorama setVelocity(double velocity) {
        this.velocity = velocity;
        return this;
    }

    public int getDelay() {
        return delay;
    }

    public Panorama setDelay(int delay) {
        this.delay = delay;
        return this;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.mode.ordinal());
        dest.writeDouble(this.angle);
        dest.writeDouble(this.step);
        dest.writeDouble(this.velocity);
        dest.writeInt(this.delay);
    }

    @Override
    public MissionItem cloneMe() {
        return new Panorama(this);
    }

}
