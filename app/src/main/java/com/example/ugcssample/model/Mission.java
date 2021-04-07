package com.example.ugcssample.model;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;


import com.example.ugcssample.model.command.ChangeSpeed;
import com.example.ugcssample.model.command.Takeoff;
import com.example.ugcssample.model.coordinate.LatLong;
import com.example.ugcssample.model.utils.MathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dji.common.mission.MissionUtils;

import static com.example.ugcssample.model.HomeLocationSourceType.EXPLICIT;
import static com.example.ugcssample.model.MissionItemType.CHANGE_SPEED;
import static com.example.ugcssample.model.MissionItemType.CIRCLE;
import static com.example.ugcssample.model.MissionItemType.LAND;
import static com.example.ugcssample.model.MissionItemType.SPLINE_WAYPOINT;
import static com.example.ugcssample.model.MissionItemType.WAYPOINT;
import static com.example.ugcssample.model.MissionItemType.isKindOfWaypoint;

/**
 * A Mission model.
 * Holds mission attributes and a set of mission items.
 */
public class Mission implements Parcelable {

    public static final Parcelable.Creator<Mission> CREATOR = new Parcelable.Creator<Mission>() {
        public Mission createFromParcel(Parcel source) {
            return new Mission(source);
        }

        public Mission[] newArray(int size) {
            return new Mission[size];
        }
    };

    public double receivedAltOrigin = 0.0;
    public double firstWpElevation = 0.0;
    public Double homeElevation = null;

    /**
     * User specified mission name
     */
    public String missionName;

    public final int missionId;
    public final MissionAttributes missionAttributes;
    private final List<MissionItem> missionItemsList = new ArrayList<>();

    public MissionTransientAttributes transientAttributes = new MissionTransientAttributes();

    public Mission() {
        this.missionAttributes = new MissionAttributes();
        this.missionId = MathUtils.randPositiveInt();
    }

    public Mission(int id, String name, MissionAttributes missionAttributes) {
        this.missionId = id;
        this.missionName = name;
        this.missionAttributes = missionAttributes;
    }

    public Mission(MissionAttributes missionAttributes) {
        this.missionAttributes = missionAttributes;
        this.missionId = MathUtils.randPositiveInt();
    }

    private Mission(Parcel in) {
        this.missionAttributes = in.readParcelable(MissionAttributes.class.getClassLoader());
        this.missionId = in.readInt();

        List<Bundle> missionItemsBundles = new ArrayList<>();
        in.readTypedList(missionItemsBundles, Bundle.CREATOR);
        if (!missionItemsBundles.isEmpty()) {
            for (Bundle bundle : missionItemsBundles) {
                missionItemsList.add(MissionItemType.restoreMissionItemFromBundle(bundle));
            }
        }
    }

    public void addMissionItem(MissionItem missionItem) {
        missionItemsList.add(missionItem);
    }

    public void addMissionItem(int index, MissionItem missionItem) {
        missionItemsList.add(index, missionItem);
    }

    public void removeMissionItem(MissionItem missionItem) {
        missionItemsList.remove(missionItem);
    }

    public void removeMissionItem(int index) {
        missionItemsList.remove(index);
    }

    public void clear() {
        missionItemsList.clear();
    }

    public MissionItem getMissionItem(int index) {
        return missionItemsList.get(index);
    }

    public List<MissionItem> getMissionItems() {
        return missionItemsList;
    }

    public int getMissionItemsCnt() {
        return missionItemsList.size();
    }

    public Takeoff isFirstItemTakeoff() {
        if (!missionItemsList.isEmpty() && missionItemsList.get(0).getType() == MissionItemType.TAKEOFF)
            return (Takeoff)missionItemsList.get(0);
        else
            return null;
    }

    public int getWaypointCount() {
        int i = 0;
        for (MissionItem item : missionItemsList) {
            if (isKindOfWaypoint(item.getType())) {
                i++;
            }
        }
        return i;
    }

    public MissionItem getWaypointAt(int index) {
        for (MissionItem item : missionItemsList) {
            if (isKindOfWaypoint(item.getType())) {
                if (index == 0) {
                    return item;
                }
                index--;
            }
        }
        throw new IllegalArgumentException(String.format(Locale.US, "No waypoint found at index(%d)", index));
    }

    public double getSpeedToPoint(int targetPointIndex) {
        int currentTargetWp = -1;
        double speed = 5.0;

        for (MissionItem mi : getMissionItems()) {
            switch (mi.getType()) {
                case CIRCLE:
                    break;
                case WAYPOINT:
                case SPLINE_WAYPOINT:
                case LAND:
                    currentTargetWp++;
                    break;
                case CHANGE_SPEED:
                    ChangeSpeed cs = (ChangeSpeed)mi;
                    speed = cs.getSpeed();
                    break;
                default:
                    break;
            }
            if (targetPointIndex == currentTargetWp) {
                return speed;
            }
        }

        return speed;
    }

    public int[] generateWaypointScrCmdIndexes() {
        int cnt = getWaypointCount();
        if (cnt == 0)
            return null;

        int[] retVal = new int[cnt];
        int i = 0;
        for (MissionItem item : missionItemsList) {
            MissionItemType type = item.getType();
            if (type == WAYPOINT
                || type == SPLINE_WAYPOINT
                || type == LAND) {
                int ind = item.getIndexInSrcCmd();
                if (ind < 0)
                    return null;
                retVal[i] = ind;
                i++;
            }
        }
        return retVal;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Booleans:
        // dest.writeByte((byte) (myBoolean ? 1 : 0));     //if myBoolean == true, byte == 1
        // myBoolean = in.readByte() != 0;

        dest.writeParcelable(missionAttributes, flags);
        dest.writeInt(missionId);

        List<Bundle> missionItemsBundles = new ArrayList<>(missionItemsList.size());
        if (!missionItemsList.isEmpty()) {
            for (MissionItem missionItem : missionItemsList) {
                missionItemsBundles.add(missionItem.getType().storeMissionItem(missionItem));
            }
        }

        dest.writeTypedList(missionItemsBundles);
    }

}
