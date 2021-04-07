package com.example.ugcssample.model.utils;


import com.example.ugcssample.model.EmergencyActionType;
import com.example.ugcssample.model.GuidedTarget;
import com.example.ugcssample.model.HomeLocationSourceType;
import com.example.ugcssample.model.Mission;
import com.example.ugcssample.model.MissionAttributes;
import com.example.ugcssample.model.MissionItem;
import com.example.ugcssample.model.MissionItemType;
import com.example.ugcssample.model.command.CameraAttitude;
import com.example.ugcssample.model.command.CameraMediaFileInfo;
import com.example.ugcssample.model.command.CameraSeriesDistance;
import com.example.ugcssample.model.command.CameraSeriesTime;
import com.example.ugcssample.model.command.CameraTrigger;
import com.example.ugcssample.model.command.CameraZoom;
import com.example.ugcssample.model.command.ChangeSpeed;
import com.example.ugcssample.model.command.Panorama;
import com.example.ugcssample.model.command.Takeoff;
import com.example.ugcssample.model.command.Wait;
import com.example.ugcssample.model.command.Yaw;
import com.example.ugcssample.model.coordinate.LatLong;
import com.example.ugcssample.model.coordinate.LatLongAlt;
import com.example.ugcssample.model.spatial.BaseSpatialItem;
import com.example.ugcssample.model.spatial.Circle;
import com.example.ugcssample.model.spatial.Land;
import com.example.ugcssample.model.spatial.PointOfInterest;
import com.example.ugcssample.model.spatial.SplineWaypoint;
import com.example.ugcssample.model.spatial.Waypoint;
import com.example.ugcssample.model.type.DirectionType;
import com.example.ugcssample.model.utils.unitsystem.providers.length.LengthUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.speed.SpeedUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.systems.UnitSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.example.ugcssample.model.EmergencyActionType.LAND;
import static com.example.ugcssample.model.EmergencyActionType.RTH;
import static com.example.ugcssample.model.MissionItemType.isKindOfWaypoint;
import static com.example.ugcssample.model.utils.StringUtils.NL;

public final class MissionUtils {

    private MissionUtils() {
    }

    public static Mission copyMission(Mission src) {
        if (src == null)
            return null;
        // ---- MissionAttributes ----
        MissionAttributes missionAttributes = new MissionAttributes();
        // ---- MissionAttributes - Home Location source ----
        if (src.missionAttributes.getHomeLocationSourceType() == HomeLocationSourceType.EXPLICIT) {
            missionAttributes.setExplicitHomeLocation(src.missionAttributes.getHomeLat(), src.missionAttributes
                .getHomeLon());
        } else {
            missionAttributes.setHomeLocationSourceType(src.missionAttributes.getHomeLocationSourceType());
        }
        // ---- MissionAttributes - Failsafe actions ----
        missionAttributes.returnAltitude = src.missionAttributes.returnAltitude;
        missionAttributes.rcLostAction = src.missionAttributes.rcLostAction;
        missionAttributes.gpsLostAction = src.missionAttributes.gpsLostAction;
        missionAttributes.lowBatteryAction = src.missionAttributes.lowBatteryAction;

        Mission m = new Mission(src.missionId, src.missionName, missionAttributes);

        return m;
    }

    public static List<LatLong> getVisibleCoords(Mission mission) {
        final List<LatLong> coords = new ArrayList<>();

        if (mission == null || mission.getMissionItems().isEmpty()) {
            return coords;
        }

        for (MissionItem item : mission.getMissionItems()) {
            if (!(item instanceof MissionItem.SpatialItem))
                continue;

            MissionItemType type = item.getType();
            if (type == MissionItemType.POINT_OF_INTEREST
                && ((PointOfInterest)item).getMode() == PointOfInterest.RegionOfInterestType.NONE)
                continue;

            final LatLong coordinate = ((MissionItem.SpatialItem)item).getCoordinate();
            if (coordinate.isValid()) {
                coords.add(coordinate);
            }

            if (type == MissionItemType.CIRCLE) {
                Circle circle = (Circle)item;
                coords.add(MathUtils.newCoordFromBearingAndDistance(coordinate, 0, circle.getRadius()));
                coords.add(MathUtils.newCoordFromBearingAndDistance(coordinate, 90, circle.getRadius()));
                coords.add(MathUtils.newCoordFromBearingAndDistance(coordinate, 180, circle.getRadius()));
                coords.add(MathUtils.newCoordFromBearingAndDistance(coordinate, 270, circle.getRadius()));
            }
        }
        return coords;
    }

