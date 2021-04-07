package com.example.ugcssample.model.utils;
import com.example.ugcssample.model.coordinate.LatLong;
import com.example.ugcssample.model.coordinate.LatLongAlt;
import com.vividsolutions.jts.geom.Coordinate;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class MathUtils {

    public static final double ONE_METER_OFFSET = 0.00000899322;

    public static final int SIGNAL_MAX_FADE_MARGIN = 50;
    public static final int SIGNAL_MIN_FADE_MARGIN = 6;
    //public static final double RAD_IN_GRAD = 0.017453292519943295d;
    public static final double TWO_PI = 2 * Math.PI;
    /**
     * Radius of the earth in meters.
     * Source: WGS84
     */
    private static final double RADIUS_OF_EARTH = 6378137.0;

    private MathUtils() {
    }

    /**
     * Computes the distance between two points taking into consideration altitude
     *
     * @return distance in meters
     */
    public static double getDistance3D(LatLongAlt from, LatLongAlt to) {
        if (from == null || to == null)
            return -1;

        final double distance2d = getDistance2D(from, to);
        double distanceSqr = Math.pow(distance2d, 2);
        double altitudeSqr = Math.pow(to.getAltitude() - from.getAltitude(), 2);

        return Math.sqrt(altitudeSqr + distanceSqr);
    }

    public static double getDistance2D(LatLong from, LatLong to) {
        if (from == null || to == null)
            return -1;

        return RADIUS_OF_EARTH * Math.toRadians(getArcInRadians(from, to));
    }

    public static double getDistance2D(double fromLat, double fromLon, double toLat, double toLon) {
        return RADIUS_OF_EARTH * Math.toRadians(getArcInRadians(fromLat, fromLon, toLat, toLon));
    }

    /**
     * Calculates the arc between two points
     * http://en.wikipedia.org/wiki/Haversine_formula
     *
     * @return the arc in degrees
     */
    private static double getArcInRadians(LatLong from, LatLong to) {

        double latitudeArc = Math.toRadians(from.getLatitude() - to.getLatitude());
        double longitudeArc = Math.toRadians(from.getLongitude() - to.getLongitude());

        double latitudeH = Math.sin(latitudeArc * 0.5);
        latitudeH *= latitudeH;
        double lontitudeH = Math.sin(longitudeArc * 0.5);
        lontitudeH *= lontitudeH;

        double tmp = Math.cos(Math.toRadians(from.getLatitude()))
            * Math.cos(Math.toRadians(to.getLatitude()));
        return Math.toDegrees(2.0 * Math.asin(Math.sqrt(latitudeH + tmp * lontitudeH)));
    }

    /**
     * Calculates the arc between two points
     * http://en.wikipedia.org/wiki/Haversine_formula
     *
     * @return the arc in degrees
     */
    private static double getArcInRadians(double fromLat, double fromLon, double toLat,
                                          double toLon) {

        double latitudeArc = Math.toRadians(fromLat - toLat);
        double longitudeArc = Math.toRadians(fromLon - toLon);

        double latitudeH = Math.sin(latitudeArc * 0.5);
        latitudeH *= latitudeH;
        double lontitudeH = Math.sin(longitudeArc * 0.5);
        lontitudeH *= lontitudeH;

        double tmp = Math.cos(Math.toRadians(fromLat))
            * Math.cos(Math.toRadians(toLat));
        return Math.toDegrees(2.0 * Math.asin(Math.sqrt(latitudeH + tmp * lontitudeH)));
    }

    /**
     * Signal Strength in percentage
     *
     * @return percentage
     */
    public static int getSignalStrength(double fadeMargin, double remFadeMargin) {
        return (int)(MathUtils.normalize(Math.min(fadeMargin, remFadeMargin),
            SIGNAL_MIN_FADE_MARGIN, SIGNAL_MAX_FADE_MARGIN) * 100);
    }

    private static double constrain(double value, double min, double max) {
        value = Math.max(value, min);
        value = Math.min(value, max);
        return value;
    }

    public static double normalize(double value, double min, double max) {
        value = constrain(value, min, max);
        return (value - min) / (max - min);
    }

    /**
     * Based on the Ramer–Douglas–Peucker algorithm algorithm
     * http://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm
     */
    public static List<LatLong> simplify(List<LatLong> list, double tolerance) {
        int index = 0;
        double dmax = 0;
        int lastIndex = list.size() - 1;

        // Find the point with the maximum distance
        for (int i = 1; i < lastIndex; i++) {
            double d = pointToLineDistance(list.get(0), list.get(lastIndex), list.get(i));
            if (d > dmax) {
                index = i;
                dmax = d;
            }
        }

        // If max distance is greater than epsilon, recursively simplify
        List<LatLong> resultList = new ArrayList<>();
        if (dmax > tolerance) {
            // Recursive call
            List<LatLong> recResults1 = simplify(list.subList(0, index + 1), tolerance);
            List<LatLong> recResults2 = simplify(list.subList(index, lastIndex + 1), tolerance);

            // Build the result list
            recResults1.remove(recResults1.size() - 1);
            resultList.addAll(recResults1);
            resultList.addAll(recResults2);
        } else {
            resultList.add(list.get(0));
            resultList.add(list.get(lastIndex));
        }

        // Return the result
        return resultList;
    }

    /**
     * Provides the distance from a point P to the line segment that passes
     * through A-B. If the point is not on the side of the line, returns the
     * distance to the closest point
     *
     * @param fPoint First point of the line
     * @param sPoint Second point of the line
     * @param point  Point to measure the distance
     */
    public static double pointToLineDistance(LatLong fPoint, LatLong sPoint, LatLong point) {
        double a = point.getLatitude() - fPoint.getLatitude();
        double b = point.getLongitude() - fPoint.getLongitude();
        double c = sPoint.getLatitude() - fPoint.getLatitude();
        double d = sPoint.getLongitude() - fPoint.getLongitude();

        double dot = a * c + b * d;
        double lenSq = c * c + d * d;
        double param = dot / lenSq;

        double xx;
        double yy;

        if (param < 0) { // point behind the segment
            xx = fPoint.getLatitude();
            yy = fPoint.getLongitude();
        } else if (param > 1) { // point after the segment
            xx = sPoint.getLatitude();
            yy = sPoint.getLongitude();
        } else { // point on the side of the segment
            xx = fPoint.getLatitude() + param * c;
            yy = fPoint.getLongitude() + param * d;
        }

        return Math.hypot(xx - point.getLatitude(), yy - point.getLongitude());
    }

    /**
     * Computes the heading between two coordinates
     * (-180 .. 0 .. 180 deg)
     *
     * @return heading in degrees
     */
    public static double getHeadingFromCoordinates180(LatLong fromLoc, LatLong toLoc) {
        double fLat = Math.toRadians(fromLoc.getLatitude());
        double fLng = Math.toRadians(fromLoc.getLongitude());
        double tLat = Math.toRadians(toLoc.getLatitude());
        double tLng = Math.toRadians(toLoc.getLongitude());

        double degree = Math.toDegrees(Math.atan2(
            Math.sin(tLng - fLng) * Math.cos(tLat),
            Math.cos(fLat) * Math.sin(tLat) - Math.sin(fLat) * Math.cos(tLat)
                * Math.cos(tLng - fLng)));

        return degree;
    }

    /**
     * Computes the heading between two coordinates
     * (0 .. 360 deg)
     *
     * @return heading in degrees
     */
    public static double getHeadingFromCoordinates360(LatLong fromLoc, LatLong toLoc) {
        double degree = getHeadingFromCoordinates180(fromLoc, toLoc);
        if (degree >= 0) {
            return degree;
        } else {
            return 360 + degree;
        }
    }

    public static double getPitchFromCoordinates(LatLongAlt from, LatLongAlt to) {
        double dist2d = getDistance2D(from, to);
        double distVert = to.getAltitude() - from.getAltitude();

        return Math.toDegrees(Math.atan2(distVert, dist2d));

    }

    public static Double getSpeedFromVelocity(double velocityX, double velocityY) {
        return Math.sqrt((velocityX * velocityX) + (velocityY * velocityY));
    }

    /**
     * Computes the course based on X,Y velocity
     *
     * @return heading in degrees
     */
    public static Double getCourseFromVelocity(double velocityX, double velocityY) {
        double degree = Math.toDegrees(Math.atan2(velocityY, velocityX));
        if (degree >= 0) {
            return degree;
        } else {
            return 360 + degree;
        }
    }

    /**
     * Extrapolate latitude/longitude given a heading and distance thanks to
     * http://www.movable-type.co.uk/scripts/latlong.html
     *
     * @param origin     Point of origin
     * @param bearingDeg bearing to navigate (Degrees)
     * @param distance   distance to be added
     * @return New point with the added distance
     */
    public static LatLong newCoordFromBearingAndDistance(LatLong origin, double bearingDeg,
                                                         double distance) {

        double lat = origin.getLatitude();
        double lon = origin.getLongitude();
        double lat1 = Math.toRadians(lat);
        double lon1 = Math.toRadians(lon);
        double brng = Math.toRadians(bearingDeg);
        double dr = distance / RADIUS_OF_EARTH;

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(dr) + Math.cos(lat1) * Math.sin(dr)
            * Math.cos(brng));
        double lon2 = lon1
            + Math.atan2(Math.sin(brng) * Math.sin(dr) * Math.cos(lat1),
            Math.cos(dr) - Math.sin(lat1) * Math.sin(lat2));

        return new LatLong(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    public static LatLong newCoordFromOffset(LatLong origin, double northOffset,
                                             double eastOffset) {
        double lat = origin.getLatitude();
        double lon = origin.getLongitude();
        double lat1 = Math.toRadians(lat);
        double lon1 = Math.toRadians(lon);
        double lat2 = lat1 + northOffset / RADIUS_OF_EARTH;
        double lon2 = lon1 + eastOffset / (RADIUS_OF_EARTH * Math.cos(lat1));
        return new LatLong(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    /**
     * Total length of the polyline in meters
     *
     * @param gridPoints
     * @return
     */
    public static double getPolylineLength(List<LatLong> gridPoints) {
        double lenght = 0;
        for (int i = 1; i < gridPoints.size(); i++) {
            final LatLong to = gridPoints.get(i - 1);
            if (to == null) {
                continue;
            }

            lenght += getDistance2D(gridPoints.get(i), to);
        }
        return lenght;
    }

    public static List<LatLong> toClosedPolygon(LatLong center, double radius) {
        return toClosedPolygon(center, radius, 10);
    }

    public static List<LatLong> toClosedPolygon(LatLong center, double radius, int pointsCount) {
        List<LatLong> retVal = new ArrayList<>(pointsCount + 1);
        LatLong top = newCoordFromBearingAndDistance(center, 0, radius);
        retVal.add(new LatLong(top.getLatitude(), top.getLongitude()));
        double stepDeg = 360.0 / pointsCount;
        double yawDeg;
        for (int i = 1; i < pointsCount; i++) {
            yawDeg = stepDeg * i;
            retVal.add(newCoordFromBearingAndDistance(center, yawDeg, radius));
        }
        retVal.add(new LatLong(top.getLatitude(), top.getLongitude()));
        return retVal;
    }

    public static int randPositiveInt() {
        return randInt(1, Integer.MAX_VALUE - 1);
    }

    public static int randInt(int min, int max) {
        return new SecureRandom().nextInt((max - min) + 1) + min;
    }

    public static int randInt() {
        Random rand = new Random();
        return rand.nextInt();
    }

    public static boolean cwOrCcw180(double courseNow, double courseAfter) {
        return cwOrCcw(courseNow < 0 ? 360 + courseNow : courseNow, courseAfter < 0 ?
            360 + courseAfter : courseAfter);
    }

    /**
     *
     */
    public static boolean cwOrCcw(double courseNow, double courseAfter) {
        double d = courseAfter - courseNow;
        if (d < 0)
            d = d + 360;
        if (d < 180)
            return true;
        else
            return false;
    }

    /**
     * Calculates closest (smallest) arc angle to reach yawTo angle from yawFrom
     *
     * @return angle to achieve target yaw. positive value means direction CW, negative CCW.
     */
    public static double findMinimalYawAngle(double yawFrom, double yawTo) {
        yawFrom = to360(yawFrom);
        yawTo = to360(yawTo);
        if (yawTo == yawFrom)
            return 0.0;
        else if (yawTo < yawFrom) {
            double d = yawTo - yawFrom;
            return Math.abs(d) < 360.0 + d ? d : 360.0 + d;
        } else {
            double d = yawFrom - yawTo;
            return Math.abs(d) < 360.0 + d ? -d : -d - 360.0;
        }
    }

    public static double averageOfTowAngle360(double yawOne, double yawTwo) {
        double shortArc = findMinimalYawAngle(yawOne, yawTwo);
        yawOne = to360(yawOne) + shortArc / 2;
        return to360(yawOne);
    }

    /**
     * Calculates closest (smallest) arc angle to reach yawTo angle from yawFrom
     *
     * @return angle to achieve target yaw. positive value means direction CW, negative CCW.
     */
    public static double findMinimalYawAngleRad(double yawFromRad, double yawToRad) {
        yawFromRad = to2Pi(yawFromRad);
        yawToRad = to2Pi(yawToRad);
        double d;
        if (yawToRad == yawFromRad)
            return 0.0;
        else if (yawToRad < yawFromRad) {
            d = yawToRad - yawFromRad;
            return Math.abs(d) < TWO_PI + d ? d : TWO_PI + d;
        } else {
            d = yawFromRad - yawToRad;
            return Math.abs(d) < TWO_PI + d ? -d : -d - TWO_PI;
        }
    }

    public static double addToCourse180(double courseNow, double addDeg) {
        double newCourse = addToCourse360(courseNow, addDeg);
        return newCourse > 180 ? newCourse - 360 : newCourse;
    }

    public static double addToCourse360(double courseNow, double addDeg) {
        if (courseNow < 0)
            return addToCourse360(courseNow + 360, addDeg);

        if (courseNow > 360)
            return addToCourse360(courseNow - 360, addDeg);

        // now we are sure that courseNow is [0..360]

        if (addDeg < -360)
            return addToCourse360(courseNow, addDeg + 360);

        if (addDeg > 360)
            return addToCourse360(courseNow, addDeg - 360);

        double newCourse = courseNow + addDeg;
        if (newCourse < 0)
            return newCourse + 360;

        if (newCourse > 360)
            return newCourse - 360;

        return newCourse;

    }

    public static Double getGuidedMissionProgress(LatLongAlt guidedStartPoint,
                                                  LatLongAlt guidedTargetPoint,
                                                  LatLongAlt position) {
        return 100 - (MathUtils.getDistance3D(position, guidedTargetPoint) * 100
            / MathUtils.getDistance3D(guidedStartPoint, guidedTargetPoint));
    }

    public static int toPlusMinus180(int value) {
        if (value > 180)
            return toPlusMinus180(value - 360);

        if (value < -180) {
            return toPlusMinus180(value + 360);
        }

        return value;
    }

    public static double toPlusMinus180(double value) {
        if (value > 180.0)
            return toPlusMinus180(value - 360.0);

        if (value < -180) {
            return toPlusMinus180(value + 360.0);
        }

        return value;
    }

    public static double to360(double value) {
        if (value >= 360.0)
            return to360(value - 360.0);

        if (value < 0.0) {
            return to360(value + 360.0);
        }

        return value;
    }

    public static int to360(int value) {
        if (value >= 360)
            return to360(value - 360);
        if (value < 0) {
            return to360(value + 360);
        }
        return value;
    }

    public static double to2Pi(double rad) {
        if (rad >= TWO_PI)
            return to2Pi(rad - TWO_PI);
        if (rad < 0.0) {
            return to2Pi(rad + TWO_PI);
        }
        return rad;
    }

    public static double getVector(double param1, double param2) {
        return Math.sqrt((param1 * param1) + (param2 * param2));
    }

    public static Integer minOrNull(Integer v1, Integer v2) {
        if (v1 == null && v2 == null)
            return null;
        if (v1 == null)
            return v2;
        if (v2 == null)
            return v1;
        return v1 > v2 ? v2 : v1;
    }

    public static Float minOrNull(Float v1, Float v2) {
        if (v1 == null && v2 == null)
            return null;
        if (v1 == null)
            return v2;
        if (v2 == null)
            return v1;
        return v1 > v2 ? v2 : v1;
    }

    public static Double minOrNull(Double v1, Double v2) {
        if (v1 == null && v2 == null)
            return null;
        if (v1 == null)
            return v2;
        if (v2 == null)
            return v1;
        return v1 > v2 ? v2 : v1;
    }

    public static Double maxOrNull(Double v1, Double v2) {
        if (v1 == null && v2 == null)
            return null;
        if (v1 == null)
            return v2;
        if (v2 == null)
            return v1;
        return v1 > v2 ? v1 : v2;
    }

    /**
     * This class contains functions used to generate a spline path.
     */
    public static class SplinePath {

        /**
         * Used as tag for logging.
         */
        private static final String TAG = SplinePath.class.getSimpleName();

        private static final int SPLINE_DECIMATION = 20;

        /**
         * Process the given map coordinates, and return a set of coordinates
         * describing the spline path.
         *
         * @param points map coordinates decimation factor
         * @return set of coordinates describing the spline path
         */
        public static List<LatLong> process(List<LatLong> points) {
            final int pointsCount = points.size();
            if (pointsCount < 4) {
                System.err.println("Not enough points!");
                return points;
            }

            final List<LatLong> results = processPath(points);
            results.add(0, points.get(0));
            results.add(points.get(pointsCount - 1));
            return results;
        }

        private static List<LatLong> processPath(List<LatLong> points) {
            final List<LatLong> results = new ArrayList<LatLong>();
            for (int i = 3; i < points.size(); i++) {
                results.addAll(processPathSegment(points.get(i - 3), points.get(i - 2),
                    points.get(i - 1), points.get(i)));
            }
            return results;
        }

        private static List<LatLong> processPathSegment(LatLong l1, LatLong l2, LatLong l3,
                                                        LatLong l4) {
            Spline spline = new Spline(l1, l2, l3, l4);
            return spline.generateCoordinates(SPLINE_DECIMATION);
        }

    }

    public static class Spline {

        private static final float SPLINE_TENSION = 1.6f;

        private LatLong p0;
        private LatLong p0Prime;
        private LatLong a;
        private LatLong b;

        public Spline(LatLong pMinus1, LatLong p0, LatLong p1, LatLong p2) {
            this.p0 = p0;

            // derivative at a point is based on difference of previous and next
            // points
            p0Prime = p1.subtract(pMinus1).dot(1 / SPLINE_TENSION);
            LatLong p1Prime = p2.subtract(this.p0).dot(1 / SPLINE_TENSION);

            // compute a and b coords used in spline formula
            a = LatLong.sum(this.p0.dot(2), p1.dot(-2), p0Prime, p1Prime);
            b = LatLong.sum(this.p0.dot(-3), p1.dot(3), p0Prime.dot(-2), p1Prime.negate());
        }

        public List<LatLong> generateCoordinates(int decimation) {
            ArrayList<LatLong> result = new ArrayList<LatLong>();
            float step = 1f / decimation;
            for (float i = 0; i < 1; i += step) {
                result.add(evaluate(i));
            }

            return result;
        }

        private LatLong evaluate(float t) {
            float tSquared = t * t;
            float tCubed = tSquared * t;

            return LatLong.sum(a.dot(tCubed), b.dot(tSquared), p0Prime.dot(t), p0);
        }

    }

    public static class LinearInterpolator {
        double[][] points;

        public LinearInterpolator(double[][] points) {
            if (points == null || points.length < 2) {
                throw new IllegalArgumentException("Points array for interpolation must be "
                    + "not less than 2");
            }

            this.points = points;
        }

        public double x(double y) {
            return getValue(y, 1);
        }

        public double y(double x) {
            return getValue(x, 0);
        }

        private double getValue(double argument, int argumentIndex) {
            for (int i = 0; i + 1 < points.length; i++) {
                double currentArgument = points[i][argumentIndex];
                double nextArgument = points[i + 1][argumentIndex];
                if (argument >= currentArgument && argument <= nextArgument) {
                    return interpolate(argument, i, argumentIndex);
                }
            }

            if (argument < points[0][argumentIndex]) {
                return interpolate(argument, 0, argumentIndex);
            } else {
                return interpolate(argument, points.length - 2, argumentIndex);
            }
        }

        private double interpolate(double argument, int interpolationIndex, int argumentIndex) {
            int functionIndex = 1 - argumentIndex;

            double currentArgument = points[interpolationIndex][argumentIndex];
            double nextArgument = points[interpolationIndex + 1][argumentIndex];
            double currentFunction = points[interpolationIndex][functionIndex];
            double nextFunction = points[interpolationIndex + 1][functionIndex];

            return currentFunction + (nextFunction - currentFunction)
                / (nextArgument - currentArgument) * (argument - currentArgument);
        }
    }

    public static double calcLongitudeOffset(double latitude) {
        return ONE_METER_OFFSET / cosForDegree(latitude);
    }

    public static double cosForDegree(double degree) {
        return Math.cos(degree * Math.PI / 180.0f);
    }

    /**
     * calculate Quadratic Bézier curves
     * A quadratic Bézier curve is the path traced by the function B(t), given points P0, P1, and P2
     */
    public static List<LatLong> calculateAdaptiveBankTurnSpLine(LatLong p0, LatLong p1,
                                                                LatLong p2, int decimation) {
        List<LatLong> pathPoints = new ArrayList<>();

        pathPoints.add(p0);

        double step = 1d / decimation;
        for (double t = step; t < 1; t += step) {
            double tSquared = t * t;
            double oneMinT = 1 - t;
            LatLong newPoint = LatLong.sum(p0.dot(oneMinT * oneMinT), p1.dot(2 * t * oneMinT),
                p2.dot(tSquared));
            pathPoints.add(newPoint);
        }

        pathPoints.add(p2);

        return pathPoints;
    }

    public static void newPointFromTwoDotsAndDist(double[] xyTwoDots, double[] xyResult,
                                                  double dist) {
        //double //TODO
        double x = xyTwoDots[2] - xyTwoDots[0];
        double y = xyTwoDots[3] - xyTwoDots[1];

        double alphaRad = Math.atan2(y, x);

        x = Math.cos(alphaRad) * dist;
        y = Math.sin(alphaRad) * dist;

        xyResult[0] = x + xyTwoDots[0];
        xyResult[1] = y + xyTwoDots[1];
    }

    public static int prc(int val) {
        return Math.min(100, Math.max(0, val));
    }

    /**
     * The algorithm is named ray casting. The idea of the algorithm is pretty simple:
     * Draw a virtual ray from anywhere outside the polygon to your point and count how often
     * it hits a side of the polygon. If the number of hits is even, it's outside of the polygon,
     * if it's odd, it's inside.
     *
     * @param polygon - polygon in geojson format (maybe with holes)
     * @param point   - considered point
     * @return true if polygon contains point
     */
    public static boolean pointInPolygon(List<List<LatLongAlt>> polygon, LatLongAlt point) {
        int intersections = 0;
        // Check if point matches existing point, return from here to skip intersection computation
        for (List<LatLongAlt> coordinate : polygon) {
            for (int i = 1; i < coordinate.size(); i++) {
                LatLongAlt v1 = coordinate.get(i - 1);
                LatLongAlt v2 = coordinate.get(i);

                if (point.equals(v2)) {
                    return true;
                }

                if (v1.getLatitude() == v2.getLatitude()
                    && v1.getLatitude() == point.getLatitude()
                    && point.getLongitude() > (v1.getLongitude() > v2.getLongitude() ?
                    v2.getLongitude() :
                    v1.getLongitude())
                    && point.getLongitude() < (v1.getLongitude() < v2.getLongitude() ?
                    v2.getLongitude() :
                    v1.getLongitude())) {
                    // Is horizontal polygon boundary
                    return true;
                }

                if (point.getLatitude() > (v1.getLatitude() < v2.getLatitude() ?
                    v1.getLatitude() : v2.getLatitude())
                    && point.getLatitude() <= (v1.getLatitude() < v2.getLatitude() ?
                    v2.getLatitude() :
                    v1.getLatitude())
                    && point.getLongitude() <= (v1.getLongitude() < v2.getLongitude() ?
                    v2.getLongitude() :
                    v1.getLongitude())
                    && v1.getLatitude() != v2.getLatitude()) {
                    double intersection = (
                        (point.getLatitude() - v1.getLatitude()) *
                            (v2.getLongitude() - v1.getLongitude()) /
                            (v2.getLatitude() - v1.getLatitude()) +
                            v1.getLongitude());

                    if (intersection == point.getLongitude()) {
                        // Is other boundary
                        return true;
                    }

                    if (v1.getLongitude() == v2.getLongitude() || point.getLongitude() <= intersection) {
                        intersections++;
                    }
                }
            }
        }

        return intersections % 2 != 0;
    }

    public static LatLong toLatLon(Coordinate c) {
        return new LatLong(c.y, c.x);
    }

    /*public static Double dist(Point p, Geometry geometry) {
        Coordinate[] nearestPts = DistanceOp.nearestPoints(p, geometry);
        double dst = getDistance2D(nearestPts[0].y, nearestPts[0].x, nearestPts[1].y,
        nearestPts[1].x);
        if (geometry.contains(p))
            return -dst;
        else
            return dst;
    }*/

}