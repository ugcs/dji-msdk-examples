package com.example.ugcssample.drone.mission;

import com.example.ugcssample.drone.MsgIdAndSession;
import com.example.ugcssample.model.GuidedTarget;
import com.example.ugcssample.model.Mission;
import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.command.CameraAttitude;
import com.example.ugcssample.model.command.CameraMediaFileInfo;
import com.example.ugcssample.model.command.CameraSeriesTime;
import com.example.ugcssample.model.command.CameraZoom;
import com.example.ugcssample.model.command.ChangeSpeed;
import com.example.ugcssample.model.command.MissionPause;
import com.example.ugcssample.model.coordinate.LatLongAlt;
import com.example.ugcssample.model.utils.AppUtils;

import java.util.HashMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import dji.common.mission.waypoint.WaypointMission;

/**
 * Created by Dima on 22/09/2015.
 */
public class DjiMissionAndHome {

    public enum StopReason {
        PAUSE, STOP_AS_NEW_CNG, STOP_AS_NEW_MISSION, MANUAL, STOP_AS_SWITCH_GUIDED, STOP_AS_SWITCH_JOYSTICK
    }

    /**
     * */
    public MsgIdAndSession sourceMsgIdAndSession;

    // For missions
    public final Mission sourceMission;
    public final boolean missmionRestart;
    public final Integer suspendedAfterWpIndex;
    public final LatLongAlt suspendedAt;

    // Only for Guided Missions
    public final LatLongAlt guidedStartPoint;
    public final GuidedTarget guidedTarget;

    // Generation result
    public /*final*/ WaypointMission waypointMission;
    public boolean needSetHome = false;
    public boolean needSetReturnAlt = false;
    public CameraSeriesTime fastestCameraSeriesTime = null;
    public HashMap<Integer, OnWpActions> wpActions = null;

    // Possible route warnings
    public boolean simulatorRelocated = false;
    public boolean actionsIgnoredAsPathCurved = false;
    public SortedMap<Integer, SortedMap<Integer, Integer>> panorammaChangedOn = null;
    public Set<Integer> tooManyActionsOn = null;
    public boolean camActionsIgnoredAsNoCamera = false;
    public boolean camAttitudeRollYawIgnored = false;
    public boolean camByDistanceIgnored = false;
    public boolean droneDangerousMode = false;
    public boolean gimbalPitchAdopted = false;
    public boolean homeLocationIgnored = false;

    public StopReason stopRequested;

    /**
     * Constructor used to build Normal Mission
     * Main controller will only upload it to drone
     */
    public DjiMissionAndHome(MsgIdAndSession sourceMsgIdAndSession, Mission sourceMission, WaypointMission
        waypointMission) {
        this.sourceMsgIdAndSession = sourceMsgIdAndSession;
        this.sourceMission = sourceMission;
        this.waypointMission = waypointMission;
        this.missmionRestart = false;
        this.guidedTarget = null;
        this.guidedStartPoint = null;
        this.suspendedAfterWpIndex = null;
        this.suspendedAt = null;
    }

    /**
     * Constructor used to build Mission Restart
     * Main controller will upload it to drone and launch it
     */
    public DjiMissionAndHome(MsgIdAndSession sourceMsgIdAndSession, Mission sourceMission, WaypointMission
        waypointMission, boolean missmionRestart) {
        if (!missmionRestart) {
            throw new RuntimeException("Wrong usage, Constructor used to build Mission Restart only!");
        }
        this.sourceMsgIdAndSession = sourceMsgIdAndSession;
        this.sourceMission = sourceMission;
        this.waypointMission = waypointMission;
        this.missmionRestart = true;
        this.suspendedAfterWpIndex = null;
        this.suspendedAt = null;

        this.guidedTarget = null;
        this.guidedStartPoint = null;
    }