    public static String generateMissionLog(Mission mission) {
        StringBuilder sb = new StringBuilder(NL);
        if (mission == null) {
            sb.append(NL).append("Mission is NULL");
            return sb.toString();
        }

        MissionAttributes ma = mission.missionAttributes;

        Double minAlt = null;
        Double maxAlt = null;

        // Mission name & id
        sb.append(NL).append("Mission '").append(mission.missionName).append("' (id=")
            .append(mission.missionId).append(")");

        if (mission.transientAttributes.transientTag != null) {
            sb.append(NL).append("  transient TAG=").append(mission.transientAttributes.transientTag);
        }
        if (mission.transientAttributes.transientDescription != null) {
            sb.append(NL).append("  transient DESCRIPTION =").append(mission.transientAttributes.transientDescription);
        }
        if (mission.transientAttributes.nativeHeadingMode != null) {
            sb.append(NL).append("  transient HEADING=").append(mission.transientAttributes.nativeHeadingMode);
        }
        sb.append(NL).append("  Home source ").append(ma.getHomeLocationSourceType().toString());
        if (ma.getHomeLocationSourceType() == HomeLocationSourceType.EXPLICIT) {
            sb.append(String.format(Locale.US, " lat=%2.7f lon=%2.7f", ma.getHomeLat(), ma.getHomeLon()));
        }
        sb.append(String.format("%n  GotoFirstWaypointMode %s", ma.gotoFirstWaypointMode.toString()));
        sb.append(String.format(Locale.US, "%n  Return Alt %2.1f, on RemoteController Lost %s",
            ma.returnAltitude, ma.rcLostAction.toString()));

        for (MissionItem mi : mission.getMissionItems()) {
            sb.append(String.format(Locale.US, "%n    %2d | %s", mi.getIndexInSrcCmd(), mi.getType().toString()));

            if (mi.getType() == MissionItemType.POINT_OF_INTEREST) {
                PointOfInterest roi = (PointOfInterest)mi;
                sb.append(" ").append(roi.getMode().toString());
            } else if (mi.getType() == MissionItemType.CHANGE_SPEED) {
                ChangeSpeed spd = (ChangeSpeed)mi;
                sb.append(String.format(Locale.US, " %2.1f", spd.getSpeed()));
            } else if (mi.getType() == MissionItemType.TAKEOFF) {
                Takeoff to = (Takeoff)mi;
                sb.append(String.format(Locale.US, " alt=%2.2fm", to.getTakeoffAltitude()));
                minAlt = MathUtils.minOrNull(minAlt, to.getTakeoffAltitude());
                maxAlt = MathUtils.maxOrNull(maxAlt, to.getTakeoffAltitude());
            } else if (mi.getType() == MissionItemType.WAYPOINT) {
                Waypoint wp = (Waypoint)mi;
                sb.append(String.format(Locale.US, " alt=%2.2fm lat=%2.7f lon=%2.7f", wp.getAlt(), wp.getLat(), wp
                    .getLon()));
                minAlt = MathUtils.minOrNull(minAlt, wp.getAlt());
                maxAlt = MathUtils.maxOrNull(maxAlt, wp.getAlt());
            } else if (mi.getType() == MissionItemType.SPLINE_WAYPOINT) {
                SplineWaypoint spwp = (SplineWaypoint)mi;
                sb.append(String.format(Locale.US, " alt=%2.2fm lat=%2.7f lon=%2.7f maxCR=%2.1fm", spwp.getAlt(),
                    spwp.getLat(), spwp.getLon(), spwp.getMaxCornerRadius()));
                minAlt = MathUtils.minOrNull(minAlt, spwp.getAlt());
                maxAlt = MathUtils.maxOrNull(maxAlt, spwp.getAlt());
            } else if (mi.getType() == MissionItemType.WAIT) {
                Wait wait = (Wait)mi;
                sb.append(String.format(Locale.US, " %5d ms", wait.getTime()));
            } else if (mi.getType() == MissionItemType.YAW) {
                Yaw yaw = (Yaw)mi;
                sb.append(String.format(Locale.US, " %2.1f", yaw.getAngle()));
            } else if (mi.getType() == MissionItemType.CAMERA_TRIGGER) {
                CameraTrigger ctr = (CameraTrigger)mi;
                CameraTrigger.CameraTriggerModeType mode = ctr.getMode();
                sb.append(String.format(" %s", mode.toString()));
            } else if (mi.getType() == MissionItemType.CAMERA_SERIES_DISTANCE) {
                CameraSeriesDistance csd = (CameraSeriesDistance)mi;
                sb.append(String.format(Locale.US, " dist=%3.2fm qty=%3d d=%dms", csd.getDistance(), csd.getQty(),
                    csd.getDelay()));
            } else if (mi.getType() == MissionItemType.CAMERA_SERIES_TIME) {
                CameraSeriesTime cst = (CameraSeriesTime)mi;
                sb.append(String.format(Locale.US, " intrvl=%1.1fs qty=%d d=%dms", cst.getIntervalAsDouble(), cst
                    .getQty(), cst.getDelay()));
            } else if (mi.getType() == MissionItemType.CAMERA_ATTITUDE) {
                CameraAttitude ca = (CameraAttitude)mi;
                sb.append(String.format(Locale.US, " pitch=%1.1f(%b) yaw=%1.1f(%b) roll=%1.1f(%b) zoom=%d(%b)",
                    ca.getPitch(), ca.isPitchAvailable(), ca.getYaw(), ca.isYawAvailable(),
                    ca.getRoll(), ca.isRollAvailable(), ca.getZoom(), ca.isZoomAvailable()));
            } else if (mi.getType() == MissionItemType.CAMERA_ZOOM) {
                CameraZoom cz = (CameraZoom)mi;
                sb.append(String.format(Locale.US, " zoom=%d", cz.getZoom()));
            } else if (mi.getType() == MissionItemType.CAMERA_MEDIA_FILE_INFO) {
                CameraMediaFileInfo cm = (CameraMediaFileInfo)mi;
                sb.append(String.format(" mediaFileInfo=%s",
                    cm.getMediaFileInfo() == null ? "NULL" : cm.getMediaFileInfo()));
            } else if (mi.getType() == MissionItemType.PANORAMA) {
                Panorama pan = (Panorama)mi;
                sb.append(String.format(Locale.US,
                    " %s %s angle=%2.1f step=%2.1f",
                    pan.getMode().toString(),
                    pan.getAngle() > 0 ? "CW" : "CCW",
                    pan.getAngle(), pan.getStep()));
            } else if (mi.getType() == MissionItemType.CIRCLE) {
                Circle cr = (Circle)mi;
                sb.append(String.format(Locale.US, " alt=%2.2fm radius=%2.2fm lat=%2.7f lon=%2.7f",
                    cr.getAlt(), cr.getRadius(), cr.getLat(), cr.getLon()));
                if (minAlt == null || minAlt > cr.getAlt())
                    minAlt = cr.getAlt();
                if (maxAlt == null || maxAlt < cr.getAlt())
                    maxAlt = cr.getAlt();
            }
        }
        sb.append(String.format(Locale.US, "%n  minAlt=%2.2fm maxAlt=%2.2fm", minAlt, maxAlt));
        return sb.toString();
    }

