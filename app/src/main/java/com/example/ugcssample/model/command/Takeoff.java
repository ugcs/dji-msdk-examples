package com.example.ugcssample.model.command;

import android.os.Parcel;

import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;


/**
 * The vehicle will climb straight up from it’s current location to the altitude specified (in meters).
 * This should be the first command of nearly all missions.
 * If the mission is begun while the copter is already flying,
 * the vehicle will climb straight up to the specified altitude.
 * If the vehicle is already above the specified altitude the takeoff command will be ignored and
 * the mission will move onto the next command immediately.
 */
public class Takeoff extends MissionItem implements MissionItem.Command, android.os.Parcelable {

    public static final Creator<Takeoff> CREATOR = new Creator<Takeoff>() {
        public Takeoff createFromParcel(Parcel source) {
            return new Takeoff(source);
        }

        public Takeoff[] newArray(int size) {
            return new Takeoff[size];
        }
    };

    /**
     * Default takeoff altitude in meters.
     */
    //FIXME must be in properties
    public static final double DEFAULT_TAKEOFF_ALTITUDE = 10.0;

    private double takeoffAltitude;

    public Takeoff() {
        super(MissionItemType.TAKEOFF);
    }

    public Takeoff(Takeoff copy) {
        super(MissionItemType.TAKEOFF, copy.getIndexInSrcCmd());
        takeoffAltitude = copy.takeoffAltitude;
    }

    private Takeoff(Parcel in) {
        super(in);
        this.takeoffAltitude = in.readDouble();
    }

    /**
     * @return take off altitude in meters
     */
    public double getTakeoffAltitude() {
        return takeoffAltitude;
    }

    /**
     * Sets the take off altitude
     *
     * @param takeoffAltitude Altitude value in meters
     */
    public Takeoff setTakeoffAltitude(double takeoffAltitude) {
        this.takeoffAltitude = takeoffAltitude;
        return this;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeDouble(this.takeoffAltitude);
    }

    @Override
    public MissionItem cloneMe() {
        return new Takeoff(this);
    }

}