    /**
     * Constructor used to build Mission Resume
     * Main controller will upload it to drone and launch it
     */
    public DjiMissionAndHome(MsgIdAndSession sourceMsgIdAndSession, Mission sourceMission, WaypointMission
        waypointMission, int suspendedAfterWpIndex, LatLongAlt suspendedAt) {
        if (suspendedAfterWpIndex < 0 || suspendedAfterWpIndex >= sourceMission.getWaypointCount()
            || suspendedAt == null) {
            throw new RuntimeException("Wrong usage, Constructor used to build Mission Resume only!");
        }
        this.sourceMsgIdAndSession = sourceMsgIdAndSession;
        this.sourceMission = sourceMission;
        this.waypointMission = waypointMission;
        this.missmionRestart = false;
        this.suspendedAfterWpIndex = suspendedAfterWpIndex;
        this.suspendedAt = suspendedAt;

        this.guidedTarget = null;
        this.guidedStartPoint = null;
    }

    /**
     * Constructor used to build Guided task
     */
    public DjiMissionAndHome(MsgIdAndSession sourceMsgIdAndSession, GuidedTarget guidedTarget, LatLongAlt
        guidedStartPoint, WaypointMission waypointMission) {
        this.sourceMsgIdAndSession = sourceMsgIdAndSession;
        this.sourceMission = null;
        this.waypointMission = waypointMission;
        this.missmionRestart = false;
        this.guidedTarget = guidedTarget;
        this.guidedStartPoint = guidedStartPoint;
        this.suspendedAfterWpIndex = null;
        this.suspendedAt = null;
        /*if((sourceMission == null && guidedTarget == null)
                || (sourceMission != null && guidedTarget != null) ){
            throw new RuntimeException("DJI Task must contain mission or guidedTarget");
        }*/
    }

    public boolean isGuidedMission() {
        return guidedTarget != null;
    }

    public boolean isMissmionRestart() {
        return missmionRestart;
    }

    public boolean isMissmionResume() {
        return suspendedAfterWpIndex != null;
    }

    public void addOnWaypointAction(Integer index, MissionItem mi) {
        if (wpActions == null)
            wpActions = new HashMap<>();

        OnWpActions actions = wpActions.get(index);
        if (actions == null) {
            actions = new OnWpActions();
            wpActions.put(index, actions);
        }

        // MissionItem could be null as OnWpActions contains also flags to report WP readched/left
        if (mi == null) {
            return;
        }

        switch (mi.getType()) {
            case CAMERA_SERIES_TIME:
                actions.seriesByTime = (CameraSeriesTime)mi;
                break;
            case CHANGE_SPEED:
                actions.changeSpeed = (ChangeSpeed)mi;
                break;
            case CAMERA_ATTITUDE:
                actions.cameraAttitude = (CameraAttitude)mi;
                break;
            case CAMERA_ZOOM:
                actions.cameraZoom = (CameraZoom)mi;
                break;
            case CAMERA_MEDIA_FILE_INFO:
                actions.cameraMediaFileInfo = (CameraMediaFileInfo)mi;
                break;
            case MISSION_PAUSE:
                actions.missionPause = (MissionPause)mi;
                break;
            default:
                AppUtils.unhandledSwitch(mi.getType().name());
                break;
        }
    }

    public OnWpActions getWpActions(Integer index) {
        return wpActions == null ? null : wpActions.get(index);
    }

    public HashMap<Integer, OnWpActions> getWpActions() {
        return wpActions;
    }

    public void onTooManyActions(Integer wpIndex) {
        if (tooManyActionsOn == null)
            tooManyActionsOn = new TreeSet<>();

        tooManyActionsOn.add(wpIndex);
    }

    // WP #1: Panorama #1: angular step changed to 45 deg
    // WP #1: Panorama #2: angular step changed to 45 deg
    // WP #2: Panorama #1: angular step changed to 45 deg
    // WP #4: Panorama #1: angular step changed to 45 deg
    // to meet DJI limits.
    public void onPanorammaAdopted(Integer wpIndex, Integer panoramaIndex, Integer stepValue) {
        if (panorammaChangedOn == null)
            panorammaChangedOn = new TreeMap<>();

        SortedMap<Integer, Integer> onWp = panorammaChangedOn.get(wpIndex);
        if (onWp == null) {
            onWp = new TreeMap<>();
            panorammaChangedOn.put(wpIndex, onWp);
        }
        onWp.put(panoramaIndex, stepValue);
    }

}