    /**
     * Generates Mission details text, used to display in Open Mission dialog
     */
    public static String generateMissionDetails(Mission mission, UnitSystem unitSystem) {
        LengthUnitProvider lp = unitSystem.getLengthUnitProvider();
        SpeedUnitProvider sp = unitSystem.getSpeedUnitProvider();
        Double minAlt = null;
        Double maxAlt = null;

        StringBuilder sb = new StringBuilder();

        // ----- Route name & Parameters -----
        enrichRouteParameters(sb, mission, lp);

        // ----- Route Task -----
        sb.append("Route Task");
        int wpOrder = 0;
        for (MissionItem mi : mission.getMissionItems()) {
            sb.append(String.format(Locale.US, "%n%s", mi.getType().toString()));
            if (mi.getType() == MissionItemType.POINT_OF_INTEREST) {
                PointOfInterest roi = (PointOfInterest)mi;
                sb.append(" ").append(roi.getMode().toString());
            } else if (mi.getType() == MissionItemType.CHANGE_SPEED) {
                ChangeSpeed spd = (ChangeSpeed)mi;
                sb.append(String.format(Locale.US, " %2.1f%s",
                    sp.getFromMetersPerSecond(spd.getSpeed()), sp.getDefLetter()));
            } else if (mi.getType() == MissionItemType.TAKEOFF) {
                Takeoff to = (Takeoff)mi;
                sb.append(String.format(Locale.US, " alt=%2.2f%s",
                    lp.getFromMeters(to.getTakeoffAltitude()), lp.getDefLetter()));
                if (minAlt == null || minAlt > to.getTakeoffAltitude())
                    minAlt = to.getTakeoffAltitude();
                if (maxAlt == null || maxAlt < to.getTakeoffAltitude())
                    maxAlt = to.getTakeoffAltitude();
            } else if (mi.getType() == MissionItemType.WAYPOINT) {
                Waypoint wp = (Waypoint)mi;
                wpOrder++;
                sb.append(String.format(Locale.US, " #%2d alt=%2.2f%s", wpOrder,
                    lp.getFromMeters(wp.getAlt()), lp.getDefLetter()));
                if (minAlt == null || minAlt > wp.getAlt())
                    minAlt = wp.getAlt();
                if (maxAlt == null || maxAlt < wp.getAlt())
                    maxAlt = wp.getAlt();
            } else if (mi.getType() == MissionItemType.SPLINE_WAYPOINT) {
                SplineWaypoint spwp = (SplineWaypoint)mi;
                wpOrder++;
                sb.append(String.format(Locale.US, " #%2d alt=%2.2f%s", wpOrder,
                    lp.getFromMeters(spwp.getAlt()), lp.getDefLetter()));
                if (minAlt == null || minAlt > spwp.getAlt())
                    minAlt = spwp.getAlt();
                if (maxAlt == null || maxAlt < spwp.getAlt())
                    maxAlt = spwp.getAlt();
            } else if (mi.getType() == MissionItemType.LAND) {
                Land wp = (Land)mi;
                wpOrder++;
                sb.append(String.format(Locale.US, " #%2d alt=%2.2f%s", wpOrder,
                    lp.getFromMeters(wp.getAlt()), lp.getDefLetter()));
                if (minAlt == null || minAlt > wp.getAlt())
                    minAlt = wp.getAlt();
                if (maxAlt == null || maxAlt < wp.getAlt())
                    maxAlt = wp.getAlt();
            } else if (mi.getType() == MissionItemType.WAIT) {
                Wait wait = (Wait)mi;
                sb.append(String.format(Locale.US, " %5d ms", wait.getTime()));
            } else if (mi.getType() == MissionItemType.YAW) {
                Yaw yaw = (Yaw)mi;
                sb.append(String.format(Locale.US, " %2.1f", yaw.getAngle()));
            } else if (mi.getType() == MissionItemType.CAMERA_TRIGGER) {
                CameraTrigger ctr = (CameraTrigger)mi;
                CameraTrigger.CameraTriggerModeType mode = ctr.getMode();
                sb.append(String.format(" %s", cameraTriggerModeTypeToString(mode)));
            } else if (mi.getType() == MissionItemType.CAMERA_SERIES_DISTANCE) {
                CameraSeriesDistance csd = (CameraSeriesDistance)mi;
                sb.append(String.format(Locale.US, " dist=%3.2f%s qty=%3d d=%dms",
                    lp.getFromMeters(csd.getDistance()), lp.getDefLetter(),
                    csd.getQty(), csd.getDelay()));
            } else if (mi.getType() == MissionItemType.CAMERA_SERIES_TIME) {
                CameraSeriesTime cst = (CameraSeriesTime)mi;
                sb.append(String.format(Locale.US, " intrvl=%1.1fs qty=%d d=%dms", cst.getIntervalAsDouble(), cst
                    .getQty(), cst.getDelay()));
            } else if (mi.getType() == MissionItemType.CAMERA_ATTITUDE) {
                CameraAttitude ca = (CameraAttitude)mi;
                sb.append(String.format(Locale.US, " pitch=%1.1f yaw=%1.1f zoom=%d",
                    ca.getPitch(), ca.getYaw(), ca.getZoom()));
            } else if (mi.getType() == MissionItemType.CAMERA_ZOOM) {
                CameraZoom cz = (CameraZoom)mi;
                sb.append(String.format(Locale.US, " zoom=%d", cz.getZoom()));
            } else if (mi.getType() == MissionItemType.CAMERA_MEDIA_FILE_INFO) {
                CameraMediaFileInfo cm = (CameraMediaFileInfo)mi;
                sb.append(String.format(" mediaFileInfo=%s",
                    cm.getMediaFileInfo() == null ? "NULL" : cm.getMediaFileInfo()));
            } else if (mi.getType() == MissionItemType.PANORAMA) {
                Panorama pan = (Panorama)mi;
                sb.append(String.format(Locale.US, " %s %s angle=%2.1f step=%2.1f",
                    pan.getMode().toString(), pan.getAngle() > 0 ? "CW" : "CCW", pan.getAngle(), pan.getStep()));
            } else { // CIRCLE and RETURN_TO_HOME are Unsupported so far...
                throw new RuntimeException(AppUtils.UNHANDLED_SWITCH + "for " + mi.getType().toString());
            }
        }
        sb.append(NL).append(NL);

        // ----- Note & Summary -----
        enrichRouteSummary(sb, lp, minAlt, maxAlt);

        return sb.toString();
    }

