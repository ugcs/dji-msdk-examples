package com.example.ugcssample.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.ugcssample.model.coordinate.LatLongAlt;

public abstract class MissionItem implements Cloneable, Parcelable {

    public interface Command {
    }

    public interface SpatialItem {
        LatLongAlt getCoordinate();

        void setCoordinate(LatLongAlt coordinate);
    }

    public interface ComplexItem<T extends MissionItem> {
        void copy(T source);
    }

    /**
     * Used when mission generated from VsmMessagesProto.Device_command
     * Each mission Device_command (sub command) must be mapped to MissionItem
     */
    private int indexInSrcCmd = -1;
    private final MissionItemType type;

    public String overrideMarkerText = null;
    public boolean visibleOnMap = true;

    protected MissionItem(MissionItemType type) {
        this.type = type;
    }

    protected MissionItem(MissionItemType type, int indexInSrcCmd) {
        this.type = type;
        this.indexInSrcCmd = indexInSrcCmd;
    }

    protected MissionItem(Parcel in) {
        this.type = MissionItemType.values()[in.readInt()];
        this.indexInSrcCmd = in.readInt();
    }

    public MissionItemType getType() {
        return type;
    }

    public int getIndexInSrcCmd() {
        return indexInSrcCmd;
    }

    public MissionItem setIndexInSrcCmd(int indexInSrcCmd) {
        this.indexInSrcCmd = indexInSrcCmd;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.type.ordinal());
        dest.writeInt(this.indexInSrcCmd);

    }

    public abstract MissionItem cloneMe();

}
