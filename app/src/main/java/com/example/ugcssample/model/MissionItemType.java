package com.example.ugcssample.model;

import android.os.Bundle;
import android.os.Parcelable;

import com.example.ugcssample.model.command.CameraAttitude;
import com.example.ugcssample.model.command.CameraFocus;
import com.example.ugcssample.model.command.CameraMediaFileInfo;
import com.example.ugcssample.model.command.CameraSeriesDistance;
import com.example.ugcssample.model.command.CameraSeriesTime;
import com.example.ugcssample.model.command.CameraTrigger;
import com.example.ugcssample.model.command.CameraZoom;
import com.example.ugcssample.model.command.ChangeSpeed;
import com.example.ugcssample.model.command.MissionPause;
import com.example.ugcssample.model.command.Panorama;
import com.example.ugcssample.model.command.ReturnToHome;
import com.example.ugcssample.model.command.Takeoff;
import com.example.ugcssample.model.command.Wait;
import com.example.ugcssample.model.command.Yaw;
import com.example.ugcssample.model.spatial.Circle;
import com.example.ugcssample.model.spatial.Land;
import com.example.ugcssample.model.spatial.PointOfInterest;
import com.example.ugcssample.model.spatial.SplineWaypoint;
import com.example.ugcssample.model.spatial.Waypoint;
import com.example.ugcssample.model.utils.ParcelableUtils;


public enum MissionItemType {
    /*WAYPOINT, SPLINE_WAYPOINT, LAND, CIRCLE,
    TAKEOFF, CHANGE_SPEED, RETURN_TO_HOME, WAIT, YAW
    CAMERA_TRIGGER, CAMERA_SERIES_TIME, CAMERA_SERIES_DISTANCE, CAMERA_ATTITUDE, PANORAMA
    POINT_OF_INTEREST*/

    WAYPOINT("Waypoint") {
        @Override
        public MissionItem getNewItem() {
            return new Waypoint();
        }

        @Override
        protected Parcelable.Creator<Waypoint> getMissionItemCreator() {
            return Waypoint.CREATOR;
        }
    },

    SPLINE_WAYPOINT("Spline Waypoint") {
        @Override
        public MissionItem getNewItem() {
            return new SplineWaypoint();
        }

        @Override
        protected Parcelable.Creator<SplineWaypoint> getMissionItemCreator() {
            return SplineWaypoint.CREATOR;
        }
    },

    CIRCLE("Circle") {
        @Override
        public MissionItem getNewItem() {
            return new Circle();
        }

        @Override
        protected Parcelable.Creator<Circle> getMissionItemCreator() {
            return Circle.CREATOR;
        }
    },

    LAND("Land") {
        @Override
        public MissionItem getNewItem() {
            return new Land();
        }

        @Override
        protected Parcelable.Creator<Land> getMissionItemCreator() {
            return Land.CREATOR;
        }
    },

    TAKEOFF("Takeoff") {
        @Override
        public MissionItem getNewItem() {
            return new Takeoff();
        }

        @Override
        protected Parcelable.Creator<Takeoff> getMissionItemCreator() {
            return Takeoff.CREATOR;
        }
    },

    CHANGE_SPEED("Change Speed") {
        @Override
        public MissionItem getNewItem() {
            return new ChangeSpeed();
        }

        @Override
        protected Parcelable.Creator<ChangeSpeed> getMissionItemCreator() {
            return ChangeSpeed.CREATOR;
        }
    },

    RETURN_TO_HOME("Return to home") {
        @Override
        public MissionItem getNewItem() {
            return new ReturnToHome();
        }

        @Override
        protected Parcelable.Creator<ReturnToHome> getMissionItemCreator() {
            return ReturnToHome.CREATOR;
        }
    },

    CAMERA_TRIGGER("Camera Trigger") {
        @Override
        public MissionItem getNewItem() {
            return new CameraTrigger();
        }

        @Override
        protected Parcelable.Creator<CameraTrigger> getMissionItemCreator() {
            return CameraTrigger.CREATOR;
        }
    },

    CAMERA_SERIES_TIME("Camera Series By Time") {
        @Override
        public MissionItem getNewItem() {
            return new CameraSeriesTime();
        }

        @Override
        protected Parcelable.Creator<CameraSeriesTime> getMissionItemCreator() {
            return CameraSeriesTime.CREATOR;
        }
    },

    CAMERA_SERIES_DISTANCE("Camera Series By Distance") {
        @Override
        public MissionItem getNewItem() {
            return new CameraSeriesDistance();
        }

        @Override
        protected Parcelable.Creator<CameraSeriesDistance> getMissionItemCreator() {
            return CameraSeriesDistance.CREATOR;
        }
    },