    private static void enrichRouteParameters(StringBuilder sb, Mission mission, LengthUnitProvider lp) {
        // ----- Route name -----
        if (mission.missionName != null && !mission.missionName.isEmpty()) {
            String name = mission.missionName;
            if (name.lastIndexOf(".") > 0) {
                name = name.substring(0, name.lastIndexOf("."));
            }
            sb.append("Route Name").append(NL).append(name).append(NL).append(NL);
        }

        // ----- Route Parameters -----
        MissionAttributes ma = mission.missionAttributes;
        sb.append("Route Parameters").append(NL);
        String hs = homeLocationSourceTypeToString(ma);
        if (hs != null)
            sb.append("Home source: ").append(hs).append(NL);

        sb.append(String.format(Locale.US, "Return altitude: %2.1f%s%n",
            lp.getFromMeters(ma.returnAltitude), lp.getDefLetter()));
        String rcAction = emergencyActionTypeToString(ma.rcLostAction);
        if (rcAction != null) {
            sb.append(rcAction).append(" on RC connection lost").append(NL);
        }
        sb.append(NL);
    }

    private static void enrichRouteSummary(StringBuilder sb, LengthUnitProvider lp, Double minAlt, Double maxAlt) {
        // ----- Note... -----
        sb.append("Note:").append(NL).append("All altitudes are in AHL format");
        sb.append(NL).append(NL);

        // ----- Route Summary -----
        sb.append("Route Summary").append(NL);

        sb.append(String.format(Locale.US, "Minimal altitude %2.2f%s%nMaximal altitude %2.2f%s",
            lp.getFromMeters(minAlt == null ? 0.0 : minAlt), lp.getDefLetter(),
            lp.getFromMeters(maxAlt == null ? 0.0 : maxAlt), lp.getDefLetter()));
    }

