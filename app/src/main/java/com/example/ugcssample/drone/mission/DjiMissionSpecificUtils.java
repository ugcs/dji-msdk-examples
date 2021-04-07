package com.example.ugcssample.drone.mission;

import android.content.Context;
import androidx.annotation.NonNull;

import com.example.ugcssample.drone.DjiTypeUtils;
import com.example.ugcssample.drone.ModelUtils;
import com.example.ugcssample.drone.MsgIdAndSession;
import com.example.ugcssample.model.EmergencyActionType;
import com.example.ugcssample.model.HomeLocationSourceType;
import com.example.ugcssample.model.Mission;
import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;
import com.example.ugcssample.model.command.CameraAttitude;
import com.example.ugcssample.model.command.CameraSeriesDistance;
import com.example.ugcssample.model.command.CameraSeriesTime;
import com.example.ugcssample.model.command.CameraTrigger;
import com.example.ugcssample.model.command.CameraZoom;
import com.example.ugcssample.model.command.ChangeSpeed;
import com.example.ugcssample.model.command.Panorama;
import com.example.ugcssample.model.command.Wait;
import com.example.ugcssample.model.command.Yaw;
import com.example.ugcssample.model.coordinate.LatLong;
import com.example.ugcssample.model.coordinate.LatLongAlt;
import com.example.ugcssample.model.dto.MinMaxInt;
import com.example.ugcssample.model.spatial.Land;
import com.example.ugcssample.model.spatial.PointOfInterest;
import com.example.ugcssample.model.spatial.SplineWaypoint;
import com.example.ugcssample.model.spatial.Waypoint;
import com.example.ugcssample.model.utils.AppUtils;
import com.example.ugcssample.model.utils.MathUtils;
import com.example.ugcssample.model.utils.MissionUtils;
import com.example.ugcssample.model.utils.unitsystem.UnitSystemManager;
import com.example.ugcssample.model.utils.unitsystem.UnitSystemType;
import com.example.ugcssample.model.utils.unitsystem.providers.length.LengthUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.systems.UnitSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dji.common.error.DJIError;
import dji.common.error.DJIMissionError;
import dji.common.flightcontroller.FlightMode;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointTurnMode;
import dji.common.model.LocationCoordinate2D;
import timber.log.Timber;

import static dji.common.mission.waypoint.WaypointMissionHeadingMode.TOWARD_POINT_OF_INTEREST;

/**
 * Created by Dima on 25/02/2016.
 */
public final class DjiMissionSpecificUtils {

    // to void dji error:
    // "The go home altitude is too low (lower than 20m)."
    public static final double MIN_RTH_ALTITUDE = 20.0d;


    /**
     * max distance between route first waypoint and aircraft takeoff position in case of start from the ground
     */
    public static final double MAX_DIST_TO_FIRST_WP = 30.0d;

    /**
     * max distance between adjacent waypoints (DJI autopilot limitation)
     */
    private static final double MAX_DIST = 2000.0d;

    /**
     * min distance between adjacent waypoints (DJI autopilot limitation)
     */
    public static final double MIN_DIST = 0.5d;

    public static final int MIN_WP_CNT = 2;
    public static final int MAX_WP_CNT = WaypointMission.MAX_WAYPOINT_COUNT;

    public static final int MIN_WAIT_MS = 1000;
    public static final int MAX_WAIT_MS = 32000;

    /**
     * Gimbal pitch rotation  value range is [-90, 0] degrees.
     */
    static final int MIN_GIMBAL_PITCH = -90;
    static final int MAX_GIMBAL_PITCH = 0;

    static final float DEFAULT_FLIGHT_SPEED = 5.0f;

    private DjiMissionSpecificUtils() {
        // Utility class
    }

