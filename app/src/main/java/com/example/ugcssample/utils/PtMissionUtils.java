package com.example.ugcssample.utils;

import com.example.ugcssample.model.GotoWaypointMode;
import com.example.ugcssample.model.HomeLocationSourceType;
import com.example.ugcssample.model.Mission;
import com.example.ugcssample.model.command.ChangeSpeed;
import com.example.ugcssample.model.command.Yaw;
import com.example.ugcssample.model.spatial.Waypoint;

import dji.common.mission.waypoint.WaypointMissionHeadingMode;

public final class PtMissionUtils {

    public static final String MISSION_TAG_ASCENDING = "MISSION_TAG_ASCENDING";
    public static final String MISSION_TAG_TO_TC = "MISSION_TAG_TO_TC";
    public static final String MISSION_TAG_TC_ADJUSTMENT = "MISSION_TAG_TC_ADJUSTMENT"; // TODO
    // refactor C&G to mission
    public static final String MISSION_TAG_TO_RADIUS = "MISSION_TAG_TO_RADIUS";
    public static final String MISSION_TAG_ORBIT = "MISSION_TAG_ORBIT";
    public static final String MISSION_TAG_COLUMNS = "MISSION_TAG_COLUMNS";
    public static final String MISSION_TAG_RTH = "MISSION_TAG_RTH";
    public static final String WP_INDEX_PAIR_SEPARATOR = "!";
    public static final String WP_INDEX_SEPARATOR = ":";

    public static final String ARROW_DOWN = "\u2193";
    public static final String ARROW_UP = "\u2191";
    public static final String MIDDLE_DOT = "\u00B7";
    public static final String MIDDLE_CIRCLE = "\u25CF";

    public static final double TC_ADJUSTMENT_MIN_DISTANCE = 0.6;

    private static final double ALLOW_RTH_IMMEDIATE_LAND_DISTANCE = 2.0;
    public static final double ORBIT_SURROUNDING_ANGLE = 390.0;

    public static final double PILOT_VIEW_SECTOR_ANGLE = 180.0;
    //public static final double PILOT_VIEW_SECTOR_ANGLE = 90.0;

    /**
     * Don't let anyone instantiate this class.
     */
    private PtMissionUtils() {
    }

    public static Mission ascentMission(double lat, double lng, double yaw, double speed) {

        double estimatedHeight = 50d;
        double safeAlt = estimatedHeight + 10d;
        double firstAlt = estimatedHeight / 3;
        Mission m = new Mission();
        m.transientAttributes.transientTag = MISSION_TAG_ASCENDING;
        m.transientAttributes.recoverWaypointDistanceTooClose = true;
        m.missionAttributes.setHomeLocationSourceType(HomeLocationSourceType.NONE);
        m.transientAttributes.forceReturnAltitudeChange = true;
        m.missionAttributes.gotoFirstWaypointMode = GotoWaypointMode.DIRECT;
        // let the user have yaw control
        m.transientAttributes.nativeHeadingMode =
                WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER.name();

        // ChangeSpeed (vertical)
        m.addMissionItem(new ChangeSpeed().setSpeed(speed));

        Waypoint wp;
        // WP1 - current position @ estimatedTowerHeightHalf < vehicleAlt ? vehicleAlt :
        // estimatedTowerHeightHalf
        wp = new Waypoint(lat, lng, firstAlt);
        m.addMissionItem(wp);
        m.addMissionItem(new Yaw().setAngle(yaw));

        // WP2
        wp = new Waypoint(lat, lng, estimatedHeight);
        m.addMissionItem(wp);
        return m;
    }

}