    private static String emergencyActionTypeToString(EmergencyActionType action) {
        if (action == null)
            return null;
        switch (action) {
            case RTH:
                return "Return to home";
            case LAND:
                return "Land";
            case WAIT:
                return "Wait";
            case CONTINUE:
                return "Continue";
            default:
                throw new RuntimeException(AppUtils.UNHANDLED_SWITCH);
        }
    }

    private static String homeLocationSourceTypeToString(MissionAttributes ma) {
        switch (ma.getHomeLocationSourceType()) {
            case NONE:
                return "Current home position";
            case CURRENT_POSITION:
                return "Current drone position";
            case FIRST_WAYPOINT:
                return "First waypoint";
            case EXPLICIT:
                return String.format(Locale.US, "Explicit at %2.7f / %2.7f", ma.getHomeLat(), ma.getHomeLon());
            default:
                throw new RuntimeException(AppUtils.UNHANDLED_SWITCH);
        }
    }

    private static String cameraTriggerModeTypeToString(CameraTrigger.CameraTriggerModeType mode) {
        switch (mode) {
            case SINGLE_SHOT:
                return "Shot";
            case START_RECORDING:
                return "Start recording";
            case STOP_RECORDING:
                return "Stop recording";
            default:
                throw new RuntimeException(AppUtils.UNHANDLED_SWITCH);
        }
    }