    CAMERA_ATTITUDE("Camera Attitude") {
        @Override
        public MissionItem getNewItem() {
            return new CameraAttitude();
        }

        @Override
        protected Parcelable.Creator<CameraAttitude> getMissionItemCreator() {
            return CameraAttitude.CREATOR;
        }
    },

    CAMERA_ZOOM("Camera Zoom") {
        @Override
        public MissionItem getNewItem() {
            return new CameraZoom();
        }

        @Override
        protected Parcelable.Creator<CameraZoom> getMissionItemCreator() {
            return CameraZoom.CREATOR;
        }
    },

    CAMERA_FOCUS("Camera Focus") {
        @Override
        public MissionItem getNewItem() {
            return new CameraFocus();
        }

        @Override
        protected Parcelable.Creator<CameraFocus> getMissionItemCreator() {
            return CameraFocus.CREATOR;
        }
    },

    CAMERA_MEDIA_FILE_INFO("Camera Media File Info") {
        @Override
        public MissionItem getNewItem() {
            return new CameraMediaFileInfo();
        }

        @Override
        protected Parcelable.Creator<CameraMediaFileInfo> getMissionItemCreator() {
            return CameraMediaFileInfo.CREATOR;
        }
    },

    PANORAMA("Panorama") {
        @Override
        public MissionItem getNewItem() {
            return new Panorama();
        }

        @Override
        protected Parcelable.Creator<Panorama> getMissionItemCreator() {
            return Panorama.CREATOR;
        }
    },

    WAIT("Wait") {
        @Override
        public MissionItem getNewItem() {
            return new Wait();
        }

        @Override
        protected Parcelable.Creator<Wait> getMissionItemCreator() {
            return Wait.CREATOR;
        }
    },

    YAW("Yaw") {
        @Override
        public MissionItem getNewItem() {
            return new Yaw();
        }

        @Override
        protected Parcelable.Creator<Yaw> getMissionItemCreator() {
            return Yaw.CREATOR;
        }
    },

    POINT_OF_INTEREST("Point of Interest") {
        @Override
        public MissionItem getNewItem() {
            return new PointOfInterest();
        }

        @Override
        protected Parcelable.Creator<PointOfInterest> getMissionItemCreator() {
            return PointOfInterest.CREATOR;
        }
    },

    MISSION_PAUSE("Pause mission") {
        @Override
        public MissionItem getNewItem() {
            return new MissionPause();
        }

        @Override
        protected Parcelable.Creator<MissionPause> getMissionItemCreator() {
            return MissionPause.CREATOR;
        }
    };

    private static final String EXTRA_MISSION_ITEM_TYPE = "extra_mission_item_type";
    private static final String EXTRA_MISSION_ITEM = "extra_mission_item";

    private final String label;

    MissionItemType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }

    @Override
    public String toString() {
        return getLabel();
    }

    public abstract MissionItem getNewItem();

    public final Bundle storeMissionItem(MissionItem item) {
        Bundle bundle = new Bundle(2);
        storeMissionItem(item, bundle);
        return bundle;
    }

    public void storeMissionItem(MissionItem missionItem, Bundle bundle) {
        bundle.putString(EXTRA_MISSION_ITEM_TYPE, name());
        bundle.putByteArray(EXTRA_MISSION_ITEM, ParcelableUtils.marshall(missionItem));
    }

    protected abstract <T extends MissionItem> Parcelable.Creator<T> getMissionItemCreator();

    public static <T extends MissionItem> T restoreMissionItemFromBundle(Bundle bundle) {
        if (bundle == null)
            return null;

        String typeName = bundle.getString(EXTRA_MISSION_ITEM_TYPE);
        byte[] marshalledItem = bundle.getByteArray(EXTRA_MISSION_ITEM);
        if (typeName == null || marshalledItem == null)
            return null;

        MissionItemType type = MissionItemType.valueOf(typeName);

        T missionItem = ParcelableUtils.unmarshall(marshalledItem, type.<T>getMissionItemCreator());
        return missionItem;
    }

    public static boolean isKindOfWaypoint(MissionItemType type) {
        return type == MissionItemType.WAYPOINT
            || type == MissionItemType.SPLINE_WAYPOINT
            || type == MissionItemType.LAND;
    }

    public static boolean isCameraActionType(MissionItemType type) {
        return type == CAMERA_SERIES_DISTANCE || type == CAMERA_SERIES_TIME || type == CAMERA_TRIGGER;
    }
}
