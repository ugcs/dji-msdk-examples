package com.example.ugcssample.model;

import android.os.Parcel;
import android.os.Parcelable;

public class MissionAttributes implements Parcelable {

    public static final Creator<MissionAttributes> CREATOR = new Creator<MissionAttributes>() {
        public MissionAttributes createFromParcel(Parcel in) {
            return new MissionAttributes(in);
        }

        public MissionAttributes[] newArray(int size) {
            return new MissionAttributes[size];
        }
    };

    // ---- Home Location source ----
    private HomeLocationSourceType homeLocationSourceType = HomeLocationSourceType.NONE;
    // not NULL only when EXPLICIT
    private double homeLat = 0.0;
    private double homeLon = 0.0;

    // ---- Failsafe actions ----
    // in meters
    public double returnAltitude = 50.0;
    public EmergencyActionType rcLostAction = EmergencyActionType.CONTINUE;
    public EmergencyActionType gpsLostAction = EmergencyActionType.WAIT;
    public EmergencyActionType lowBatteryAction = EmergencyActionType.LAND;

    // --- Waypoint mission attributes ---
    // TODO: 7.12.18 do these need to be parceled?
    public GotoWaypointMode gotoFirstWaypointMode = GotoWaypointMode.DEFAULT;

    private MissionAttributes(Parcel in) {
        // ---- Home Location source ----
        this.homeLocationSourceType = HomeLocationSourceType.values()[in.readInt()];
        this.homeLat = in.readDouble();
        this.homeLon = in.readDouble();

        // ---- Failsafe actions ----
        this.returnAltitude = in.readDouble();
        this.rcLostAction = EmergencyActionType.values()[in.readInt()];
        this.gpsLostAction = EmergencyActionType.values()[in.readInt()];
        this.lowBatteryAction = EmergencyActionType.values()[in.readInt()];
    }

    public MissionAttributes() {
    }

    /***/
    public MissionAttributes setHomeLocationSourceType(HomeLocationSourceType homeLocationSourceType) {
        if (homeLocationSourceType == HomeLocationSourceType.EXPLICIT) {
            throw new RuntimeException("EXPLICIT - home can be set only by setExplicitHomeLocation method");
        }
        this.homeLocationSourceType = homeLocationSourceType;
        this.homeLat = 0.0;
        this.homeLon = 0.0;
        return this;
    }

    public HomeLocationSourceType getHomeLocationSourceType() {
        return homeLocationSourceType;
    }

    public void setExplicitHomeLocation(double homeLat, double homeLon) {
        this.homeLocationSourceType = HomeLocationSourceType.EXPLICIT;
        this.homeLat = homeLat;
        this.homeLon = homeLon;
    }

    public double getHomeLat() {
        return homeLat;
    }

    public double getHomeLon() {
        return homeLon;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // ---- Home Location source ----
        dest.writeInt(this.homeLocationSourceType.ordinal());
        dest.writeDouble(this.homeLat);
        dest.writeDouble(this.homeLon);

        // ---- Failsafe actions ----
        dest.writeDouble(this.returnAltitude);
        dest.writeInt(this.rcLostAction.ordinal());
        dest.writeInt(this.gpsLostAction.ordinal());
        dest.writeInt(this.lowBatteryAction.ordinal());
    }

}