    /**
     * 1. Using Safe trajectory type, UCS server generates additional waypoints
     * with same coordinates but different altitude in order to
     * perform "stairs" flight (only horizontal and vertical movement, no flight by diagonal)
     * Some adjacent waypoints that are located too close in vertical axis (less then altitudeLimit)
     * could not be accepted by autopilot as they are too close.
     * So we will remove them
     * <p>
     * 2. If adjacent waypoints has same lat,lon,alt, all actions must be from second WP must be merged into first,
     * second WP must be removed
     */
    public static void clearVerticalWaypoints(Mission m, double altitudeLimit) {
        int cnt = m.getMissionItemsCnt();
        for (int currentWpIndex = 0; currentWpIndex < cnt - 1; currentWpIndex++) {
            MissionItemType firstType = m.getMissionItem(currentWpIndex).getType();
            if (firstType != MissionItemType.WAYPOINT && firstType != MissionItemType.SPLINE_WAYPOINT) {
                continue;
            }

            int nextWpIndex = 0;

            for (int j = currentWpIndex + 1; j < cnt; j++) {
                MissionItemType type = m.getMissionItem(j).getType();
                if (type == MissionItemType.WAYPOINT || type == MissionItemType.SPLINE_WAYPOINT) {
                    nextWpIndex = j;
                    break;
                }
            }

            if (nextWpIndex < 1) {
                continue;
            }

            BaseSpatialItem first = (BaseSpatialItem)m.getMissionItem(currentWpIndex);
            BaseSpatialItem next = (BaseSpatialItem)m.getMissionItem(nextWpIndex);

            if (first.getLat() == next.getLat() && first.getLon() == next.getLon()) {
                if (first.getAlt() == next.getAlt()) {
                    // user mede double clicks
                    m.removeMissionItem(nextWpIndex);  // just remove WP (actions will be merged)
                    clearVerticalWaypoints(m, altitudeLimit);
                    return;

                } else if (Math.abs(first.getAlt() - next.getAlt()) < altitudeLimit) {
                    // waypoint generated by "safe trajectory type"
                    first.getCoordinate().setAltitude(next.getAlt());
                    for (int x = nextWpIndex; x > currentWpIndex; x--) {
                        m.removeMissionItem(x);
                    }
                    clearVerticalWaypoints(m, altitudeLimit);
                    return;
                }
            }
        }
    }

    /**
     * Randomly generate TargetPoint
     **/
    public static GuidedTarget prepareTestGuidedTarget(LatLongAlt position, LatLong home, double yaw) {
        GuidedTarget tp = new GuidedTarget();
        tp.speed = 2.0;
        tp.coordinate = new LatLongAlt();
        tp.coordinate.setAltitude(position.getAltitude());
        tp.altitudeAmsl = position.getAltitude();

        // tp.coordinate.setLatitude(mLatitude + 15 * MathUtils.ONE_METER_OFFSET);
        // tp.coordinate.setLongitude(mLongitude + 15 * MathUtils.calcLongitudeOffset(mLatitude));
        LatLong ll = MathUtils.newCoordFromBearingAndDistance(position, yaw, 15.0);
        tp.coordinate.setLatitude(ll.getLatitude());
        tp.coordinate.setLongitude(ll.getLongitude());

        return tp;
    }

    public static Mission prepareTestMission(LatLong position, boolean validMission) {
        double mLatitude = position.getLatitude();
        double mLongitude = position.getLongitude();
        Mission m = new Mission();

        if (validMission) {
            m.missionAttributes.setHomeLocationSourceType(HomeLocationSourceType.FIRST_WAYPOINT);
        } else {
            m.missionAttributes.setExplicitHomeLocation(mLatitude - 35 * MathUtils.ONE_METER_OFFSET, mLongitude);
        }

        m.missionAttributes.rcLostAction = EmergencyActionType.RTH;
        m.missionAttributes.returnAltitude = 50.1d + MathUtils.randInt(1, 10);

        // Takeoff
        //m.addMissionItem(new Takeoff().setTakeoffAltitude(10d));

        // WP1
        Waypoint wp1 = new Waypoint();
        wp1.setCoordinate(new LatLongAlt(mLatitude + 1 * MathUtils.ONE_METER_OFFSET, mLongitude, 25d));
        m.addMissionItem(wp1);

        /*if (!validMission) {
            Waypoint wpX = new Waypoint();
            //wpX.setCoordinate(new LatLongAlt(mLatitude + 15.3 * MathUtils.ONE_METER_OFFSET, mLongitude, 12d));
            wpX.setCoordinate(new LatLongAlt(mLatitude + 3000 * MathUtils.ONE_METER_OFFSET, mLongitude, 12d));
            m.addMissionItem(wpX);
        }*/

        // Wait 2s
        //m.addMissionItem(new Wait().setTime(2000));

        // Tilt camera to 70deg down
        //m.addMissionItem(new CameraAttitude().setTilt(70.0d));

        // Make a shot
        //m.addMissionItem(new CameraTrigger().setMode(CameraTrigger.CameraTriggerModeType.SINGLE_SHOT));

        // Start Video recording
        //m.addMissionItem(new CameraTrigger().setMode(CameraTrigger.CameraTriggerModeType.START_RECORDING));

        // ChangeSpeed
        m.addMissionItem(new ChangeSpeed().setSpeed(5d));

        //m.addMissionItem(new CameraSeriesTime().setInterval(3000));

        // WP2
        Waypoint wp2 = new Waypoint();
        wp2.setCoordinate(new LatLongAlt(mLatitude + 25 * MathUtils.ONE_METER_OFFSET, mLongitude + 25 * MathUtils
            .calcLongitudeOffset(mLatitude), 25d));
        m.addMissionItem(wp2);

        // Stop Video recording
        //m.addMissionItem(new CameraTrigger().setMode(CameraTrigger.CameraTriggerModeType.STOP_RECORDING));

        //m.addMissionItem(new Panorama().setMode(Panorama.PanoramaModeType.MODE_PHOTO).setAngle(-300d).setStep(60));

        // ChangeSpeed
        //m.addMissionItem(new ChangeSpeed().setSpeed(5d));

        //m.addMissionItem(new CameraSeriesTime().setInterval(4000));

        // WP3
        Waypoint wp3 = new Waypoint();
        wp3.setCoordinate(new LatLongAlt(mLatitude + 15 * MathUtils.ONE_METER_OFFSET, mLongitude, 25d));
        m.addMissionItem(wp3);

        // Start Camera Series evry 3s
        //m.addMissionItem(new CameraSeriesTime().setInterval(5000));

        // WP4
        Waypoint wp4 = new Waypoint();
        wp4.setCoordinate(new LatLongAlt(mLatitude + 15 * MathUtils.ONE_METER_OFFSET, mLongitude - 15 * MathUtils
            .calcLongitudeOffset(mLatitude), 25));
        m.addMissionItem(wp4);

        // WP4 (Land)
        //m.addMissionItem(new Land(new LatLongAlt(mLatitude, mLongitude, 20d)));
        return m;
    }

