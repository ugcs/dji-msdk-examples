package com.example.ugcssample.drone.mission;

import android.content.Context;

import androidx.annotation.NonNull;


import com.example.ugcssample.model.utils.unitsystem.providers.length.LengthUnitProvider;

import java.util.Locale;

import dji.common.error.DJIError;
import dji.common.mission.hotpoint.HotpointMission;

public class DjiMissionSpecificHotPointUtils {

    private DjiMissionSpecificHotPointUtils() {
        // Utility class
    }

    public static String logDjiHotpointMission(@NonNull HotpointMission task, boolean simulatorMode) {
        StringBuilder sb = new StringBuilder();
        sb.append("HotpointMission");
        if (simulatorMode) {
            sb.append(String.format(Locale.US, "%n  SIMULATOR MODE on"));
        }
        sb.append(String.format(Locale.US, "%n  Target alt=%2.2fm lat=%3.7f lon=%3.7f",
            task.getAltitude(), task.getHotpoint().getLatitude(), task.getHotpoint().getLongitude()));
        sb.append(String.format(Locale.US, "%n  radius=%2.2fm cw=%s start=%s heading=%s",
            task.getRadius(), task.isClockwise() + "", task.getStartPoint().toString(),
            task.getHeading().toString()));
        sb.append(String.format(Locale.US, "%n  angularVelocity=%2.2fdeg/s", task.getAngularVelocity()));
        double p = 2 * Math.PI * task.getRadius();
        double spd = p * task.getAngularVelocity() / 360;
        sb.append(String.format(Locale.US, "%n  Calculated: perimeter=%2.2fm speed=%2.2fm/s", p, spd));

        return sb.toString();
    }

    /**
     * from DJI HotpointMission.checkParameters()
     * boolean var1 = true;
     * var1 &= this.hotpoint.isValid();
     * var1 &= this.altitude >= 5.0D && this.altitude <= 500.0D;
     * var1 &= this.radius >= 5.0D && this.radius <= 500.0D;
     * return var1?null:DJIError.COMMON_PARAM_INVALID;
     */
    public static String validateHotpointMission(HotpointMission m, Context context, LengthUnitProvider lup) {
        double d = m.getAltitude();
        if (d < 5.0 || d > 500.0)
            return "context.getString(R.string.hot_point_altitude_error,                lup.getFromMeters(5.0), lup.getDefLetter(),                lup.getFromMeters(500.0), lup.getDefLetter())";

        d = m.getRadius();
        if (d < 5.0 || d > 500.0)
            return" context.getString(R.string.hot_point_radius_error,                lup.getFromMeters(5.0), lup.getDefLetter(),                lup.getFromMeters(500.0), lup.getDefLetter())";

        DJIError e = m.checkParameters();
        if (e != null)
            return e.getDescription();
        return null;
    }

}