    public static WaypointMission generateDjiTaskTest(LatLong currentPosition) {
        double mHomeLatitude = currentPosition.getLatitude();
        double mHomeLongitude = currentPosition.getLongitude();

        // Step 1: create mission
        WaypointMission.Builder waypointMission = new WaypointMission.Builder();
        waypointMission.maxFlightSpeed(14f);
        waypointMission.autoFlightSpeed(4f);
        waypointMission.setExitMissionOnRCSignalLostEnabled(false);
        waypointMission.finishedAction(WaypointMissionFinishedAction.NO_ACTION);
        waypointMission.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        waypointMission.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
        waypointMission.headingMode(WaypointMissionHeadingMode.AUTO);
        waypointMission.repeatTimes(1);

        // Step 2: create waypoints and prepare coordinates
        dji.common.mission.waypoint.Waypoint northPoint = new dji.common.mission.waypoint.Waypoint(mHomeLatitude + 10
            * MathUtils.ONE_METER_OFFSET, mHomeLongitude, 10f);
        dji.common.mission.waypoint.Waypoint eastPoint = new dji.common.mission.waypoint.Waypoint(mHomeLatitude,
            mHomeLongitude + 10 * MathUtils.calcLongitudeOffset(mHomeLatitude), 20f);
        dji.common.mission.waypoint.Waypoint southPoint = new dji.common.mission.waypoint.Waypoint(mHomeLatitude - 10
            * MathUtils.ONE_METER_OFFSET, mHomeLongitude, 30f);
        dji.common.mission.waypoint.Waypoint westPoint = new dji.common.mission.waypoint.Waypoint(mHomeLatitude,
            mHomeLongitude - 10 * MathUtils.calcLongitudeOffset(mHomeLatitude), 40f);

        //Step 3: add actions
        northPoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -60));
        northPoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
        eastPoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
        southPoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, 60));
        southPoint.addAction(new WaypointAction(WaypointActionType.START_RECORD, 0));
        westPoint.addAction(new WaypointAction(WaypointActionType.STOP_RECORD, 0));

        //Step 4: add waypoints into the mission
        waypointMission.addWaypoint(northPoint);
        waypointMission.addWaypoint(eastPoint);
        waypointMission.addWaypoint(southPoint);
        waypointMission.addWaypoint(westPoint);

        return waypointMission.build();
    }

    public static WaypointMission generateFakeMission(LatLong currentPosition, double altitude) {
        double mHomeLatitude = currentPosition.getLatitude();
        double mHomeLongitude = currentPosition.getLongitude();

        // Step 1: create mission
        WaypointMission.Builder waypointMission = new WaypointMission.Builder();
        waypointMission.maxFlightSpeed(14f);
        waypointMission.autoFlightSpeed(4f);
        waypointMission.setExitMissionOnRCSignalLostEnabled(false);
        waypointMission.finishedAction(WaypointMissionFinishedAction.NO_ACTION);
        waypointMission.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        waypointMission.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
        waypointMission.headingMode(WaypointMissionHeadingMode.AUTO);
        waypointMission.repeatTimes(1);

        // Step 2: create waypoints and prepare coordinates
        dji.common.mission.waypoint.Waypoint northPoint = new dji.common.mission.waypoint.Waypoint(mHomeLatitude + 10
            * MathUtils.ONE_METER_OFFSET, mHomeLongitude, (float)altitude);
        dji.common.mission.waypoint.Waypoint eastPoint = new dji.common.mission.waypoint.Waypoint(mHomeLatitude,
            mHomeLongitude + 10 * MathUtils.calcLongitudeOffset(mHomeLatitude), (float)altitude);

        //Step 4: add waypoints into the mission
        waypointMission.addWaypoint(northPoint);
        waypointMission.addWaypoint(eastPoint);

        return waypointMission.build();
    }

    public static DjiMissionAndHome generateDjiTaskForMissionRestart(MsgIdAndSession msgIdAndSession, Mission m,
                                                                     boolean hasCameraMounted) {
        DjiMissionAndHome temp = generateDjiTaskForMission(msgIdAndSession, m, hasCameraMounted);
        DjiMissionAndHome task = new DjiMissionAndHome(temp.sourceMsgIdAndSession, temp.sourceMission, temp
            .waypointMission, true);
        task.needSetHome = false;
        task.needSetReturnAlt = m.transientAttributes.forceReturnAltitudeChange;
        task.fastestCameraSeriesTime = null;
        task.wpActions = temp.wpActions;

        task.simulatorRelocated = false;
        task.actionsIgnoredAsPathCurved = temp.actionsIgnoredAsPathCurved;
        task.panorammaChangedOn = temp.panorammaChangedOn;
        task.tooManyActionsOn = temp.tooManyActionsOn;

        return task;
    }

    public static DjiMissionAndHome generateDjiTaskForMissionResume(MsgIdAndSession msgIdAndSession, Mission m, int
        suspendedAfterWpIndex, LatLongAlt missionSuspendedAt,
                                                                    boolean hasCameraMounted) {
        DjiMissionAndHome temp = generateDjiTaskForMission(msgIdAndSession, m, hasCameraMounted);
        WaypointMission waypointMission = DjiMissionSpecificUtils.subTask(temp.waypointMission,
            suspendedAfterWpIndex, missionSuspendedAt);
        DjiMissionAndHome task = new DjiMissionAndHome(temp.sourceMsgIdAndSession, temp.sourceMission,
            waypointMission, suspendedAfterWpIndex, missionSuspendedAt);
        task.needSetHome = false;
        task.needSetReturnAlt = false;
        task.fastestCameraSeriesTime = null;
        task.wpActions = temp.wpActions;

        task.simulatorRelocated = false;
        task.actionsIgnoredAsPathCurved = temp.actionsIgnoredAsPathCurved;
        task.panorammaChangedOn = temp.panorammaChangedOn;
        task.tooManyActionsOn = temp.tooManyActionsOn;

        return task;
    }

    public static String validateReceivedMission(Mission m, Context context) {
        boolean hasYaw = false;
        boolean hasPoi = false;
        for (MissionItem mi : m.getMissionItems()) {
            switch (mi.getType()) {
                case POINT_OF_INTEREST:
                    hasPoi = true;
                    break;
                case YAW:
                    hasYaw = true;
                    break;
                case WAIT:
                    Wait wait = (Wait)mi;
                    if (MIN_WAIT_MS > wait.getTime() || MAX_WAIT_MS < wait.getTime()) {
                        return "context.getString(R.string.WAYPOINT_WAIT_ERROR, MIN_WAIT_MS / 1000, MAX_WAIT_MS / 1000)";
                    }
                default:
                    break;
            }
        }

        if (hasPoi && hasYaw) {
            return "context.getString(R.string.WAYPOINT_YAW_POI_ERROR)";
        }

        return null;
    }

    /**
     * WaypointMissionFlightPathMode (Default NORMAL)
     * NORMAL - The flight path will be normal and the aircraft will move from one waypoint to the next in straight
     * lines.
     * <p>
     * CURVED - The flight path will be curved and the aircraft will move from one waypoint to the next in a curved
     * motion, adhering to the cornerRadiusInMeters, which is set in Waypoint.
     * <p><p>
     * WaypointMissionHeadingMode (Default AUTO):
     * AUTO - Aircraft's heading will always be in the direction of flight.
     * <p>
     * USING_INITIAL_DIRECTION - Aircraft's heading will be set to the heading when reaching the first waypoint.
     * Before reaching the first waypoint, the aircraft's heading can be controlled by the remote controller.
     * When the aircraft reaches the first waypoint, its heading will be fixed.
     * <p>
     * CONTROL_BY_REMOTE_CONTROLLER - Aircraft's heading will be controlled by the remote controller.
     * <p>
     * USING_WAYPOINT_HEADING - Aircraft's heading will be gradually set to the next waypoint heading while
     * travelling between two adjacent waypoints.
     * <p>
     * TOWARD_POINT_OF_INTEREST - Aircraft's heading will always toward point of interest.
     */

    public static DjiMissionAndHome generateDjiTaskForMission(MsgIdAndSession msgIdAndSession, Mission m,
                                                              boolean hasCameraMounted) {

        MissionUtils.clearVerticalWaypoints(m, MIN_DIST);

        //WaypointMissionData mTask = prepareTestMission(dm.position.getGps().getLatLong());
        WaypointMission.Builder mTask = newWaypointMissionBuilder();

        //mTask.pointOfInterestLatitude = 0.0d;
        //mTask.pointOfInterestLongitude = 0.0d;
        DjiMissionAndHome missionAndHome = new DjiMissionAndHome(msgIdAndSession, m, null);
        missionAndHome.sourceMsgIdAndSession = msgIdAndSession;
        missionAndHome.needSetHome = m.missionAttributes.getHomeLocationSourceType() != HomeLocationSourceType.NONE;
        missionAndHome.needSetReturnAlt = m.missionAttributes.returnAltitude >= 20.0;

        setGotoFirstWaypointMode(mTask, m);
        setExitMissionOnRCSignalLostEnabled(mTask, m);
        setAutoFlightSpeed(mTask, m);

        // All commands:
        /*WAYPOINT, SPLINE_WAYPOINT, LAND, CIRCLE,
        TAKEOFF, CHANGE_SPEED, RETURN_TO_HOME, WAIT, YAW
        CAMERA_TRIGGER, CAMERA_SERIES_TIME, CAMERA_SERIES_DISTANCE, CAMERA_ATTITUDE, PANORAMA
        POINT_OF_INTEREST*/

        //Step 3: Lets extract all WP with coordinates
        // As a side operation, we will process all not WP dependent actions, like
        // land or RTH
        for (MissionItem mi : m.getMissionItems()) {
            MissionItemType type = mi.getType();

            switch (type) {
                case WAYPOINT:
                    Waypoint wp = (Waypoint)mi;
                    mTask.addWaypoint(newDjiWaypoint(wp.getLat(), wp.getLon(), (float)wp.getAlt()));
                    break;
                case SPLINE_WAYPOINT:
                    SplineWaypoint spwp = (SplineWaypoint)mi;
                    dji.common.mission.waypoint.Waypoint spDjiWp = newDjiWaypoint(
                        spwp.getLat(), spwp.getLon(), (float)spwp.getAlt());
                    spDjiWp.cornerRadiusInMeters = 5;
                    mTask.addWaypoint(spDjiWp);
                    mTask.flightPathMode(WaypointMissionFlightPathMode.CURVED);
                    break;
                case LAND:
                    Land land = (Land)mi;
                    mTask.addWaypoint(newDjiWaypoint(land.getLat(), land.getLon(), (float)land.getAlt()));
                    mTask.finishedAction(WaypointMissionFinishedAction.AUTO_LAND);
                    break;
                case RETURN_TO_HOME:
                    mTask.finishedAction(WaypointMissionFinishedAction.GO_HOME);
                    break;
                case TAKEOFF:
                    // Nothing to do
                    break;
                default:
                    // We are not suppose to handle all MissionItem here
                    break;
            }
        }

        updateHeading(mTask);

        //Step 4: Process all rest
        float lastKnownSpeed = mTask.getAutoFlightSpeed();
        int wpIndex = -1;
        int panIndexInCurrentWp = 0;
        dji.common.mission.waypoint.Waypoint currentWp = null;
        PointOfInterest currentPoi = null; // = MissionUtils.findFirstPointOfInterest(m);

        CameraSeriesTime fastestCameraSeriesTime = null;

        for (MissionItem mi : m.getMissionItems()) {
            MissionItemType type = mi.getType();
            switch (type) {
                case TAKEOFF:
                case RETURN_TO_HOME:
                    // Already processed
                    break;

                case CIRCLE:
                    Timber.w("CIRCLE is not supported mission item");
                    break;

                case WAYPOINT:
                case SPLINE_WAYPOINT:
                case LAND:
                    wpIndex++;
                    panIndexInCurrentWp = 0;
                    currentWp = mTask.getWaypointList().get(wpIndex);
                    missionAndHome.addOnWaypointAction(wpIndex, null);
                    // for FC fw > 3.2.10.0 could be set directly in mission
                    if (ModelUtils.useNativeChangeSpeed()) {
                        // From docs: speed - The base automatic speed of the aircraft as it moves between
                        // this waypoint and the next waypoint with range [0, 15] m/s.
                        currentWp.speed = lastKnownSpeed;
                    }
                    if (currentPoi != null) {
                        mTask.headingMode(WaypointMissionHeadingMode.USING_WAYPOINT_HEADING);
                        double heading = MathUtils.getHeadingFromCoordinates180(new LatLong(currentWp.coordinate
                            .getLatitude(), currentWp.coordinate.getLongitude()), currentPoi.getCoordinate());
                        currentWp.heading = (short)heading;
                    }
                    break;
                case WAIT:
                    if (currentWp != null) {
                        Wait wait = (Wait)mi;
                        // For Way_Point_Action_Staty, the parameter means how long it will stay and have no action,
                        // the unit is ms;
                        addAtionToWp(missionAndHome, currentWp, wpIndex, WaypointActionType.STAY, wait.getTime());
                    } else {
                        Timber.e("Got Wait, but NO waypoint");
                    }
                    break;

                case YAW:
                    if (currentWp != null) {
                        Yaw yaw = (Yaw)mi;
                        // yaw angle must be in a range [-180 .. 180]
                        addAtionToWp(missionAndHome, currentWp, wpIndex, WaypointActionType.ROTATE_AIRCRAFT,
                            (int)MathUtils.toPlusMinus180(yaw.getAngle()));
                        mTask.headingMode(WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER);
                    }
                    break;
                case POINT_OF_INTEREST:
                    PointOfInterest roi = (PointOfInterest)mi;

                    switch (roi.getMode()) {
                        case LOCATION:
                            currentPoi = roi;
                            break;
                        case NONE:
                            currentPoi = null;
                            break;
                        default:
                            throw new RuntimeException(AppUtils.UNHANDLED_SWITCH);
                    }

                    if (currentPoi != null && currentWp != null) {
                        mTask.headingMode(WaypointMissionHeadingMode.USING_WAYPOINT_HEADING);
                        double heading = MathUtils.getHeadingFromCoordinates180(new LatLong(currentWp.coordinate
                            .getLatitude(), currentWp.coordinate.getLongitude()), currentPoi.getCoordinate());
                        currentWp.heading = (short)heading;
                    }

                    if (currentPoi != null
                        && m.transientAttributes.nativeHeadingMode != null
                        && m.transientAttributes.nativeHeadingMode.equals(TOWARD_POINT_OF_INTEREST.name())) {
                        mTask.setPointOfInterest(ModelUtils.toDjiLocation(currentPoi.getCoordinate()));
                    }

                    break;

                case CAMERA_ATTITUDE:
                    if (currentWp != null) {
                        CameraAttitude ca = (CameraAttitude)mi;
                        if (m.transientAttributes.forceOnWpActionForCameraAttitude) {
                            missionAndHome.addOnWaypointAction(wpIndex, mi);
                        } else {
                            boolean pitchAvailable = ca.isPitchAvailable();
                            if (pitchAvailable) {
                                validateAndAddGimbalPitch(ca.getPitch(), missionAndHome, currentWp, wpIndex);
                            }

                            if (ca.isYawAvailable() || ca.isRollAvailable()) {
                                missionAndHome.camAttitudeRollYawIgnored = true;
                            }

                        }

                        if (ca.isZoomAvailable()) {
                            missionAndHome.addOnWaypointAction(wpIndex, new CameraZoom().setZoom(ca.getZoom()));
                        }

                    } else {
                        Timber.e("Got CAMERA_ATTITUDE, but NO waypoint");
                    }
                    break;

                case CAMERA_TRIGGER:
                    if (currentWp != null) {
                        CameraTrigger ct = (CameraTrigger)mi;
                        if (ct.getMode() == CameraTrigger.CameraTriggerModeType.SINGLE_SHOT) {
                            checkIfNeedToStopVideoAndStop(missionAndHome, mTask.getWaypointList(), currentWp, wpIndex);
                            addAtionToWp(missionAndHome, currentWp, wpIndex, WaypointActionType.START_TAKE_PHOTO, 0);
                        } else if (ct.getMode() == CameraTrigger.CameraTriggerModeType.START_RECORDING) {
                            checkIfNeedToStopVideoAndStop(missionAndHome, mTask.getWaypointList(), currentWp, wpIndex);
                            addAtionToWp(missionAndHome, currentWp, wpIndex, WaypointActionType.START_RECORD, 0);
                        } else if (ct.getMode() == CameraTrigger.CameraTriggerModeType.STOP_RECORDING)
                            addAtionToWp(missionAndHome, currentWp, wpIndex, WaypointActionType.STOP_RECORD, 0);
                        else Timber.e("unknown camera trigger state");
                    } else {
                        Timber.e("Got CAMERA_TRIGGER, but NO waypoint");
                    }
                    break;
                case CAMERA_SERIES_DISTANCE:
                    if (currentWp != null) {
                        checkIfNeedToStopVideoAndStop(missionAndHome, mTask.getWaypointList(), currentWp, wpIndex);
                        // for FC fw > 3.2.10.0 could be set directly in mission
                        boolean nativeCamSeries = ModelUtils.useNativePhotoByDistance();
                        if (nativeCamSeries) {
                            // From docs: shootPhotoDistanceInterval - The distance interval in meters when two
                            // photos are
                            // taken as the aircraft moves between the current waypoint and the next waypoint
                            CameraSeriesDistance serDst = (CameraSeriesDistance)mi;
                            currentWp.shootPhotoDistanceInterval = (float)serDst.getDistance();
                        } else {
                            missionAndHome.camByDistanceIgnored = true;
                        }
                    }
                    break;
                case CAMERA_SERIES_TIME:
                    if (currentWp != null) {
                        // for FC fw > 3.2.10.0 could be set directly in mission
                        boolean nativeCamSeries = ModelUtils.useNativePhotoByTime();
                        if (nativeCamSeries || hasCameraMounted) {
                            checkIfNeedToStopVideoAndStop(missionAndHome, mTask.getWaypointList(), currentWp, wpIndex);
                            CameraSeriesTime serTm = (CameraSeriesTime)mi;
                            if (nativeCamSeries && !serTm.transientAtrForceOfBoard) {
                                // From docs: shootPhotoTimeInterval - The time interval in seconds when two photos are
                                // taken as the aircraft moves between the current waypoint and the next waypoint.
                                currentWp.shootPhotoTimeInterval = (float)serTm.getRoundedIntervalInSeconds();
                            } else {
                                missionAndHome.addOnWaypointAction(wpIndex, mi);
                            }
                            if (fastestCameraSeriesTime == null
                                || fastestCameraSeriesTime.getInterval() > serTm.getInterval()) {
                                fastestCameraSeriesTime = serTm;
                            }
                        } else {
                            missionAndHome.camActionsIgnoredAsNoCamera = true;
                        }
                    }
                    break;
                case CAMERA_MEDIA_FILE_INFO:
                    missionAndHome.addOnWaypointAction(wpIndex, mi);
                    break;
                case MISSION_PAUSE:
                    missionAndHome.addOnWaypointAction(wpIndex, mi);
                    break;
                case PANORAMA:
                    if (currentWp != null) {
                        checkIfNeedToStopVideoAndStop(missionAndHome, mTask.getWaypointList(), currentWp, wpIndex);
                        Panorama p = (Panorama)mi;
                        ArrayList<WaypointAction> actions = createPanoramaActions(p, currentWp, null);
                        if (actions.size() > 15) {
                            int deg = (int)p.getAngle();  // pan. angle [-360, 360], positive = clockwise
                            int step = (int)p.getStep(); //step angle, zero stands for a continuous rotation.
                            Timber.w(String.format(Locale.US, "Panorama on wp #%2d has too many actions - %2d ",
                                wpIndex, actions.size()));

                            while (step <= 170 && actions.size() > 15) {
                                step = step + 5;
                                Timber.w(String.format(Locale.US, "Increasing step = %2d ", step));
                                actions = createPanoramaActions(p, currentWp, step);
                                Timber.w(String.format(Locale.US, "Got %2d actions ", actions.size()));
                                missionAndHome.onPanorammaAdopted(wpIndex, panIndexInCurrentWp, step);
                            }
                        }
                        for (WaypointAction action : actions) {
                            addAtionToWp(missionAndHome, currentWp, wpIndex, action.actionType, action.actionParam);
                        }
                        panIndexInCurrentWp++;
                    } else {
                        Timber.e("Got PANORAMA, but NO waypoint");
                    }
                    break;
                case CHANGE_SPEED:
                    if (currentWp != null) {
                        // for FC fw > 3.2.10.0 could be set directly in mission
                        if (ModelUtils.useNativeChangeSpeed()) {
                            // From docs: speed - The base automatic speed of the aircraft as it moves between
                            // this waypoint and the next waypoint with range [0, 15] m/s.
                            ChangeSpeed chSpd = (ChangeSpeed)mi;
                            currentWp.speed = (float)chSpd.getSpeed();
                            lastKnownSpeed = currentWp.speed;
                        } else {
                            // yes, current WP !
                            // as speed action will be performed after reaching this (current) WP,
                            // and drone will fly with new speed to NEXT WP
                            missionAndHome.addOnWaypointAction(wpIndex, mi);
                        }
                    }
                    break;
                default:
                    Timber.e("Unsupported mission item " + type.toString());
                    break;
            }
        }
        missionAndHome.fastestCameraSeriesTime = fastestCameraSeriesTime;

        // Post actions - add YAW actions if
        addYawActions(mTask, missionAndHome);

        // Post actions - detect turn mode based on WP's heading
        for (int i = 0; i < (mTask.getWaypointList().size() - 1); i++) {
            dji.common.mission.waypoint.Waypoint current = mTask.getWaypointList().get(i);
            dji.common.mission.waypoint.Waypoint next = mTask.getWaypointList().get(i + 1);

            boolean cw = MathUtils.cwOrCcw180(current.heading, next.heading);
            current.turnMode = cw ? WaypointTurnMode.CLOCKWISE : WaypointTurnMode.COUNTER_CLOCKWISE;
        }
        dji.common.mission.waypoint.Waypoint last = mTask.getWaypointList().get(mTask.getWaypointList().size() - 1);
        last.turnMode = WaypointTurnMode.CLOCKWISE;

        // Post actions - fix corner radius values. i.e. now all SPWP's has maxCR
        if (mTask.getFlightPathMode() == WaypointMissionFlightPathMode.CURVED) {
            missionAndHome.actionsIgnoredAsPathCurved = hasAnyActions(mTask);
            mTask.getWaypointList().get(0).cornerRadiusInMeters = 0.2F;
            mTask.getWaypointList().get(mTask.getWaypointCount() - 1).cornerRadiusInMeters = 0.2F;
            for (int i = 1; i < mTask.getWaypointCount() - 1; i++) {
                dji.common.mission.waypoint.Waypoint prev = mTask.getWaypointList().get(i - 1);
                dji.common.mission.waypoint.Waypoint current = mTask.getWaypointList().get(i);
                dji.common.mission.waypoint.Waypoint next = mTask.getWaypointList().get(i + 1);

                double distA = MathUtils.getDistance2D(new LatLong(prev.coordinate.getLatitude(), prev.coordinate
                    .getLongitude()), new LatLong(current.coordinate.getLatitude(), current.coordinate.getLongitude()));

                double distB = MathUtils.getDistance2D(new LatLong(current.coordinate.getLatitude(), current
                    .coordinate.getLongitude()), new LatLong(next.coordinate.getLatitude(), next.coordinate
                    .getLongitude()));

                distA = (distA - 0.2d) / 2;
                distB = (distB - 0.2d) / 2;
                float min = (float)(distA < distB ? distA : distB);
                if (min < 0.2F) min = 0.2F;
                if (min < current.cornerRadiusInMeters) current.cornerRadiusInMeters = min;
            }
        }

        if (m.transientAttributes.nativeHeadingMode != null) {
            try {
                WaypointMissionHeadingMode hm = WaypointMissionHeadingMode.valueOf(m.transientAttributes
                    .nativeHeadingMode);
                if (hm != null) {
                    mTask.headingMode(hm);
                }
            } catch (Exception e) {
                Timber.w("Unsupported nativeHeadingMode = " + m.transientAttributes.nativeHeadingMode);
            }
        }

        //if (!FlightMode.GPS_ATTI.toString().equals(dm.missionInfo.droneControlModeNativeName)) {
        //    Timber.w("NOT IN FlightMode.GPS_ATTI");
        //    missionAndHome.droneDangerousMode = true;
        //}

        DJIError e = mTask.checkParameters();
        if (DJIMissionError.WAYPOINT_DISTANCE_TOO_CLOSE == e && m.transientAttributes.recoverWaypointDistanceTooClose) {
            recoverWaypointDistanceTooClose(mTask);
        }
        missionAndHome.waypointMission = mTask.build();
        return missionAndHome;
    }

    public static void recoverWaypointDistanceTooClose(WaypointMission.Builder mTask) {
        final int cnt = mTask.getWaypointList().size();
        dji.common.mission.waypoint.Waypoint wp;
        dji.common.mission.waypoint.Waypoint nextWp;
        for (int i = 0; i < cnt - 1; i++) {
            wp = mTask.getWaypointList().get(i);
            nextWp = mTask.getWaypointList().get(i + 1);
            recoverWaypointDistanceTooClose(wp, nextWp);
        }
        //Last And First
        wp = mTask.getWaypointList().get(0);
        nextWp = mTask.getWaypointList().get(cnt - 1);
        recoverWaypointDistanceTooClose(wp, nextWp);

        DJIError e = mTask.checkParameters();
        if (DJIMissionError.WAYPOINT_DISTANCE_TOO_CLOSE == e) {
            nextWp.altitude = nextWp.altitude + (float)MIN_DIST;
        }
    }

    private static void recoverWaypointDistanceTooClose(dji.common.mission.waypoint.Waypoint wp,
                                                        dji.common.mission.waypoint.Waypoint nextWp) {
        WaypointMission.Builder tempMission = new WaypointMission.Builder();
        tempMission.maxFlightSpeed(14f);
        tempMission.autoFlightSpeed(2f);
        tempMission.setExitMissionOnRCSignalLostEnabled(false);
        tempMission.finishedAction(WaypointMissionFinishedAction.NO_ACTION);
        tempMission.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        tempMission.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
        tempMission.headingMode(WaypointMissionHeadingMode.USING_WAYPOINT_HEADING);
        tempMission.repeatTimes(1);

        tempMission.addWaypoint(wp);
        tempMission.addWaypoint(nextWp);
        DJIError e = tempMission.checkParameters();
        if (DJIMissionError.WAYPOINT_DISTANCE_TOO_CLOSE == e) {
            nextWp.altitude = nextWp.altitude + (float)MIN_DIST;
        }
    }

    private static boolean hasAnyActions(WaypointMission.Builder mTask) {
        for (dji.common.mission.waypoint.Waypoint wp : mTask.getWaypointList()) {
            if (hasAnyActions(wp))
                return true;
        }
        return false;
    }

    private static boolean hasAnyActions(dji.common.mission.waypoint.Waypoint wp) {
        return wp != null && wp.waypointActions != null && !wp.waypointActions.isEmpty();
    }

    private static boolean hasYawActions(dji.common.mission.waypoint.Waypoint wp) {
        if (wp != null && wp.waypointActions != null && !wp.waypointActions.isEmpty()) {
            for (WaypointAction action : wp.waypointActions) {
                if (action.actionType == WaypointActionType.ROTATE_AIRCRAFT)
                    return true;
            }
        }

        return false;
    }

    private static void updateHeading(WaypointMission.Builder mTask) {
        List<dji.common.mission.waypoint.Waypoint> wpList = mTask.getWaypointList();
        int length = wpList.size();
        dji.common.mission.waypoint.Waypoint wp;
        dji.common.mission.waypoint.Waypoint nextWp;
        int lastKnownHeading = 0;
        for (int i = 0; i < length; i++) {
            wp = wpList.get(i);
            nextWp = i < length - 1 ? wpList.get(i + 1) : null;
            if (nextWp != null) {
                lastKnownHeading = (int)MathUtils.getHeadingFromCoordinates180(
                    DjiTypeUtils.toLatLong(wp.coordinate),
                    DjiTypeUtils.toLatLong(nextWp.coordinate));
            }
            wp.heading = lastKnownHeading;
        }
    }

    private static WaypointMission.Builder newWaypointMissionBuilder() {
        return new WaypointMission.Builder().headingMode(WaypointMissionHeadingMode.AUTO)
            .maxFlightSpeed(WaypointMission.MAX_FLIGHT_SPEED)
            .flightPathMode(WaypointMissionFlightPathMode.NORMAL)
            .finishedAction(WaypointMissionFinishedAction.NO_ACTION)
            .autoFlightSpeed(DEFAULT_FLIGHT_SPEED);
    }

    private static void setGotoFirstWaypointMode(WaypointMission.Builder mTask, Mission m) {
        switch (m.missionAttributes.gotoFirstWaypointMode) {
            case DIRECT:
                mTask.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.POINT_TO_POINT);
                break;
            case SAFE:
                mTask.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
                break;
            default:
                mTask.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
                break;
        }
    }

    private static void setExitMissionOnRCSignalLostEnabled(WaypointMission.Builder mTask, Mission m) {
        EmergencyActionType onRc = m.missionAttributes.rcLostAction;
        mTask.setExitMissionOnRCSignalLostEnabled(onRc != EmergencyActionType.CONTINUE);
    }

    // Detect default flight speed
    private static void setAutoFlightSpeed(WaypointMission.Builder mTask, Mission m) {
        Double firstChangeSpeed = MissionUtils.findFirstChangeSpeed(m);
        if (firstChangeSpeed != null) {
            float defaultSpd = (float)firstChangeSpeed.doubleValue();
            Double spdAfterTakeoff = MissionUtils.findChangeSpeedJustAfterTakeoff(m);
            if (spdAfterTakeoff != null) {
                defaultSpd = (float)spdAfterTakeoff.doubleValue();
            }
            mTask.autoFlightSpeed(defaultSpd > WaypointMission.MAX_FLIGHT_SPEED
                ? WaypointMission.MAX_FLIGHT_SPEED : defaultSpd);
        }
    }

    private static dji.common.mission.waypoint.Waypoint newDjiWaypoint(double lat, double lon, float alt) {
        dji.common.mission.waypoint.Waypoint wp = new dji.common.mission.waypoint.Waypoint(lat, lon, alt);
        wp.heading = 0;
        wp.speed = 0.0F;
        wp.shootPhotoTimeInterval = 0.0F;
        wp.shootPhotoDistanceInterval = 0.0F;
        return wp;
    }

    private static void addYawActions(WaypointMission.Builder mTask, DjiMissionAndHome missionAndHome) {
        if (missionAndHome.sourceMission == null || !MissionUtils.hasYaw(missionAndHome.sourceMission))
            return;
        int wpIndex = -1;
        int wpCnt = mTask.getWaypointList().size();
        for (dji.common.mission.waypoint.Waypoint currentWp : mTask.getWaypointList()) {
            wpIndex++;
            if (!hasYawActions(currentWp) && wpIndex < wpCnt - 1) {
                addAtionToWp(missionAndHome, currentWp, wpIndex, WaypointActionType.ROTATE_AIRCRAFT,
                    MathUtils.toPlusMinus180(currentWp.heading));
            }
        }
    }

    /*private static void calculateHeading(dji.common.mission.waypoint.Waypoint djiWp,
                                         List<dji.common.mission.waypoint.Waypoint> djiWpList, LatLong myPos) {
        djiWp.heading = 0;
        LatLong currWpLl = new LatLong(djiWp.coordinate.getLatitude(), djiWp.coordinate.getLongitude());
        LatLong prevWpLl = null;
        if (djiWpList.size() > 0) {
            dji.common.mission.waypoint.Waypoint prevWayPoint = djiWpList.get(djiWpList.size() - 1);
            prevWpLl = new LatLong(prevWayPoint.coordinate.getLatitude(), prevWayPoint.coordinate.getLongitude());
        } else {
            prevWpLl = myPos;
        }
        if (prevWpLl != null) {
            double heading = MathUtils.getHeadingFromCoordinates180(prevWpLl, currWpLl);
            djiWp.heading = (int)heading;
        }
    }*/

    private static void validateAndAddGimbalPitch(double gimbalPitch,DjiMissionAndHome task,
                                                  dji.common.mission.waypoint.Waypoint currentWp, int wpIndex) {
        MinMaxInt pitchCapability = new MinMaxInt(-90, 0);
        gimbalPitch = MathUtils.toPlusMinus180(gimbalPitch);

        if (gimbalPitch > MAX_GIMBAL_PITCH) {
            Timber.w(String.format(Locale.US, "CAMERA_ATTITUDE - unsupported tilt : %2d", (int)-gimbalPitch));
            gimbalPitch = MAX_GIMBAL_PITCH;
            task.gimbalPitchAdopted = true;
        }

        double minPitch = pitchCapability == null ? MIN_GIMBAL_PITCH : pitchCapability.min;
        if (gimbalPitch < minPitch) {
            Timber.w(String.format(Locale.US, "CAMERA_ATTITUDE - unsupported tilt : %2d", (int)-gimbalPitch));
            gimbalPitch = minPitch;
            task.gimbalPitchAdopted = true;
        }

        addAtionToWp(task, currentWp, wpIndex, WaypointActionType.GIMBAL_PITCH, (int)gimbalPitch);

    }

    // FIXME SDK 4.5.1 - rename
    private static void addAtionToWp(DjiMissionAndHome missionAndHome, dji.common.mission.waypoint.Waypoint
        currentWp, int wpIndex, WaypointActionType action, int val) {
        if (currentWp.waypointActions.size() < 15) {
            currentWp.addAction(new WaypointAction(action, val));
        } else {
            missionAndHome.onTooManyActions(wpIndex);
        }
    }

    private static void checkIfNeedToStopVideoAndStop(DjiMissionAndHome missionAndHome, List<dji.common.mission.waypoint.Waypoint> waypointList, dji.common.mission.waypoint.Waypoint currentWp, int wpIndex) {
        if (wpIndex < 1) return;

        for (WaypointAction one : currentWp.waypointActions) {
            if (one.actionType == WaypointActionType.STOP_RECORD) {
                return;
            }
        }

        dji.common.mission.waypoint.Waypoint prevWp = waypointList.get(wpIndex - 1);
        if (!hasAnyActions(prevWp))
            return;

        boolean hasStartVideoAsLastAction = false;
        for (WaypointAction one : prevWp.waypointActions) {
            if (one.actionType == WaypointActionType.START_RECORD) {
                hasStartVideoAsLastAction = true;
            } else if (one.actionType == WaypointActionType.STOP_RECORD) {
                hasStartVideoAsLastAction = false;
            }
        }

        if (hasStartVideoAsLastAction) {
            addAtionToWp(missionAndHome, currentWp, wpIndex, WaypointActionType.STOP_RECORD, 0);
        }

    }

    /*public static String validateReceiedMission(WaypointMission m, Context context,
                                                BaseAppPrefs applicationPreferences) {

    }*/

    public static String validateReceivedMission(WaypointMission m, Context context) {
        dji.common.mission.waypoint.Waypoint wp;
        dji.common.mission.waypoint.Waypoint wpNext; // after 1st loop, here will be last WP
        List<dji.common.mission.waypoint.Waypoint> wpl = m.getWaypointList();

        UnitSystem unitSystem = UnitSystemManager.getUnitSystem(UnitSystemType.METRIC);

        if (wpl.size() < MIN_WP_CNT) {
            return "context.getString(R.string.dji_shared_sys_msg_mission_minimal_wp)";
        }

        double distToNext2d = 0d;
        double distToNext3d = 0d;
        double distToNextVert = 0d;

        for (int i = 0; i < wpl.size() - 1; i++) {
            wp = wpl.get(i);
            wpNext = wpl.get(i + 1);
            distToNext2d = MathUtils.getDistance2D(new LatLong(wp.coordinate.getLatitude(), wp.coordinate
                .getLongitude()), new LatLong(wpNext.coordinate.getLatitude(), wpNext.coordinate.getLongitude()));
            distToNext3d = MathUtils.getDistance3D(new LatLongAlt(wp.coordinate.getLatitude(), wp.coordinate
                .getLongitude(), wp.altitude), new LatLongAlt(wpNext.coordinate.getLatitude(), wpNext.coordinate
                .getLongitude(), wpNext.altitude));
            distToNextVert = wpNext.altitude - wp.altitude;
            if (distToNext2d >= MAX_DIST) {
                LengthUnitProvider lp = unitSystem.getLengthUnitProvider();
                return "error";
            }

            if (!isValidAdjacentMinDistance(distToNext2d, distToNextVert)) {
                LengthUnitProvider lp = unitSystem.getLengthUnitProvider();
                return "error";
            }
        }

        // First and last
        wp = wpl.get(0);
        wpNext = wpl.get(wpl.size() - 1);
        distToNext2d = MathUtils.getDistance2D(new LatLong(wp.coordinate.getLatitude(), wp.coordinate.getLongitude()),
            new LatLong(wpNext.coordinate.getLatitude(), wpNext.coordinate.getLongitude()));
        distToNext3d = MathUtils.getDistance3D(
            new LatLongAlt(wp.coordinate.getLatitude(), wp.coordinate.getLongitude(), wp.altitude),
            new LatLongAlt(wpNext.coordinate.getLatitude(), wpNext.coordinate.getLongitude(), wpNext.altitude));
        distToNextVert = wpNext.altitude - wp.altitude;
        if (distToNext2d >= MAX_DIST) {
            LengthUnitProvider lp = unitSystem.getLengthUnitProvider();
            return "error";
        }

        return null;
    }

    public static boolean isValidAdjacentMinDistance(LatLongAlt llaFrom, LatLongAlt llaTo) {
        double dist = MathUtils.getDistance2D(llaFrom, llaTo);
        double vert = llaFrom.getAltitude() - llaTo.getAltitude();
        return isValidAdjacentMinDistance(dist, vert);
    }

    public static boolean isValidAdjacentMinDistance(double distToNext2d, double distToNextVert) {
        return !(distToNext2d < MIN_DIST && Math.abs(distToNextVert) < MIN_DIST);
    }

    public static String logDjiWaypointMission(@NonNull Context context,
                                               @NonNull DjiMissionAndHome task) {

        return "";
    }

    private static WaypointMission subTask(WaypointMission task, Integer suspendedAfterWpIndex, LatLongAlt pausedOn) {
        WaypointMission.Builder subTask = new WaypointMission.Builder();
        //int wayPointCount = 0;
        //int startWaypointIndex = 0;
        subTask.repeatTimes(task.getRepeatTimes());
        subTask.gotoFirstWaypointMode(task.getGotoFirstWaypointMode());
        subTask.setExitMissionOnRCSignalLostEnabled(task.isExitMissionOnRCSignalLostEnabled());
        subTask.setPointOfInterest(task.getPointOfInterest());
        subTask.setGimbalPitchRotationEnabled(task.isGimbalPitchRotationEnabled());
        subTask.headingMode(task.getHeadingMode());
        subTask.finishedAction(task.getFinishedAction());
        subTask.flightPathMode(task.getFlightPathMode());
        subTask.maxFlightSpeed(task.getMaxFlightSpeed());
        subTask.autoFlightSpeed(task.getAutoFlightSpeed());

        dji.common.mission.waypoint.Waypoint wp = new  dji.common.mission.waypoint.Waypoint(pausedOn.getLatitude(),
            pausedOn.getLongitude(), (float)pausedOn.getAltitude());
        subTask.addWaypoint(wp);

        for (int i = suspendedAfterWpIndex + 1; i < task.getWaypointCount(); i++) {
            subTask.addWaypoint(task.getWaypointList().get(i));
        }
        subTask.getWaypointList().get(0).cornerRadiusInMeters = 0.2F;
        if (subTask.getWaypointList().size() >= 2) {
            subTask.getWaypointList().get(1).cornerRadiusInMeters = 0.2F;
        }
        return subTask.build();
    }

    private static ArrayList<WaypointAction> createPanoramaActions(Panorama panorama,  dji.common.mission.waypoint.Waypoint currentWp, Integer stepOveride) {
        ArrayList<WaypointAction> retVal = new ArrayList<>();

        int deg = (int)panorama.getAngle();  // pan. angle [-360, 360], positive = clockwise
        int step = stepOveride != null ? stepOveride : (int)panorama.getStep(); //step angle, zero stands for a
        // continuous rotation.
        float delay = panorama.getDelay(); // Delay interval between two steps in milliseconds.
        double velocityDegPerSecond = panorama.getVelocity();

        boolean fullRevolution = deg >= 360 || deg <= -360;

        //Step1 : detect current course
        double courseNow = detectCurrentCourse(currentWp);

        // we have 7

        if (panorama.getMode() == Panorama.PanoramaModeType.MODE_PHOTO) {
            retVal.add(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
            if (step <= 0) step = 60;
            if (step > 170) step = 170;

            int maxExtraTurnCnt = deg / step;
            if (maxExtraTurnCnt < 0) maxExtraTurnCnt = maxExtraTurnCnt * -1;
            int ost = deg % step;
            if (ost < 0) ost = ost * -1;

            for (int i = 0; i < maxExtraTurnCnt; i++) {
                courseNow = MathUtils.addToCourse180(courseNow, deg > 0 ? step : -step);
                retVal.add(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, (int)courseNow));
                retVal.add(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
            }

            if (ost > 0) {
                courseNow = MathUtils.addToCourse180(courseNow, deg > 0 ? ost : -ost);
                retVal.add(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, (int)courseNow));
                retVal.add(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
            }

            if (fullRevolution) {
                retVal.remove(retVal.size() - 1);
                retVal.remove(retVal.size() - 1);
            }

        } else if (panorama.getMode() == Panorama.PanoramaModeType.MODE_VIDEO) {
            retVal.add(new WaypointAction(WaypointActionType.START_RECORD, 0));

            //Step2 : add yaw in loop to reach target
            int maxExtraTurnCnt = deg / 170;
            if (maxExtraTurnCnt < 0) maxExtraTurnCnt = maxExtraTurnCnt * -1;
            int ost = deg % 170;
            if (ost < 0) ost = ost * -1;

            for (int i = 0; i < maxExtraTurnCnt; i++) {
                courseNow = MathUtils.addToCourse180(courseNow, deg > 0 ? 170 : -170);
                retVal.add(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, (int)courseNow));
            }

            if (ost > 0) {
                courseNow = MathUtils.addToCourse180(courseNow, deg > 0 ? ost : -ost);
                retVal.add(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, (int)courseNow));
            }
            //end Step2
            retVal.add(new WaypointAction(WaypointActionType.STOP_RECORD, 0));
        } else {
            Timber.e("unknown panorama mode");
        }

        return retVal;
    }

    private static double detectCurrentCourse(dji.common.mission.waypoint.Waypoint currentWp) {
        double retVal = currentWp.heading;
        if (hasAnyActions(currentWp)) {
            for (int i = 0; i < currentWp.waypointActions.size(); i++) {
                if (currentWp.waypointActions.get(i).actionType == WaypointActionType.ROTATE_AIRCRAFT) {
                    retVal = currentWp.waypointActions.get(i).actionParam;
                }
            }
        }
        return retVal;
    }

    public static String generateMissionWarningText(Context context, DjiMissionAndHome task) {
        return "";
    }

    public static String generateIntervalRoundingWarning(HashMap<Integer, OnWpActions> wpActions) {
        if (wpActions == null || wpActions.isEmpty()) return null;
        Set<Integer> timeIntervalRounded = new HashSet<>();
        for (OnWpActions one : wpActions.values()) {
            CameraSeriesTime cst = one.seriesByTime;
            if (cst != null && cst.getInterval() % 1000 > 0) {
                timeIntervalRounded.add(cst.getInterval());
            }
        }

        if (timeIntervalRounded.isEmpty()) return null;

        List<Integer> sortedList = new ArrayList<>(timeIntervalRounded);
        Collections.sort(sortedList);

        StringBuilder sb = new StringBuilder();
        Iterator<Integer> iterator = sortedList.iterator();
        while (iterator.hasNext()) {
            Integer i = iterator.next();
            CameraSeriesTime cst = new CameraSeriesTime().setInterval(i);
            sb.append(String.format(Locale.US, "%1.1fs to %ds", cst.getIntervalAsDouble(), cst
                .getRoundedIntervalInSeconds()));
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public static String generateErrorDescriptionForPrepareFailure(DJIError djiError, Context
        context) {
        String ed = djiError.getDescription();
        /*if (djiError instanceof dji.common.error.DJIFlightControllerError) {
            dji.common.error.DJIFlightControllerError mError = (dji.common.error.DJIFlightControllerError)djiError;
            if (dji.common.error.DJIFlightControllerError.MISSION_RESULT_RC_MODE_ERROR == mError) {
                if (dm != null) {
                    VehicleType vt = dm.droneInfo.vehicleType;
                    if (vt == VehicleType.DJI_PHANTOM3_STA) {
                        ed = context.getString(R.string.rc_mode_error_p3std);
                    } else if (vt == VehicleType.DJI_A3 || vt == VehicleType.DJI_PHANTOM_4 || vt == VehicleType
                        .DJI_PHANTOM_4_PRO || vt == VehicleType.DJI_N3) {
                        ed = context.getString(R.string.rc_mode_error_p4pro);
                    } else {
                        ed = context.getString(R.string.rc_mode_error);
                    }
                }
                return ed;
            }

            if (dji.common.error.DJIFlightControllerError.MISSION_RESULT_AIRCRAFT_TAKINGOFF == mError) {
                return ed;
            }
            return ed;
        }*/

        // FIXME deprecated as now wi will use modern waypoint operator
        if (djiError instanceof DJIMissionError) {
            DJIMissionError mError = (DJIMissionError)djiError;
            // The waypoint distance is too long
            if (DJIMissionError.WAYPOINT_DISTANCE_TOO_LONG == mError) {
                ed = "The distance between adjacent waypoints should be smaller than 2km. Note, that first and last "
                    + "waypoints are also considered as an adjacent.";
            } else if (DJIMissionError.TAKE_OFF == mError) {
                ed = djiError.getDescription();
            } else if (DJIMissionError.RC_MODE_ERROR == mError) {
                ed = "ERROT";
            }

        } else {
            ed = ed + "\nAn error occurred during mission upload, please ensure the data link is strong and try again.";
        }
        return ed;
    }

    /**
     * We need to stop series by tyme when reaching some WP.
     * However, there is an exceptions, when we don't need to stop series:
     * 0. border cases, i.e index = 0 or last
     * 1. mission has no camera actions at all.
     * 2.
     */
    public static boolean smartSeriesStop(int reachedWp, @NonNull Mission mission) {
        int wpCnt = mission.getWaypointCount();
        if (reachedWp <= 0) return false; // as reached 1st WP.

        if (reachedWp >= wpCnt - 1) return true; // as reached last wp

        List<MissionItem> missionItems = mission.getMissionItems();
        boolean hasAnyCameraActions = false;
        for (int i = 0; i < missionItems.size(); i++) {
            MissionItem item = missionItems.get(i);
            MissionItemType type = item.getType();
            if (MissionItemType.isCameraActionType(type)) hasAnyCameraActions = true;
        }

        if (!hasAnyCameraActions) return false; // as mission has no any camera actions at all.

        CameraSeriesTime whyWeStarted = null;
        CameraSeriesTime nextSeries = null;

        int foundWpInd = -1;
        for (int i = 0; i < missionItems.size(); i++) {
            MissionItem item = missionItems.get(i);
            MissionItemType type = item.getType();
            switch (type) {
                case WAYPOINT:
                case SPLINE_WAYPOINT:
                case LAND:
                    foundWpInd++;
                    break;
                case CAMERA_SERIES_TIME:
                    if (foundWpInd < reachedWp) {
                        whyWeStarted = (CameraSeriesTime)item;
                    } else if (foundWpInd == reachedWp) {
                        nextSeries = (CameraSeriesTime)item;
                    }
                    break;
                case CAMERA_TRIGGER:
                case PANORAMA:
                    if (foundWpInd == reachedWp) {
                        return true;
                    }
                    break;
            }
        }

        if (whyWeStarted != null && nextSeries != null) {
            if (whyWeStarted.getQty() == nextSeries.getQty()
                && whyWeStarted.getInterval() == nextSeries.getInterval()
                && whyWeStarted.getQty() == 0) {
                return false;
            }
        }

        return true;
    }

}