    public static Mission prepareMissionForCameraSeriesTimeTest(LatLong position, boolean validMission) {
        double mLatitude = position.getLatitude();
        double mLongitude = position.getLongitude();
        Mission m = new Mission();

        m.missionAttributes.setHomeLocationSourceType(HomeLocationSourceType.FIRST_WAYPOINT);
        m.missionAttributes.rcLostAction = EmergencyActionType.RTH;
        m.missionAttributes.returnAltitude = 50d;

        // Takeoff
        m.addMissionItem(new Takeoff().setTakeoffAltitude(10d));

        // ChangeSpeed
        m.addMissionItem(new ChangeSpeed().setSpeed(2.0d));

        // WP1
        Waypoint wp1 = new Waypoint();
        wp1.setCoordinate(new LatLongAlt(mLatitude - 1 * MathUtils.ONE_METER_OFFSET, mLongitude, 25d));
        m.addMissionItem(wp1);

        // Tilt camera to 45deg down
        m.addMissionItem(new CameraAttitude().setPitch(-45.0d));

        // Start Camera Series every 3s
        m.addMissionItem(new CameraSeriesTime().setInterval(3000));

        // WP2
        Waypoint wp2 = new Waypoint();
        wp2.setCoordinate(new LatLongAlt(mLatitude + 25 * MathUtils.ONE_METER_OFFSET, mLongitude + 25 * MathUtils
            .calcLongitudeOffset(mLatitude), 25d));
        m.addMissionItem(wp2);

        // ChangeSpeed
        m.addMissionItem(new ChangeSpeed().setSpeed(5.0d));

        // WP3
        Waypoint wp3 = new Waypoint();
        wp3.setCoordinate(new LatLongAlt(mLatitude + 25 * MathUtils.ONE_METER_OFFSET, mLongitude - 25 * MathUtils
            .calcLongitudeOffset(mLatitude), 25d));
        m.addMissionItem(wp3);

        // ChangeSpeed
        m.addMissionItem(new ChangeSpeed().setSpeed(2.0d));

        // Start Camera Series every 3s
        m.addMissionItem(new CameraSeriesTime().setInterval(validMission ? 5000 : 1000));

        // WP4
        Waypoint wp4 = new Waypoint();
        wp4.setCoordinate(new LatLongAlt(mLatitude - 25 * MathUtils.ONE_METER_OFFSET, mLongitude + 25 * MathUtils
            .calcLongitudeOffset(mLatitude), 25d));
        m.addMissionItem(wp4);

        // WP4 (Land)
        m.addMissionItem(new Land(new LatLongAlt(mLatitude, mLongitude, 20d)));
        return m;
    }

    public static int getEmergencyActionTypeName(EmergencyActionType type) {
        switch (type) {
            case RTH:
                return 1;
            case LAND:
                return 2;
            case WAIT:
                return 3;
            case CONTINUE:
                return 4;
            default:
                throw new RuntimeException(AppUtils.UNHANDLED_SWITCH);
        }
    }

    public static int getDirectionTypeName(DirectionType type) {
        switch (type) {
            case CW:
                return 1;
            case CCW:
                return 2;
            default:
                throw new RuntimeException(AppUtils.UNHANDLED_SWITCH);
        }
    }

    public static LatLongAlt findFirstWp(final Mission mission) {
        for (MissionItem item : mission.getMissionItems()) {
            if (item instanceof Waypoint || item instanceof SplineWaypoint)
                return ((MissionItem.SpatialItem)item).getCoordinate();
        }
        return null;
    }

    public static int findLastWpIndex(final Mission mission) {
        if (mission.getMissionItems() != null && !mission.getMissionItems().isEmpty()) {
            for (int i = mission.getMissionItems().size() - 1; i >= 0; i--) {
                MissionItem item = mission.getMissionItems().get(i);
                if (item instanceof Waypoint || item instanceof SplineWaypoint) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static BaseSpatialItem findLastWp(final Mission mission) {
        if (mission.getMissionItems() != null && !mission.getMissionItems().isEmpty()) {
            for (int i = mission.getMissionItems().size() - 1; i >= 0; i--) {
                MissionItem item = mission.getMissionItems().get(i);
                if (item instanceof Waypoint || item instanceof SplineWaypoint) {
                    return (BaseSpatialItem)item;
                }
            }
        }
        return null;
    }

    public static Double findFirstChangeSpeed(final Mission mission) {
        for (MissionItem item : mission.getMissionItems()) {
            if (item instanceof ChangeSpeed)
                return ((ChangeSpeed)item).getSpeed();
        }
        return null;
    }

    public static Double findChangeSpeedJustAfterTakeoff(final Mission mission) {
        for (int i = 0; i < mission.getMissionItems().size(); i++) {
            MissionItem item = mission.getMissionItems().get(i);
            if (item instanceof Takeoff) {
                if (i + 1 < mission.getMissionItems().size()) {
                    MissionItem nextItem = mission.getMissionItems().get(i + 1);
                    if (nextItem instanceof ChangeSpeed) {
                        return ((ChangeSpeed)nextItem).getSpeed();
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    public static Double findChangeSpeedJustAfterFirstWaypoint(final Mission mission) {
        for (int i = 0; i < mission.getMissionItems().size(); i++) {
            MissionItem item = mission.getMissionItems().get(i);
            if (item instanceof SplineWaypoint || item instanceof Waypoint) {
                if (i + 1 < mission.getMissionItems().size()) {
                    MissionItem nextItem = mission.getMissionItems().get(i + 1);
                    if (nextItem instanceof ChangeSpeed) {
                        return ((ChangeSpeed)nextItem).getSpeed();
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    public static Double findLastChangeSpeed(final Mission mission) {
        Double retVal = null;
        for (MissionItem item : mission.getMissionItems()) {
            if (item instanceof ChangeSpeed) {
                ChangeSpeed cs = (ChangeSpeed)item;
                retVal = cs.getSpeed();
            }
        }
        return retVal;
    }

    public static PointOfInterest findFirstPointOfInterest(final Mission mission) {
        if (mission == null)
            return null;
        for (MissionItem item : mission.getMissionItems()) {
            if (item.getType() == MissionItemType.POINT_OF_INTEREST) {
                PointOfInterest poi = (PointOfInterest)item;
                if (poi.getMode() == PointOfInterest.RegionOfInterestType.LOCATION) {
                    return poi;
                }
            }
        }
        return null;
    }

    public static Yaw findFirstYaw(final Mission mission) {
        if (mission == null)
            return null;
        for (MissionItem item : mission.getMissionItems()) {
            if (item.getType() == MissionItemType.YAW) {
                return (Yaw)item;
            }
        }
        return null;
    }

    public static Yaw findFirstYawAfterSecondWp(final Mission mission) {
        if (mission == null)
            return null;

        int wpFound = 0;

        for (MissionItem item : mission.getMissionItems()) {
            if (isKindOfWaypoint(item.getType())) {
                wpFound++;
            } else if (item.getType() == MissionItemType.YAW) {
                if (wpFound == 1)
                    return null;
                else
                    return (Yaw)item;
            }
        }
        return null;
    }

    public static boolean hasYawOrPoi(Mission mission) {
        for (MissionItem item : mission.getMissionItems()) {
            if (item.getType() == MissionItemType.YAW
                || item.getType() == MissionItemType.POINT_OF_INTEREST) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasYaw(Mission mission) {
        for (MissionItem item : mission.getMissionItems()) {
            if (item.getType() == MissionItemType.YAW) {
                return true;
            }
        }
        return false;
    }
}
